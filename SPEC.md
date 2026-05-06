# SPEC.md — OllamaDrama

_Living specification. Present tense = exists. Future tense = must be built or extended._

---

## 1. Overview

OllamaDrama is a Java library and test harness for orchestrating queries across multiple Large Language Model (LLM) providers — Ollama (local/self-hosted), OpenAI, and Anthropic Claude — with a focus on structured, auditable, confidence-scored responses. It enforces a strict JSON response protocol on every session, runs ensemble voting across model pools to reach consensus answers, integrates external tools via the Model Context Protocol (MCP), and auto-discovers Ollama endpoints across local networks. The project is packaged as a fat JAR and consumed either as a library (via its singleton services and session API) or driven directly through JUnit test classes.

The problem it solves: raw LLM responses are probabilistic, uncalibrated, and provider-specific. OllamaDrama normalises every reply into a `{response, probability, motivation, assumptions_made}` envelope, compares confidence scores against per-model empirical thresholds, and surfaces only high-confidence consensus answers — making LLM output tractable for security research, knowledge extraction, and automated decision workflows.

Primary users are security engineers, LLM researchers, and developers who run the library against local Ollama stacks or cloud API keys, query it from integration tests, or embed it in agent pipelines.

---

## 2. Goals & Non-Goals

### Goals
- Provide a single Java API for Ollama, OpenAI, xAI, and Claude with uniform `SingleStringQuestionResponse` output.
- Enforce a strict JSON response schema (`response` / `probability` / `motivation` / `assumptions_made` / `tool_calls`) on every strict-mode session.
- Run ensemble voting across any configurable set of models; aggregate by uniqueness and confidence threshold.
- Auto-discover Ollama servers on configurable C-class networks; deduplicate by model fingerprint.
- Integrate MCP tools (list, call, preprocess, prompt-injection-guard) with manual or trust-based approval.
- Tune per-model probability thresholds to normalise confidence across heterogeneous models.
- Support scheduled/recurring agent tasks loaded from a filesystem task folder.
- Expose an optional Qwen3 TTS voice output channel.
- Provide model tier metadata (S/M/L/XL/XXL/XXXL) and 240+ n_ctx defaults.
- Build as a self-contained fat JAR with no external Spring context.

