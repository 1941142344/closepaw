package ai.closepaw.llm

/** ChatGPT sign-in defaults. Availability is still enforced by the signed-in account. */
object CodexModels {
    const val DEFAULT_MODEL_ID = "gpt-6.1-sol"
    const val DEFAULT_MODEL_KEY = "gpt-6.1-sol-codex"

    // Only migrate retired ChatGPT entries; API and custom providers keep their identity.
    fun migrateSavedModel(name: String): String = when (name) {
        "gpt-5.2-codex", "gpt-5.4-codex", "gpt-5.5-codex" -> DEFAULT_MODEL_KEY
        else -> name
    }
}
