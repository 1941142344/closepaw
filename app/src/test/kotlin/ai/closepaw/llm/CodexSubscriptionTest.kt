package ai.closepaw.llm

import ai.closepaw.auth.CodexHeaders
import com.google.common.truth.Truth.assertThat
import com.openai.models.responses.*
import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Test

/** Offline protocol tests: all credentials and screen pixels here are synthetic. */
class CodexSubscriptionTest {
    private val model = "gpt-6.1-sol"

    @Test
    fun `packaged catalog selects current subscription model and migrates old sessions`() {
        val catalog = ModelCatalog.fromJson(File("src/main/assets/llm_models.json").readText())
        assertThat(catalog.defaultModel(LLMProvider.OPENAI_CODEX)).isEqualTo("gpt-6.1-sol-codex")
        assertThat(catalog.modelsFor(LLMProvider.OPENAI_CODEX).map { it.modelId })
            .containsExactly("gpt-6.1-sol", "gpt-6-sol", "gpt-6-luna").inOrder()
        for (key in listOf("gpt-5.2-codex", "gpt-5.4-codex", "gpt-5.5-codex", "gpt-6.1-sol-codex")) {
            val entry = catalog.resolve(key)
            assertThat(entry.modelId).isEqualTo(model)
            assertThat(entry.provider).isEqualTo(LLMProvider.OPENAI_CODEX)
            assertThat(entry.api).isEqualTo(ApiType.RESPONSE)
            assertThat(entry.supportsVision).isTrue()
        }
        assertThat(catalog.names()).doesNotContain("gpt-5.5-codex")
    }

    @Test
    fun `screen image and explicit non-strict tool schemas survive serialization`() {
        val image = ResponseInputImage.builder().detail(ResponseInputImage.Detail.HIGH)
            .imageUrl("data:image/png;base64,ZmFrZQ==").build()
        val message = ResponseInputItem.ofEasyInputMessage(EasyInputMessage.builder()
            .role(EasyInputMessage.Role.USER)
            .content(EasyInputMessage.Content.ofResponseInputMessageContentList(listOf(ResponseInputContent.ofInputImage(image))))
            .build())
        val obj = JSONObject(CodexRequestBuilder.buildRequestBody("inspect", listOf(message), tools(), model))
        val part = obj.getJSONArray("input").getJSONObject(0).getJSONArray("content").getJSONObject(0)
        assertThat(part.getString("image_url")).isEqualTo("data:image/png;base64,ZmFrZQ==")
        assertThat(part.getString("detail")).isEqualTo("high")
        assertThat(part.getString("type")).isEqualTo("input_image")
        assertThat(obj.getJSONArray("tools").getJSONObject(0).getBoolean("strict")).isFalse()
        val mobile = (0 until obj.getJSONArray("tools").length()).map { obj.getJSONArray("tools").getJSONObject(it) }
            .first { it.getString("name") == "mobile_action" }.getJSONObject("parameters")
        assertThat(mobile.getJSONObject("properties").has("duration_ms")).isTrue()
        assertThat(mobile.getJSONArray("required").toString()).doesNotContain("duration_ms")
        val strict = tools().first().toBuilder().strict(true).build()
        assertThat(CodexRequestBuilder.convertTools(listOf(strict)).getJSONObject(0).getBoolean("strict")).isTrue()
    }