### Non-Goals
- Not a REST API server or web application (no HTTP server is exposed by the library itself; `MCPServerForExamples` is a test utility only).
- Not a model fine-tuning or training tool.
- Not a vector database or RAG retrieval engine (RAG prompt templates exist, but retrieval is the caller's responsibility).
- Not a persistent conversation store (chat history lives in-process only).
- Not a multi-tenant SaaS product; no user management, billing, or tenancy isolation.
- Not responsible for provisioning or managing Ollama, OpenAI, or Claude infrastructure.

---

## 3. Technology Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 17 | Core runtime |
| Build | Maven | 3.x | Dependency management, fat JAR assembly |
| Ollama client | ollama4j | 1.1.6 | Ollama HTTP API wrapper, chat, model management |
| Claude client | anthropic-java | 2.17.0 | Anthropic Messages API |
| OpenAI client | openai-java (official `com.openai`) | 4.34.0 | OpenAI chat completions |
| MCP core | mcp | 0.17.2 | Model Context Protocol SDK |
| MCP WebFlux | mcp-spring-webflux | 0.17.2 | Reactive MCP transport |
| MCP WebMVC | mcp-spring-webmvc | 0.17.2 | Servlet MCP transport |
| HTTP | Apache HttpClient | 4.5.14 | Low-level HTTP (MCP endpoint calls, network scan) |
| JSON | Jackson Databind + Core | 2.21.1 | Primary JSON serialisation |
| JSON | FastJSON | 2.0.61 | Opportunistic / lenient JSON parsing |
| JSON | Gson | 2.13.2 | Supplementary serialisation |
| Utilities | Apache Commons Text | 1.15.0 | String manipulation |
| Utilities | Apache Commons IO | 2.21.0 | File operations |
| Utilities | Google Guava | 33.5.0-jre | Collections, hashing |
| Config | JADConfig | 0.13.0 | Environment-variable-driven configuration |
| Logging | SLF4J API | 1.7.32 | Logging facade |
| Logging | Logback Classic | 1.2.13 (test) | Logging implementation (excluded from fat JAR) |
| Testing | JUnit | 4.13.2 | Test framework |
| Packaging | Maven Assembly Plugin | 3.1.1 | jar-with-dependencies fat JAR |

**Recommended upgrades:**
- Logback should be promoted from test-only to a runtime dependency (with `logback.xml` included) for production deployments.

---

## 4. Architecture

OllamaDrama is a **layered, singleton-service library** with no web framework and no dependency injection container. The architecture has four layers:

1. **Configuration layer** — `OllamaDramaSettings` (central config POJO), `Globals` (static constants, model databases, ensemble strings), `AppSettings` (per-invocation overrides), `PreparedQueries` (prompt templates). These are pure data; no I/O.

2. **Service layer** — `OllamaService`, `ClaudeService`, `OpenAIService`, `XaiService` singletons. Each singleton owns the lifecycle of its provider connections: endpoint discovery, connection pooling, session factory methods. `OllamaService` additionally owns MCP endpoint discovery and tool registry. `XaiService` reuses `OpenAISession` against xAI's OpenAI-compatible endpoint (`https://api.x.ai/v1`).

3. **Session layer** — `OllamaSession`, `ClaudeSession`, `OpenAISession`. A session represents one stateful conversation with one model on one endpoint. It holds chat history, enforces the strict JSON protocol, manages context-window trimming, and dispatches MCP tool calls recursively up to a configurable depth. xAI models are served via `OpenAISession` with a different base URL.

4. **Orchestration layer** — `OllamaEnsemble` (parallel multi-session voting), `TaskSchedulerApp` (filesystem-driven scheduled tasks), `OrchestratorStatus`/`Server` (remote orchestrator state model).

Cross-cutting utilities (`MCPUtils`, `JSONUtils`, `OllamaUtils`, `ConfigUtils`, etc.) are static helpers with no state.

**Key architectural constraints that must be preserved:**
- Singletons use double-checked locking; the pattern must not be changed to Spring beans or framework-managed components without a deliberate migration.
- The strict JSON protocol (the four-key envelope) is the contract surface. Changing the keys breaks all callers.
- `TRIM_TRIGGER_RATIO = 0.9` and `TRIM_TARGET_RATIO = 0.75` govern context-window management; altering them changes memory vs. recall trade-offs.
- MCP tool calls must never auto-execute without either `mcp_blind_trust = true` or explicit caller-side approval unless the tool appears in `trusted_mcp_toolnames_csv`.

---

## 5. Module / Package Structure

This is a **single-module Maven project**. The main source tree under `ntt.security.ollamadrama` is:

| Package | Role |
|---|---|
| `config` | `Globals` (static constants, model DB, ensemble definitions), `OllamaDramaSettings` (central config), `PreparedQueries` (prompt templates) |
| `singletons` | `OllamaService`, `ClaudeService`, `OpenAIService`, `XaiService` — provider lifecycle managers (xAI reuses `OpenAISession` against the OpenAI-compatible endpoint at `https://api.x.ai/v1`) |
| `objects` | Core domain POJOs: `OllamaEndpoint`, `MCPEndpoint`, `MCPTool`, `ToolCallRequest`, `OllamaEnsemble`, `OllamaWrappedSession`, `ChatInteraction`, `Server`, `OrchestratorStatus` |
| `objects.sessions` | `OllamaSession`, `ClaudeSession`, `OpenAISession` — stateful conversation managers |
| `objects.response` | `SingleStringQuestionResponse`, `SingleStringEnsembleResponse`, `StatementResponse` — response value objects |
| `enums` | `InteractMethod` (STDIN / FILE), `SessionType` (STRICT / CREATIVE / DEFAULT) |
| `agent` | `Task`, `Scheduler` (enum), `TaskSchedulerApp`, `AppSettings` — filesystem-driven task scheduling |
| `mcp` | `MCPServerForExamples` (test MCP server), `McpResponseTruncator` |
| `orchestrator` | `Server`, `OrchestratorStatus` — remote orchestrator data model |
| `utils` | `OllamaUtils`, `MCPUtils`, `JSONUtils`, `ConfigUtils`, `ClaudeUtils`, `OpenAIUtils`, `XaiUtils`, `SystemUtils`, `DateUtils`, `InteractUtils`, `FilesUtils`, `VoiceUtils`, `NetUtilsLocal`, `HttpRequestUtils`, `StringsUtils`, `NumUtils` |
| `cron` | `RewireOllama` — `java.util.Timer`-based periodic task that calls `OllamaService.wireOllama(false)` on a fixed `checkIntervalInSeconds` after an `initalDelayInSeconds` warm-up. Used by host applications to re-discover Ollama endpoints (network scan + orchestrator pull) without blocking the main loop, e.g. after suspected node churn. Single-class package; no other cron jobs are defined. |

---

## 6. Domain Model

### Core Entities

**`OllamaDramaSettings`** — the root configuration object. Contains all runtime parameters: model list, endpoint coordinates, MCP settings, API keys, thread pool size, voice config. A single instance is owned by `OllamaService`; callers may construct additional instances for custom invocations. Invariant: `ollama_port` must be 1–65535; `threadPoolCount` must be > 0; model name list is deduplicated on `sanityCheck()`.

**`OllamaEndpoint`** — identifies one Ollama server: URL, username, password. May be the local default, a remote satellite, or a discovered network host.

**`MCPEndpoint`** — identifies one MCP server: schema, host, port, SSE path. Default path is `/sse`.

**`MCPTool`** — a tool available from one MCP endpoint: name, raw tool schema string, owning `MCPEndpoint`.

**`ToolCallRequest`** — a parsed tool-call request from an LLM response: tool name, call type (`oneshot` or `continuous`), arguments map, raw request string. Passes `sanitycheck_pass()` iff none of name/calltype/rawrequest are null or blank.

**`OllamaSession`** / **`ClaudeSession`** / **`OpenAISession`** — a live conversation context. Holds model name, endpoint reference, chat history (via `OllamaChatResult` / message list), options, settings, UUID, session type, and token budget tracking. Not serialisable; must not be shared across threads.

**`OllamaEnsemble`** — a named collection of `OllamaWrappedSession` instances. Owns a `ConcurrentHashMap` of sessions. Executes questions in parallel and aggregates into `SingleStringEnsembleResponse`.

**`OllamaWrappedSession`** — pairs an `OllamaSession` with a `probability_threshold` (default 70). The threshold gates whether a session's answer is counted as "confident."

**`SingleStringQuestionResponse`** — the universal response value object. Fields: `response` (string), `probability` (0–100 integer), `empty` (boolean), `motivation`, `assumptions_made`, `tool_calls`, `exec_time` (seconds), `promptinject` (boolean).

**`SingleStringEnsembleResponse`** — aggregate of per-model `SingleStringQuestionResponse`s. Tracks unique replies and unique confident replies. `getBestResponse()` returns the most common response. `getEnsemble()` returns a formatted summary.

**`Task`** — a scheduled prompt: id, prompt text, `Scheduler` enum value, last-executed timestamp. `isEligibleToRun()` compares elapsed minutes against the scheduler's interval.

### Enums

| Enum | Values | Significance |
|---|---|---|
| `SessionType` | STRICT, CREATIVE, DEFAULT | Controls inference options (temp, topK, topP) and response-format enforcement |
| `InteractMethod` | STDIN, FILE | Input channel for interactive/agent sessions |
| `Scheduler` | ALWAYS(0), EVERY_HOUR(60), EVERY_4HOURS(240), DAILY(1440), WEEKLY(10080), ONCE(MAX_VALUE) | Task recurrence interval in minutes |

### Business Rules
- A model's response is only counted as confident if its reported `probability` meets or exceeds the model's entry in `Globals.MODEL_PROBABILITY_THRESHOLDS`. If the model has no entry, the session-level `probability_threshold` (default 70) applies.
- `FAILTOUNDERSTAND` is a reserved sentinel: if a model returns it with probability 0, the response is suppressed.
- Context-window trimming fires when estimated token count reaches 90% of `session_tokens_maxlen`; history is trimmed to 75% of the limit, preserving the system prompt and most-recent turns.
- MCP tool calls are recursive up to `DEFAULT_MAX_RECURSIVE_TOOLCALL_DEPTH = 5` by default (overridable per `AppSettings`).
- Ensemble deduplication hashes model sets to prevent two endpoints hosting the same models from both contributing to a vote.
- **"Paris" validation** (`ollama_skip_paris_validation = false`, default) — at endpoint registration, after fingerprinting, every configured model on the endpoint runs a single strict-mode probe asking *"Is the capital city of France named Paris? Reply with only Yes or No."* (see `OllamaService.validate_model`, line 570). The model passes only if it returns `Yes`/`YES` parseable through the strict JSON envelope. The probe simultaneously verifies (a) the endpoint can serve the model, (b) the model produces strict-JSON output under the configured `Options`, and (c) the model is not catastrophically uncensored or broken. Guard models (`gpt-oss-safeguard`, `shieldgemma`, `llama-guard3`) are exempted from the Paris probe and instead validated via a model-specific creative-mode probe (`Globals.guard_model_benign_response`). The name "Paris" is just the test prompt's content — it is not a pairwise check. Setting `ollama_skip_paris_validation = true` skips the probe entirely (quick-boot mode); failed endpoints are placed in `abandoned_ollamas` and not retried in the same boot cycle.

---

## 7. Inbound Interfaces

OllamaDrama exposes no HTTP server in normal operation. All inbound interfaces are in-process Java API calls or filesystem triggers.

### 7.1 Strict Question — `OllamaSession.askStrictChatQuestion`

**Trigger:** Direct Java call.  
**Input:** `String question`, `Boolean make_tools_available`.  
**Behaviour:**
1. Appends `ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS` schema to the question.
2. Submits to `OllamaAPI.chat()` with the configured `Options`.
3. Parses the JSON reply into `SingleStringQuestionResponse` via `JSONUtils.createPOJOFromJSONOpportunistic`.
4. If `promptinject` flag set and `mcp_enable_promptinject_protection` is true, re-evaluates the response through a guard model.
5. If tool calls are present and `make_tools_available` is true, executes tool call recursively.
6. If token budget nears limit, calls `trimChatHistoryIfNeeded()` before the next turn.  
**Output:** `SingleStringQuestionResponse` with populated `response`, `probability`, `motivation`, `assumptions_made`, `tool_calls`, `exec_time`.  
**Error cases:** Parse failure → returns response with `empty = true`. Ollama connectivity failure → retried up to `MAX_RETRY_ATTEMPTS = 10` with `RETRY_DELAY = 5s` and `LONG_RETRY_DELAY = 30s`.  
**Security:** Caller-controlled; no authentication on the Java API itself.

### 7.2 Statement Provision — `OllamaSession.provideChatStatement`

**Trigger:** Direct Java call.  
**Input:** `String statement`, `Boolean make_tools_available`.  
**Behaviour:** Same pipeline as 7.1 but uses `ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_STATEMENTS` schema (acknowledgement protocol, not a question).  
**Output:** `StatementResponse`.

### 7.3 Ensemble Question — `OllamaEnsemble.ask_chat_question`

**Trigger:** Direct Java call.  
**Input:** `String question`, `boolean hide_if_uncertain`, `long timeout_ms`.  
**Behaviour:**
1. Submits question to all sessions in `sessions` map concurrently via thread pool.
2. Collects `SingleStringQuestionResponse` per session.
3. Aggregates into `SingleStringEnsembleResponse`.
4. Applies per-model probability thresholds to populate confident-reply buckets.  
**Output:** `SingleStringEnsembleResponse`.  
**Error cases:** Per-session timeouts are absorbed; sessions that time out contribute no response.

### 7.4 Claude Question — `ClaudeSession.askChatQuestion`

**Trigger:** Direct Java call.  
**Input:** `String question`, `boolean hide_if_uncertain`.  
**Behaviour:**
1. Prepends `ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS` to the question.
2. Calls Anthropic Messages API with model, max_tokens=4096, temperature=0.0.
3. Retries up to 3 times on parse failure (1s delay); throttles 10s on exception.  
**Output:** `SingleStringQuestionResponse`.  
**Error cases:** Parse failure after 3 retries → empty response. API exception → empty response after 10s throttle.

### 7.5 OpenAI Question — `OpenAISession.askChatQuestion`

**Trigger:** Direct Java call.  
**Input / Behaviour / Output:** Equivalent to 7.4 but via OpenAI chat completions API. Temperature=0.0, TopP=0.0.

### 7.6 Scheduled Task Execution — `TaskSchedulerApp`

**Trigger:** `runAllTasksInParallel()` called by host application (e.g., from a cron-driven main loop).  
**Input:** Task files loaded from a folder. File extension determines schedule (`.always`, `.every_hour`, `.every_4hours`, `.daily`, `.weekly`, `.once`).  
**Behaviour:**
1. `loadTasksFromFolder(foldername)` reads all task files; file content becomes the prompt.
2. `isEligibleToRun()` checks each task against `lastExecuted` and schedule interval.
3. Eligible tasks are executed in parallel via `OllamaUtils` or similar.
4. State saved to `taskstatefile` JSON after each pass.  
**Output:** Task results logged; state file updated.  
**Error cases:** Filesystem errors on load → task skipped. Execution errors → logged, task marked executed to avoid retry storm.

### 7.7 Interactive Session — `InteractUtils` (STDIN / FILE mode)

**Trigger:** `interact_method = STDIN` reads from standard input; `FILE` polls `interact_filepath`.  
**Input:** Free-form text prompts.  
**Behaviour:** Feeds prompts into the configured session; prints responses.  
**Output:** Console or file output.

### 7.8 MCP Example Server — `MCPServerForExamples.startServer(int port)`

**Trigger:** Explicit `startServer(port)` call (used in tests only).  
**Exposes tools:**
- `get_the_meaning_of_life` → `"42"`
- `get_current_time_in_UTC` → UTC timestamp string
- `get_pi` → prompt injection payload (intentional — for injection detection tests)
- `use_hidden_algorithm_with_two_numbers(num1, num2)` → `num1 + num2 + 1`

---

## 8. Outbound Interfaces

### 8.1 Ollama Chat API

**Target:** Ollama HTTP server (default `http://localhost:11434`).  
**Sent:** Chat message list + model name + `Options` (temperature, topK, topP, minP, n_ctx, seed, num_predict).  
**Expected response:** `OllamaChatResult` with generated text.  
**Failure handling:** `createConnection` retries indefinitely with exponential delays if the server is unreachable at startup. Per-request failures throw; callers retry up to `MAX_RETRY_ATTEMPTS`.

### 8.2 Anthropic Messages API

**Target:** `https://api.anthropic.com` via `anthropic-java` SDK.  
**Sent:** Model name, system prompt, user messages, max_tokens=4096.  
**Expected response:** Text content block parsed as JSON.  
**Failure handling:** 3 retries on parse failure; 10s throttle on SDK exception.

### 8.3 OpenAI Chat Completions API

**Target:** `https://api.openai.com` via the official `com.openai:openai-java` SDK (`OpenAIClient` built through `OpenAIOkHttpClient.builder().apiKey(...).build()`).  
**Sent:** Model name, single user message constructed via `ChatCompletionCreateParams.builder().addUserMessage(content).model(modelName)…build()`. For standard chat models (`gpt-3.5*`, `gpt-4*`, `gpt-5*`) `temperature(0.0)` and `topP(0.0)` are also set for determinism. For reasoning models (`o1*`, `o3*`, `o4*`) those two builder calls are skipped — the OpenAI API rejects non-default values and would return HTTP 400. Detection is by name prefix in `OpenAISession.isReasoningModel(String)`.  
**Expected response:** `ChatCompletion` — first `choices().get(0).message().content()` (Optional, `.orElse("")`) is parsed as JSON.  
**Failure handling:** Same as 8.2 — 3 retries on parse failure with 1s delay; 10s throttle on SDK exception. The new SDK throws `com.openai.errors.OpenAIException` subclasses; these are caught generically.

### 8.4 xAI Chat Completions API

**Target:** `https://api.x.ai/v1` via `XaiService`, using `OpenAISession` with a custom base URL.  
**Sent:** Model name, single user message via the same `ChatCompletionCreateParams` builder as 8.3. Temperature=0.0 and TopP=0.0 are set for standard xAI models; reasoning models follow the same `isReasoningModel()` detection to skip those parameters.  
**Expected response:** `ChatCompletion` — parsed identically to 8.3.  
**Failure handling:** Same as 8.3 — 3 retries on parse failure with 1s delay; 10s throttle on SDK exception.

### 8.5 MCP Tool List (outbound)

**Target:** MCP endpoint at `http://{host}:{port}{path}` (SSE).  
**Sent:** `ListTools` RPC via `MCPUtils.listToolFromMCPEndpoint`.  
**Expected response:** `ListToolsResult` with tool schemas.  
**Failure handling:** Timeout controlled by `MCP_LIST_TOOLS_TIMEOUT = 5s`; failure logged, endpoint skipped.

### 8.6 MCP Tool Call (outbound)

**Target:** Same MCP endpoint.  
**Sent:** `CallTool` RPC with tool name and argument map.  
**Expected response:** `CallToolResult`.  
**Failure handling:** `halt_on_tool_error` flag controls whether a tool failure aborts the session. Response truncated by `McpResponseTruncator` to prevent context overflow.

### 8.7 Network Scan (Ollama Discovery)

**Target:** All hosts on configured C-class networks, port 11434.  
**Sent:** HTTP probe.  
**Expected response:** Ollama version/model listing.  
**Failure handling:** Non-responsive hosts silently skipped; duplicates filtered by model-set fingerprint SHA-256.

### 8.8 Qwen3 TTS

**Target:** `qwen3tts_url` (configurable HTTP endpoint).  
**Sent:** Text string + voice identifier.  
**Expected response:** Audio data or playback trigger.  
**Failure handling:** Errors logged; TTS is non-critical.

### 8.9 Task State File

**Target:** Local filesystem at `taskstatefile` path (default `task_state.json`).  
**Sent:** JSON serialisation of task last-executed timestamps.  
**Failure handling:** IO errors on write should be logged and tolerated (non-fatal).

### 8.9 Orchestrator Status Fetch

**Target:** Remote orchestrator HTTP endpoint configured via `orchestrator_url` (e.g. `http://127.0.0.1:1111/api/status`).  
**Sent:** HTTP GET (no body, no auth) via `HttpRequestUtils.getBodyUsingGETUrlRequestOpportunistic`.  
**Expected response:** JSON serialisation of `OrchestratorStatus` — `{ status, total_servers, healthy_servers, servers: [{ name, url, healthy, models, capacity, priority, active_requests, last_check }] }`. Parsed via `JSONUtils.createPOJOFromJSONOpportunistic` into `OrchestratorStatus`.  
**Behaviour:** `OllamaService.add_orchestrator_ollamas` runs during `wire_ollama()` whenever `orchestrator_url` is non-null. Each `Server` URL returned by the orchestrator is added to the candidate `OllamaEndpoint` map (skipping any URL already in `abandoned_ollamas` or already-discovered candidates). Each candidate is then validated through the standard fingerprint + PARIS sanity check pipeline like any other endpoint.  
**Failure handling:** Body fetch and JSON parse are opportunistic — a null body, malformed JSON, or null `OrchestratorStatus` silently yields no additional endpoints. The orchestrator is treated as a hint source, not a required input.  
**Authentication:** None currently. The `Server.priority`, `Server.capacity`, and `Server.active_requests` fields are accepted but not yet used for routing; they are reserved for future load-balanced dispatch.

### 8.10 xAI (Grok) Chat Completions API

**Target:** `https://api.x.ai/v1` via the same `com.openai:openai-java` SDK. The xAI Chat Completions endpoint is OpenAI-compatible at the wire-protocol level, so the only differences from §8.3 are the base URL and the API key.  
**Client construction:** `OpenAIOkHttpClient.builder().apiKey(settings.getXaikey()).baseUrl(XaiService.XAI_BASE_URL).build()` — performed in `XaiService.get_strict_session(...)`. Sessions returned are plain `OpenAISession` instances; the chat-completion call path is identical to OpenAI.  
**Sent / Expected response / Failure handling:** Same as §8.3. Grok models accept `temperature` and `top_p`, so `OpenAISession.isReasoningModel(...)` correctly returns `false` for `grok-*` and the deterministic `0.0` settings are kept.  
**Authentication:** Bearer token (xAI API key). No additional headers required.

---

## 9. Configuration Reference

All configuration is loaded from the `OLLAMADRAMACONFIG` environment variable as a JSON blob parsed by `ConfigUtils.parseConfigENV()` into `OllamaDramaSettings`.

| Key | Default | Description | Required |
|---|---|---|---|
| `ollama_models` | `Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_MAXCONTEXT_L` | Comma-separated model names to use | No |
| `ollama_port` | `11434` | Ollama server port | No |
| `ollama_timeout` | `240` | Per-request timeout in seconds | No |
| `ollama_username` | `""` | Basic auth username for Ollama proxy | No |
| `ollama_password` | `""` | Basic auth password for Ollama proxy | **Secret** |
| `ollama_scan` | `true` | Auto-scan network for Ollama endpoints | No |
| `ollama_skip_paris_validation` | `false` | Skip the per-model "Paris" sanity check at endpoint registration (see §6 Business Rules) — quick-boot mode | No |
| `n_ctx_override` | `-1` | Override n_ctx for all models (-1 = use model defaults) | No |
| `temperature_override` | `-1.0` | Override temperature (-1 = use session defaults) | No |
| `orchestrator_url` | `null` | URL of remote orchestrator | No |
| `satellites` | `[]` | List of additional `OllamaEndpoint` objects | No |
| `threadPoolCount` | `20` | Size of executor thread pool | No |
| `openaikey` | `""` | OpenAI API key | **Secret** |
| `use_openai` | `false` | Enable OpenAI provider | No |
| `claudekey` | `""` | Anthropic API key | **Secret** |
| `use_claude` | `false` | Enable Claude provider | No |
| `xaikey` | `""` | xAI (Grok) API key — used by `XaiService` to authenticate against `https://api.x.ai/v1` | **Secret** |
| `use_xai` | `false` | Enable xAI provider | No |
| `mcp_ports` | `"8000,8080,9000"` | CSV of ports to scan for MCP endpoints | No |
| `mcp_sse_paths` | `["/sse"]` | SSE paths to probe on MCP hosts | No |
| `mcp_scan` | `false` | Auto-scan for MCP endpoints | No |
| `mcp_blind_trust` | `false` | Auto-approve all MCP tool calls | No |
| `mcp_enable_promptinject_protection` | `true` | Run prompt injection detection on tool output | No |
| `trusted_mcp_toolnames_csv` | `""` | Whitelist of tool names that auto-approve | No |
| `filtered_mcp_toolnames_csv` | `""` | Blacklist of tool names to block | No |
| `mcp_satellites` | `[]` | List of additional `MCPEndpoint` objects | No |
| `autopull_max_llm_size` | `"XL"` | Maximum model tier to auto-pull (S/M/L/XL/XXL/XXXL) | No |
| `interact_method` | `"STDIN"` | Interaction channel (STDIN or FILE) | No |
| `interact_filepath` | `"/interact"` | File path for FILE interaction mode | No |
| `qwen3tts_enable` | `false` | Enable Qwen3 TTS output | No |
| `qwen3tts_url` | `""` | TTS endpoint URL | No |
| `qwen3tts_voice` | `""` | TTS voice identifier | No |
| `rounds_per_pass` | `2` | Number of ensemble rounds per question | No |

---

## 10. Data Model

OllamaDrama has **no persistent database**. All state is in-memory or in flat files.

### In-memory state

| Store | Owner | Type | Contents |
|---|---|---|---|
| Ollama endpoints | `OllamaService` | `Map<String, OllamaEndpoint>` | Discovered/configured Ollama servers |
| MCP tools | `OllamaService` | `Map<String, MCPTool>` | All tools discovered from all MCP endpoints |
| Registered fingerprints | `OllamaService` | `Set<String>` | SHA-256 hashes of model sets (dedup) |
| Chat history | `OllamaSession` | `OllamaChatResult` | Ordered message list for one conversation |
| Ensemble sessions | `OllamaEnsemble` | `ConcurrentHashMap<String, OllamaWrappedSession>` | Active sessions keyed by model name |
| Task state | `TaskSchedulerApp` | `List<Task>` | Scheduled tasks with last-execution timestamps |

### Flat-file state

| File | Format | Contents |
|---|---|---|
| `taskstatefile` (default: `task_state.json`) | JSON | Task IDs mapped to last-executed ISO timestamps |
| Task folder files | Plain text | Prompt text; filename suffix encodes schedule |

### n_ctx defaults

`Globals` contains a `HashMap<String, Integer>` (`MODEL_N_CTX_DEFAULTS`) with 240+ entries mapping model name strings to context window sizes (e.g., `llama3.1:70b` → 32768, `qwen3:32b` → 32768, `mistral:7b` → 32768). These are used when `n_ctx_override` is -1.

### Model probability thresholds

`Globals.MODEL_PROBABILITY_THRESHOLDS` maps model name strings to integer threshold values calibrated empirically (e.g., `qwen2.5:7b` → 14, `claude-opus-4-6` → 44, `gpt-4` → 65). A model's answer is "confident" only if its self-reported probability meets or exceeds this threshold.

**Newer-generation OpenAI models** (`o1`, `o3`, `o3-mini`, `o4-mini`, `gpt-4.1*`, `gpt-5*`) have starter thresholds inherited from the closest legacy peer — reasoning models default to 55 (matching `o1-mini`), flagship chat to 65 (matching `gpt-4`), and lighter "mini"/"nano" tiers to 55/44 respectively. These are explicitly *not* empirically calibrated; re-tune via `OllamaConfidenceThresholdTuning` against your own evaluation prompt set before relying on the confident-vote bucket in production.

**xAI / Grok models** (`grok-4`, `grok-3`, `grok-3-fast`, `grok-3-mini`, `grok-2-latest`, `grok-2-1212`) carry the same starter-threshold caveat — flagship at 65, mini/legacy variants at 55. Calibrate before relying on the confident-vote bucket.

---

## 11. Security Model

### Authentication

- **Ollama:** Optional HTTP Basic Auth via `ollama_username` / `ollama_password`. Applied per `OllamaEndpoint`; supports per-satellite credentials.
- **OpenAI:** API key via `openaikey` config value. Passed as Bearer token by the SDK.
- **Claude:** API key via `claudekey` config value. Passed by the Anthropic SDK.
- **MCP endpoints:** No authentication mechanism currently defined. Future work: add per-endpoint auth headers.
- **Library API itself:** No authentication. The caller owns the JVM process and is trusted.

### Prompt Injection Protection

When `mcp_enable_promptinject_protection = true` (default), MCP tool output is passed through a guard-model classification step before being fed back to the session. Guard models: `gpt-oss-safeguard:20b`, `shieldgemma`, `llama-guard3`. The prompt injection detection prompt is defined in `Globals.PROMPT_INJECTION_DETECTION_PROMPT`. If injection is detected, `SingleStringQuestionResponse.promptinject` is set to `true`.

### MCP Tool Call Safety

- `mcp_blind_trust = false` (default): tool calls require explicit caller-side approval before execution. The tool name and arguments must be reviewed.
- `trusted_mcp_toolnames_csv`: tools on this whitelist auto-approve without caller review.
- `filtered_mcp_toolnames_csv`: tools on this blacklist are blocked outright.
- `toolcall_history` is maintained per session to enable audit of all tool calls made in a conversation.

### MCP Tool Output Pre-processing — `mcp_preprocess`

`OllamaDramaSettings.mcp_preprocess` is a `Map<String, Function<String, String>>` keyed by MCP tool name. After a tool call succeeds and the raw text is extracted via `MCPUtils.getRawText`, `OllamaSession.askStrictChatQuestion` checks the map: if a `Function` is registered for the tool name, that function is applied to the raw output and the **return value replaces the entire content fed back to the model** for the next turn. If no entry exists, the default wrapper (`"\nResponse from running tool_call <raw>:\n\n<raw>"`) is used instead.

**Use cases:**
- Strip noise (HTML boilerplate, headers, ANSI codes) from a verbose tool response before it consumes context window.
- Project a structured tool result down to the few fields the model actually needs.
- Redact secrets or PII from tool output before it enters the chat history.

**Constraints and risks:**
- The map is set in Java code on `OllamaDramaSettings` / `AppSettings`; it is not loaded from the `OLLAMADRAMACONFIG` JSON blob, since `Function` is not JSON-serialisable.
- The function runs in the calling thread inside the session's tool-call recursion. It must be fast and side-effect-free; throwing from the function will propagate and kill the in-flight question.
- Pre-processing happens **before** the prompt-injection guard sees the text. If the function rewrites injection markers, it can mask injection attempts. Treat the function as part of the trust boundary alongside `trusted_mcp_toolnames_csv`.
- There is no automatic chaining or composition; only one function per tool name.

**Worked example:**

```java
OllamaDramaSettings settings = ConfigUtils.parseConfigENV();
settings.getMcp_preprocess().put("fetch", raw -> {
    // keep only the first 2 KB of fetched HTML so wide pages don't blow the context
    return raw.length() > 2048 ? raw.substring(0, 2048) + "\n…[truncated]…" : raw;
});
```

### Sensitive Configuration

API keys (`openaikey`, `claudekey`, `xaikey`, `ollama_password`) must be supplied as environment variable content (`OLLAMADRAMACONFIG` JSON blob) and must not be committed to version control. No encryption at rest is provided; the caller is responsible for securing the environment.

### Public Operations

All Java API methods are public by design (library). Network-level exposure exists only when `MCPServerForExamples.startServer()` is called (test/dev use only) — that HTTP server is unauthenticated and must not be started in production.

---

## 12. Error Handling Strategy

### Classification

| Class | Examples | Handling |
|---|---|---|
| Connectivity | Ollama unreachable, API timeout | Retry with `RETRY_DELAY` (5s) up to `MAX_RETRY_ATTEMPTS` (10); then `LONG_RETRY_DELAY` (30s) |
| Parse failure | LLM returned malformed JSON | Up to 3 retries (Claude/OpenAI); return `empty=true` response after exhaustion |
| Tool error | MCP tool call returned error | If `halt_on_tool_error=true` → abort session; else log and continue |
| Validation | Invalid port, empty model name | `sanityCheck()` throws `IllegalArgumentException` at config load time |
| Prompt injection | Guard model classifies tool output as injection | `promptinject=true` flag set; caller decides whether to surface the response |
| Throttling | OpenAI/Claude rate limit exception | 10-second sleep then retry |

### Error Response Shape

All session errors produce a `SingleStringQuestionResponse` with `empty = true`, `probability = 0`, and `response = ""`. No error codes or HTTP status codes apply (this is a library, not a web service).

### Retry and Fallback

- Ollama endpoint failure during ensemble: the session contributes no vote; the ensemble proceeds with remaining sessions.
- All retry logic is blocking (sleeps the calling thread). Callers using ensemble mode should use the thread pool to avoid head-of-line blocking.

---

## 13. Testing Strategy

### What exists

| Test Class | Level | Coverage |
|---|---|---|
| `ConfigTest.flushOllamaDramaConfig` | Unit | JSON round-trip of `OllamaDramaSettings` |
| `BuildDateTest` | Unit | Build timestamp assertion |
| `MCPTest.simple_direct_HTTP_MCP_ListTool_Test` | Integration | Lists tools from a live MCP endpoint |
| `MCPTest.simple_direct_HTTP_MCP_CallTool_Test` | Integration | Calls `fetch` tool on `ntt.com` |
| `MCPTest.simple_HTTP_MCP_Tool_Test_fetch` | Integration | LLM-driven tool discovery and call |
| `MCPTest.simple_HTTP_MCP_Temperature_LLMToolCallOutput_Test` | Integration | Tool discovery, call, and output in a session |
| `OllamaStandardEnsemble` | Integration | Ensemble voting across Ollama models |
| `OpenAIStandardEnsemble` | Integration | Ensemble voting via OpenAI |
| `ClaudeStandardEnsemble` | Integration | Ensemble voting via Claude |
| `DebateTest` | Integration | Multi-LLM debate scenario |
| `MemoryLossTest` | Integration | Context window stress under long prompts |
| `OllamaConfidenceThresholdTuning` | Integration | Calibrates per-model probability thresholds |
| `GeneralKnowledgeTest` | Integration | Factual Q&A accuracy |
| `MaliciousTest` | Integration | Jailbreak and adversarial prompt resistance |
| `UncensoredTest` | Integration | Uncensored model validation |

All integration tests require a live Ollama instance (and optionally OpenAI/Claude API keys). There is no test container or mock for Ollama.

### What must be unit tested
- `OllamaDramaSettings.sanityCheck()` — deduplication and validation logic.
- `SingleStringQuestionResponse` JSON parsing via `JSONUtils.createPOJOFromJSONOpportunistic`.
- `Task.isEligibleToRun()` — scheduling logic across all `Scheduler` values.
- `ToolCallRequest.sanitycheck_pass()` — all null/empty combinations.
- `McpResponseTruncator.truncate()` — boundary conditions (empty string, exact max, over max).
- Token estimation in `OllamaSession.estimateTokenCount()`.
- Context-window trim logic in `OllamaSession.trimChatHistoryIfNeeded()`.

### Coverage gaps that should be addressed
- No unit tests for `MCPUtils.parseTool()` or `MCPUtils.parseArguments()` (JSON extraction from LLM tool-call strings).
- No unit tests for `NetUtilsLocal` network scanning logic.
- No tests for `OllamaDramaSettings.updateWithAppSettings()` merge logic.
- No tests for `TaskSchedulerApp` state persistence (`saveState` / `loadTasksFromFolder`).
- Integration tests rely on external services; CI will fail without a local Ollama instance. A mock Ollama server or WireMock stub should be introduced for CI.

---

## 14. Build & Run

### Build

```bash
mvn clean package -DskipTests
```

Produces two JARs in `target/`:
- `ollamadrama-{version}.jar` — library JAR without dependencies.
- `ollamadrama-{version}-jar-with-dependencies.jar` — fat JAR for standalone execution.

To build and run tests (requires live Ollama on localhost:11434):

```bash
mvn clean verify
```

### Run locally

The library has no `main` class. It is used by embedding in a host application or by running JUnit test classes directly from an IDE.

Minimum environment for Ollama-backed usage:

```bash
export OLLAMADRAMACONFIG='{"ollama_models":"qwen3:4b","ollama_port":11434,"ollama_timeout":60,"ollama_scan":false}'
```

For OpenAI:

```bash
export OLLAMADRAMACONFIG='{"use_openai":true,"openaikey":"sk-...","ollama_scan":false}'
```

For Claude:

```bash
export OLLAMADRAMACONFIG='{"use_claude":true,"claudekey":"sk-ant-...","ollama_scan":false}'
```

For xAI (Grok):

```bash
export OLLAMADRAMACONFIG='{"use_xai":true,"xaikey":"xai-...","ollama_scan":false}'
```

### Using xAI (Grok) via the OpenAI-compatible client

xAI exposes a Chat Completions API at `https://api.x.ai/v1` that is binary-compatible with OpenAI's. OllamaDrama wraps that compatibility behind a thin singleton, `XaiService`, so callers can pick xAI exactly the same way they pick OpenAI — only the service name and the model identifier change. The session class (`OpenAISession`) is shared verbatim between both providers; nothing else needs to change.

**1. Set the API key.** Either supply it through the standard `OLLAMADRAMACONFIG` JSON blob (`xaikey`) or set it on a `OllamaDramaSettings` / `AppSettings` instance directly. Get a key from <https://console.x.ai>.

**2. Pick a Grok model.** Use a model identifier from `Globals.MODEL_NAMES_XAI_*` (e.g. `grok-4`, `grok-3`, `grok-3-mini`, `grok-2-latest`). All Grok models accept `temperature`/`top_p`, so `OpenAISession` keeps the deterministic `0.0` defaults — no special handling needed.

**3. Single-session usage.** Call `XaiService.getStrictSession(modelName)` and use the returned `OpenAISession` like any other:

```java
OllamaDramaSettings settings = ConfigUtils.parseConfigENV();   // reads xaikey from OLLAMADRAMACONFIG
OpenAISession grok = XaiService.getStrictSession("grok-4", settings);
SingleStringQuestionResponse r = grok.askChatQuestion("Who founded NTT?", false);
System.out.println(r.getResponse() + " (probability " + r.getProbability() + ")");
```

**4. Ensemble usage.** Call `XaiUtils.strictEnsembleRun` exactly like `OpenAIUtils.strictEnsembleRun`:

```java
OllamaDramaSettings settings = ConfigUtils.parseConfigENV();
SingleStringEnsembleResponse out = XaiUtils.strictEnsembleRun(
        "What is the capital of Sweden?",
        Globals.MODEL_NAMES_XAI_TIER1,         // "grok-4,grok-3,"
        settings,
        false);
out.printEnsemble();
```

**5. Mixing providers.** Each provider has its own service, so a multi-provider ensemble simply runs each in turn (or in parallel via the thread pool):

```java
SingleStringEnsembleResponse openai_out = OpenAIUtils.strictEnsembleRun(query, Globals.MODEL_NAMES_OPENAI_TIER1, settings, false);
SingleStringEnsembleResponse xai_out    = XaiUtils.strictEnsembleRun(query, Globals.MODEL_NAMES_XAI_TIER1, settings, false);
SingleStringEnsembleResponse claude_out = ClaudeUtils.strictEnsembleRun(query, Globals.MODEL_NAMES_CLAUDE_FRONTIER, settings, false);
```

**Common pitfalls:**
- *Wrong key in `openaikey`*: the wrappers are strictly separated. `OpenAIService` reads `openaikey`, `XaiService` reads `xaikey`. Putting your xAI key in `openaikey` will cause `https://api.openai.com` to reject it with HTTP 401.
- *Confidence thresholds*: the entries in `MODEL_PROBABILITY_THRESHOLDS` for `grok-*` are starter values, not empirically calibrated. Run `OllamaConfidenceThresholdTuning` against your own evaluation prompts before relying on the confident-vote bucket in production.
- *Reasoning detection*: `OpenAISession.isReasoningModel` only matches OpenAI's o1/o3/o4 family. Grok's reasoning modes are gated on the API side (e.g. `grok-3-fast` vs `grok-3`) and do not need the parameter-omission branch — `temperature` and `top_p` are always accepted.

### MCP example server (dev/test)

A Docker-based MCP fetch server is available:

```bash
cd scripts/mcp
docker build -t mcp-fetch .
docker run -p 8080:8080 mcp-fetch
```

This starts an `mcp-server-fetch` instance behind `mcp-proxy` on port 8080.

### Required external dependencies at runtime

| Dependency | Required when | Notes |
|---|---|---|
| Ollama server | `use_openai=false`, `use_claude=false` | Default provider |
| OpenAI API access | `use_openai=true` | Requires valid `openaikey` |
| Anthropic API access | `use_claude=true` | Requires valid `claudekey` |
| xAI API access | `use_xai=true` | Requires valid `xaikey` (Grok models) |
| MCP server(s) | `mcp_scan=true` or `mcp_satellites` non-empty | Optional; tools unavailable without |
| Qwen3 TTS endpoint | `qwen3tts_enable=true` | Optional voice output |

---

## 15. Open Questions & Future Work

### Known design gaps
- MCP endpoint authentication is absent. When MCP servers require auth headers, the library provides no mechanism to supply them.
- All retry logic is blocking. High-concurrency callers that queue many sessions will exhaust the thread pool during Ollama restarts. Async retry (CompletableFuture + scheduled retry) should replace the current `Thread.sleep` approach.
- No structured logging format (JSON log lines) — makes log aggregation in production environments harder. Logback configuration should be standardised and bundled.
- `MCPServerForExamples.get_pi()` intentionally returns a prompt injection payload (for testing). This must never be started in production; a test-only Maven profile or guard should enforce this.

### Recommended next steps
1. Add WireMock-based Ollama stub so integration tests run in CI without a live GPU server.
2. Implement load-aware dispatch in the orchestrator client (`orchestrator_url` → route across `Server.priority` / `Server.capacity` / `Server.active_requests`).
3. Replace blocking retry sleeps with scheduled async retries.
4. Define a JSON logging profile (logback JSON encoder) for production deployments.
5. Add per-`MCPEndpoint` auth header configuration.
6. Calibrate the starter thresholds for `o1`/`o3`/`o4-mini`/`gpt-4.1*`/`gpt-5*` via `OllamaConfidenceThresholdTuning` (the model identifiers and reasoning-model parameter handling are already wired up; only the empirical numbers in `MODEL_PROBABILITY_THRESHOLDS` are starter values).
