package ai.closepaw.llm

import java.io.BufferedReader
import java.io.InputStream
import org.json.JSONObject

/**
 * Parses Server-Sent Events from a raw byte stream (OkHttp response body)
 * and maps Codex-specific event types to [LLMStreamEvent].
 */
object CodexSseParser {

    data class SseEvent(val type: String, val json: JSONObject)

    /**
     * Parse SSE events from an OkHttp response body stream.
     * Yields parsed JSON events, skipping [DONE] markers.
     */
    fun parse(source: InputStream): Sequence<SseEvent> = sequence {
        val reader = BufferedReader(source.reader(Charsets.UTF_8))
        val dataBuilder = StringBuilder()

        while (true) {
            val line = reader.readLine() ?: break

            when {
                line.startsWith("data:") -> {
                    if (dataBuilder.isNotEmpty()) dataBuilder.append('\n')
                    dataBuilder.append(line.removePrefix("data:").trimStart())
                }
                line.isBlank() -> {
                    val data = dataBuilder.toString().trim()
                    dataBuilder.clear()
                    if (data.isEmpty() || data == "[DONE]") continue
                    try {
                        val json = JSONObject(data)
                        val type = json.optString("type", "")
                        if (type.isNotEmpty()) yield(SseEvent(type, json))
                    } catch (e: Exception) {
                        throw IllegalStateException("Invalid Codex stream event; response was not completed")
                    }
                }
                // Ignore "event:", "id:", "retry:" and other non-data lines
            }
        }

        // Flush remaining data (stream ended without trailing blank line)
        val remaining = dataBuilder.toString().trim()
        if (remaining.isNotEmpty() && remaining != "[DONE]") {
            try {
                val json = JSONObject(remaining)
                val type = json.optString("type", "")
                if (type.isNotEmpty()) yield(SseEvent(type, json))
            } catch (e: Exception) {
                throw IllegalStateException("Invalid final Codex stream event; response was not completed")
            }
        }
    }

    /**
     * Map a Codex SSE event to an [LLMStreamEvent].
     * Returns null for events we don't need to emit.
     */
    fun mapToStreamEvent(
        event: SseEvent,
        accumulator: ToolCallAccumulator
    ): LLMStreamEvent? = when (event.type) {
        "response.created" -> {
            val responseId = event.json.getJSONObject("response").getString("id")
            LLMStreamEvent.Created(responseId)
        }
        "response.output_text.delta" -> {
            LLMStreamEvent.TextDelta(event.json.getString("delta"))
        }
        "response.output_item.added" -> {
            val outputIndex = event.json.optInt("output_index", 0)
            accumulator.onItemAdded(outputIndex, event.json.getJSONObject("item"))
            null
        }
        "response.function_call_arguments.delta" -> {
            val outputIndex = event.json.optInt("output_index", 0)
            accumulator.onArgumentsDelta(outputIndex, event.json.getString("delta"))
            null
        }
        "response.function_call_arguments.done" -> {
            accumulator.onArgumentsDone(
                event.json.optInt("output_index", 0),
                event.json.getString("arguments"),
            )
            null
        }
        "response.output_item.done" -> {
            val outputIndex = event.json.optInt("output_index", 0)
            val item = event.json.getJSONObject("item")
            if (item.optString("type") == "function_call") {
                accumulator.onItemDone(outputIndex, item)?.let { LLMStreamEvent.ToolCallDone(it) }
            } else null
        }
        "response.done", "response.completed" -> {
            val response = event.json.optJSONObject("response")
            when (response?.optString("status")) {
                "failed" -> LLMStreamEvent.Failed(
                    response.optJSONObject("error")?.optString("message") ?: "Codex response failed"
                )
                "incomplete", "cancelled" -> LLMStreamEvent.Failed("Codex response was not completed")
                else -> LLMStreamEvent.Completed
            }
        }
        "response.incomplete" -> {
            val reason = event.json
                .optJSONObject("response")
                ?.let { response ->
                    response.optJSONObject("incomplete_details")?.optString("reason")
                        ?: response.optString("incomplete_reason", "unknown")
                }
                ?: "unknown"
            LLMStreamEvent.Failed("Response incomplete: $reason")
        }
        "response.failed" -> {
            val message = event.json
                .optJSONObject("response")
                ?.optJSONObject("error")
                ?.optString("message", "Unknown error")
                ?: "Unknown error"
            LLMStreamEvent.Failed(message)
        }
        "error" -> {
            val error = event.json.optJSONObject("error") ?: event.json
            val message = error.optString("message", "")
                .ifEmpty { error.optString("code", "Unknown error") }
            LLMStreamEvent.Failed(message)
        }
        else -> null
    }

    /**
     * Accumulates function_call_arguments.delta events into complete tool calls.
     * Supports parallel tool calls by tracking state per output_index.
     */
    class ToolCallAccumulator {
        private data class PendingCall(
            val callId: String,
            val name: String,
            val args: StringBuilder = StringBuilder()
        )

        private val pending = mutableMapOf<Int, PendingCall>()
        private val emittedCallIds = mutableSetOf<String>()

        fun onItemAdded(outputIndex: Int, item: JSONObject) {
            if (item.optString("type") == "function_call") {
                pending[outputIndex] = PendingCall(
                    callId = item.optString("call_id", ""),
                    name = item.optString("name", "")
                )
            }
        }

        fun onArgumentsDelta(outputIndex: Int, delta: String) {
            pending[outputIndex]?.args?.append(delta)
        }

        fun onArgumentsDone(outputIndex: Int, arguments: String) {
            pending[outputIndex]?.args?.apply { clear(); append(arguments) }
        }

        fun onItemDone(outputIndex: Int, item: JSONObject): LLMToolCall? {
            if (item.optString("type") != "function_call") return null
            val call = pending.remove(outputIndex)
            val id = item.optString("call_id").ifBlank { call?.callId.orEmpty() }
            val fn = item.optString("name").ifBlank { call?.name.orEmpty() }
            // The completed item's arguments are authoritative, even if deltas
            // were coalesced or lost. Never execute an incomplete JSON command.
            val args = item.optString("arguments").ifBlank { call?.args?.toString().orEmpty() }
            require(id.isNotBlank() && fn.isNotBlank()) { "Codex tool call is missing its id or name" }
            try { JSONObject(args) } catch (_: Exception) {
                throw IllegalStateException("Codex tool call has incomplete or invalid arguments")
            }
            if (!emittedCallIds.add(id)) return null
            return LLMToolCall(callId = id, name = fn, arguments = args)
        }
    }
}
