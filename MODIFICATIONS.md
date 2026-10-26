# Community modifications

Upstream: https://github.com/imoonkey/closepaw

Base: `75dae2653f5a6b25d5df51ee7008b0f830de1536`.

This fork modifies the upstream work for ChatGPT/Codex subscription compatibility. The original application and its contributors retain their attribution. This is a community preview, not an upstream or OpenAI release.

Changes prepared on 2026-10-26 and documented for publication on 2026-10-27:

- `auth/OpenAIOAuth.kt`, `auth/AuthStore.kt`: retain OAuth access tokens rather than exchanging them for API keys; refresh-field preservation, account header parsing, callback and logging hardening.
- `llm/CodexRequestBuilder.kt`, `llm/CodexResponseClient.kt`, `llm/CodexSseParser.kt`: preserve tool/image options, correlate tool calls, handle final SSE arguments and terminal errors, improve subscription diagnostics.
- `llm/CodexModels.kt` (new), model catalog and model asset, app/session default configuration: configure current Codex model IDs and migrate old subscription model settings.
- `memory/MemoryStore.kt`: replace existing memory files correctly on Windows with an atomic-move fallback.
- Unit tests: OAuth, model migration, protocol round trips, tool argument handling and platform-independent assertions.
- Community README, installation/validation/distribution documents, video materials and local publication helpers.
- Upstream release workflow restricted to the upstream repository; a manual community verification workflow added so the fork does not invoke upstream signing-secret requirements.

See `doc/dev/chatgpt-plus-compatibility.md` for detailed reasoning and `doc/release/community/VALIDATION.md` for measured results and remaining verification.