    @Test
    fun `subscription requests round trip tool calls with fresh credentials in both client paths`() = runBlocking {
        val requests = mutableListOf<Request>()
        var authCalls = 0
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests.add(request)
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(toolStream().toResponseBody()).build()
        }.build()
        val client = CodexResponseClient({
            authCalls++
            CodexHeaders("fake-oauth-$authCalls", "fake-account", null)
        }, transport)
        try {
            val result = client.chatWithTools("control phone", emptyList(), tools(), model, 800)
            assertThat(result.responseId).isEqualTo("response-1")
            assertThat(result.toolCalls.map { it.name }).containsExactly("mobile_action", "termux_shell").inOrder()
            assertThat(JSONObject(result.toolCalls.first().arguments).getString("action")).isEqualTo("swipe")
            val history = result.toolCalls.flatMap { call -> listOf(
                ResponseInputItem.ofFunctionCall(ResponseFunctionToolCall.builder().callId(call.callId)
                    .name(call.name).arguments(call.arguments).build()),
                ResponseInputItem.ofFunctionCallOutput(ResponseInputItem.FunctionCallOutput.builder()
                    .callId(call.callId).output("success").build())
            ) }
            val streamed = client.chatWithToolsStreaming("control phone", history, tools(), model).toList()
            assertThat(streamed.filterIsInstance<LLMStreamEvent.ToolCallDone>().map { it.toolCall }).isEqualTo(result.toolCalls)
            assertThat(streamed.last()).isEqualTo(LLMStreamEvent.Completed)
            assertThat(authCalls).isEqualTo(2)
            for ((index, request) in requests.withIndex()) {
                assertThat(request.url.toString()).isEqualTo("https://chatgpt.com/backend-api/codex/responses")
                assertThat(request.header("Authorization")).isEqualTo("Bearer fake-oauth-${index + 1}")
                assertThat(request.header("chatgpt-account-id")).isEqualTo("fake-account")
                val buffer = Buffer()
                request.body!!.writeTo(buffer)
                val json = JSONObject(buffer.readUtf8())
                assertThat(json.getString("model")).isEqualTo(model)
                assertThat(json.getBoolean("store")).isFalse()
                assertThat(json.getBoolean("stream")).isTrue()
                assertThat(json.has("max_output_tokens")).isFalse()
                if (index == 1) {
                    val input = json.getJSONArray("input")
                    assertThat(input.length()).isEqualTo(4)
                    assertThat(input.getJSONObject(1).getString("call_id")).isEqualTo("call-1")
                    assertThat(input.getJSONObject(1).getString("output")).isEqualTo("success")
                }
            }
        } finally { client.cleanup() }
    }

    @Test
    fun `incomplete final response fails in both client paths`() = runBlocking {
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(event("response.completed", JSONObject("""{"response":{"status":"incomplete"}}""")).toResponseBody()).build()
        }.build()
        val client = CodexResponseClient({ CodexHeaders("fake", null, null) }, transport)
        try {
            val error = runCatching { client.chatWithTools("", emptyList(), emptyList(), model, null) }.exceptionOrNull()
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            val streamed = client.chatWithToolsStreaming("", emptyList(), emptyList(), model).toList()
            assertThat(streamed.filterIsInstance<LLMStreamEvent.Failed>()).hasSize(1)
            assertThat(streamed).doesNotContain(LLMStreamEvent.Completed)
        } finally { client.cleanup() }
    }

    private fun tools(): List<FunctionTool> {
        val registry = ai.closepaw.tool.ToolRegistry()
        registry.registerAll(ai.closepaw.tool.impl.MobileActionTool(), ai.closepaw.tool.impl.TermuxShellTool())
        return registry.generateResponsesApiTools()
    }

    private fun toolStream(): String = buildString {
        append(event("response.created", JSONObject("""{"response":{"id":"response-1"}}""")))
        val calls = listOf("mobile_action" to """{"action":"swipe","start":[100,800],"end":[100,200]}""",
            "termux_shell" to """{"command":"pwd"}""")
        calls.forEachIndexed { index, (name, args) ->
            val item = JSONObject().put("type", "function_call").put("name", name).put("call_id", "call-${index + 1}")
            append(event("response.output_item.added", JSONObject().put("output_index", index).put("item", item)))
            append(event("response.function_call_arguments.delta", JSONObject().put("output_index", index).put("delta", "{")))
            append(event("response.function_call_arguments.done", JSONObject().put("output_index", index).put("arguments", args)))
            val done = event("response.output_item.done", JSONObject().put("output_index", index).put("item", item))
            append(done)
            item.put("arguments", args)
            append(event("response.output_item.done", JSONObject().put("output_index", index).put("item", item)))
        }
        append(event("response.completed", JSONObject("""{"response":{"status":"completed"}}""")))
    }

    private fun event(type: String, json: JSONObject) = "data: ${json.put("type", type)}\n\n"
}
