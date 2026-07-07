# Coding Agent Harness — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Java CLI coding agent harness that helps users create AI companion web applications, with memory as the deep-dive dimension.

**Architecture:** Plain Java 17 CLI application, layered into llm → tools → guard → feedback → memory → loop → cli. Memory is the star feature with pluggable backends (SQLite/JSON/InMemory) and a dual-layer design (harness memory + runtime injectable into generated projects).

**Tech Stack:** Java 17, Maven, OkHttp 4, Jackson 2, Mustache, SQLite JDBC, JUnit 5, Mockito, AssertJ

## Global Constraints

- Java 17+, Maven 3.8+
- All core mechanisms must be testable with MockLlmProvider (no real LLM dependency in unit tests)
- API key never hardcoded, never in config files, never committed to git
- TDD strict: failing test → pass → refactor → commit per task
- Each task ends with a standalone, testable deliverable
- Commit message format: `feat(module): description` or `test(module): description`
- Maximum 30 agent loop turns, maximum 3 self-correction retries per task
- Generated projects must compile and pass tests autonomously

---

## Dependency & Parallelism Overview

```
Phase 1: Foundation (sequential)
  T01 pom.xml + dirs → T02 LlmProvider → T03 MockLlmProvider → T04 Config → T05 Credentials

Phase 2: Core Mechanics (parallel within groups)
  Group A: T06 Tool → T07-T11 Tools → T12 ToolRegistry
  Group B: T13 Guard → T14-T15 Guards → T16 HITL
  Group C: T17 FeedbackSensor → T18 TestFeedbackSensor

Phase 3: Memory System (main contribution)
  T19 MemoryStore → T20-T22 Backends → T23 HarnessMemory
  → T24 SlidingWindow → T25 SummaryScheduler → T26 ProjectMemoryRuntime

Phase 4: Integration
  T27 AgentLoop → T28 ContextBuilder → T29 CLI → T30 Scaffolder → T31 Integration Tests

Phase 5: Demos & CI
  T32 Mechanism Demos → T33 CI Config → T34 README
```

---

## Phase 1: Foundation

### Task 1: Project scaffolding — pom.xml and directory structure

**Files:**
- Create: `pom.xml`
- Create: all package directories under `src/main/java/com/codingharness/`

**Interfaces:**
- Produces: Maven project that builds successfully

- [ ] **Step 1: Create pom.xml** (see SPEC §八 for full dependency list). Key deps: OkHttp 4.12.0, Jackson 2.17.0, SnakeYAML 2.2, SQLite JDBC 3.45.1.0, Mustache compiler 0.9.11, SLF4J 2.0.12 + Logback 1.5.3, JUnit 5.10.2, Mockito 5.10.0, AssertJ 3.25.3. Shade plugin with mainClass=com.codingharness.CliMain.

- [ ] **Step 2: Create directory structure**
```bash
mkdir -p src/main/java/com/codingharness/{core,tools,guard,feedback,memory,llm,config,credentials,scaffold}
mkdir -p src/test/java/com/codingharness/{core,tools,guard,feedback,memory,llm,demo}
mkdir -p src/main/resources/templates
```

- [ ] **Step 3: Verify** — `mvn compile` → BUILD SUCCESS

- [ ] **Step 4: Commit** — `git add pom.xml && git commit -m "feat: add Maven POM with all dependencies"`

### Task 2: LLM abstraction interfaces

**Files:**
- Create: `src/main/java/com/codingharness/llm/LlmProvider.java`
- Create: `src/main/java/com/codingharness/llm/LlmRequest.java`
- Create: `src/main/java/com/codingharness/llm/LlmResponse.java`

**Interfaces:**
- Produces: `LlmProvider` — interface with `complete(LlmRequest) → LlmResponse` and `getName() → String`
- Produces: `LlmRequest` — record with `model`, `messages` (List<Message>), `tools` (List<ToolDef>), `maxTokens`, `temperature`
- Produces: `LlmResponse` — record with `content`, `toolCalls` (List<ToolCall>), `finishReason`, `tokenUsage` (TokenUsage record)
- Produces: inner records `Message(String role, String content)` with static factory methods `system()`, `user()`, `assistant()`
- Produces: inner records `ToolDefinition`, `ToolCall`, `TokenUsage`

- [ ] **Step 1: Write all three files** — LlmRequest is a record with nested Message/ToolDefinition records; LlmResponse is a record with nested ToolCall/TokenUsage records; LlmProvider is a single-method interface.

- [ ] **Step 2: Verify** — `mvn compile` → BUILD SUCCESS

- [ ] **Step 3: Commit** — `git add src/main/java/com/codingharness/llm/ && git commit -m "feat(llm): add LlmProvider interface with request/response records"`

### Task 3: DeepSeek provider

**Files:**
- Create: `src/main/java/com/codingharness/llm/DeepSeekProvider.java`
- Create: `src/test/java/com/codingharness/llm/DeepSeekProviderTest.java`

**Interfaces:**
- Consumes: `LlmProvider` (T02)
- Produces: `DeepSeekProvider` — OkHttp-based, OpenAI-compatible format

- [ ] **Step 1: Write failing test** — `shouldConstructWithApiKey()`, `shouldRejectNullApiKey()`, `shouldRejectBlankApiKey()` — all construction-only, no real API calls.

- [ ] **Step 2: Run to verify failure** — `mvn test -Dtest=DeepSeekProviderTest` → FAIL

- [ ] **Step 3: Implement** — Constructor validates apiKey non-null/non-blank; OkHttpClient with 30s connect + 120s read timeouts; `complete()` builds OpenAI-format JSON body, sends POST to `{baseUrl}/v1/chat/completions`, parses response. Jackson ObjectMapper for serialization. Log API calls without key.

- [ ] **Step 4: Run tests** — `mvn test -Dtest=DeepSeekProviderTest` → PASS

- [ ] **Step 5: Commit** — `git add ... && git commit -m "feat(llm): implement DeepSeekProvider with OpenAI-compatible API calls"`

### Task 4: MockLlmProvider

**Files:**
- Create: `src/main/java/com/codingharness/llm/MockLlmProvider.java`
- Create: `src/test/java/com/codingharness/llm/MockLlmProviderTest.java`

**Interfaces:**
- Consumes: `LlmProvider` (T02)
- Produces: `MockLlmProvider` — takes `List<LlmResponse>` in constructor, returns them in FIFO order, repeats last when queue exhausted

- [ ] **Step 1: Write failing test** — `shouldReturnScriptedResponsesInOrder()`, `shouldReturnLastResponseWhenQueueExhausted()`, `shouldThrowWhenInitializedWithEmptyList()`

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — Queue<LlmResponse> with LinkedBlockingQueue, `complete()` polls queue, returns lastResponse if null

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(llm): add MockLlmProvider for deterministic offline testing"`

### Task 5: Configuration system

**Files:**
- Create: `src/main/java/com/codingharness/config/HarnessConfig.java`
- Create: `src/main/java/com/codingharness/config/ConfigManager.java`
- Create: `src/test/java/com/codingharness/config/ConfigManagerTest.java`

**Interfaces:**
- Produces: `HarnessConfig` record — `maxTurns`, `autoApproveLevel`, `model`, `provider`, `baseUrl`, `memoryBackend`, `memoryDbPath`, `shellWhitelist`
- Produces: `ConfigManager` — `load()` returns defaults if file missing; `save(HarnessConfig)` writes YAML

- [ ] **Step 1: Write HarnessConfig record** with static `defaults()` factory returning: maxTurns=30, autoApproveLevel="WARNING", model="deepseek-chat", provider="deepseek", baseUrl="https://api.deepseek.com", memoryBackend="sqlite", memoryDbPath="~/.coding-harness/memory.db", shellWhitelist=["mvn","java","npm"]

- [ ] **Step 2: Write failing ConfigManager test** — default-on-missing, save+load round-trip, custom YAML parsing

- [ ] **Step 3: Run to verify failure** → FAIL

- [ ] **Step 4: Implement ConfigManager** — SnakeYAML parsing, create parent dirs on save, type-safe getters with defaults

- [ ] **Step 5: Run tests** → PASS

- [ ] **Step 6: Commit** — `git commit -m "feat(config): add YAML-based configuration system with defaults"`

### Task 6: Credential store (AES encrypted)

**Files:**
- Create: `src/main/java/com/codingharness/credentials/CredentialStore.java`
- Create: `src/main/java/com/codingharness/credentials/AesCredentialStore.java`
- Create: `src/test/java/com/codingharness/credentials/CredentialStoreTest.java`

**Interfaces:**
- Produces: `CredentialStore` — `store(key, value)`, `retrieve(key) → Optional<String>`, `exists(key) → boolean`, `delete(key)`, `maskedDisplay(key) → String`
- Produces: `AesCredentialStore` — AES/GCM/NoPadding encrypted file storage, PBKDF2 key derivation

- [ ] **Step 1: Write CredentialStore interface** — 5 methods as above

- [ ] **Step 2: Write test** — CRUD, missing key returns empty, masked display shows `***-last6chars`, raw storage file does NOT contain plaintext

- [ ] **Step 3: Run to verify failure** → FAIL

- [ ] **Step 4: Implement AesCredentialStore** — AES-256-GCM, PBKDF2WithHmacSHA256 (100k iterations), Java serialized Map encrypted on disk, load on startup, persist on mutation

- [ ] **Step 5: Run tests** → PASS

- [ ] **Step 6: Commit** — `git commit -m "feat(credentials): add AES-encrypted credential store with masked display"`

---

## Phase 2: Core Mechanics

### Task 7: Tool interface and ToolResult

**Files:**
- Create: `src/main/java/com/codingharness/tools/Tool.java`
- Create: `src/main/java/com/codingharness/tools/ToolResult.java`

**Interfaces:**
- Produces: `Tool` — `name()`, `description()`, `parameters() → Map<String,Object>` (JSON Schema), `execute(Map<String,Object> args, ProjectContext ctx) → ToolResult`
- Produces: `ToolResult` — `success`, `output`, `error`

- [ ] **Step 1: Write Tool interface and ToolResult record** — ToolResult is `record ToolResult(boolean success, String output, String error) {}` with static factory `success(String output)` and `failure(String error)`. Tool is the interface above.

- [ ] **Step 2: Verify** — `mvn compile` → BUILD SUCCESS

- [ ] **Step 3: Commit** — `git commit -m "feat(tools): add Tool interface and ToolResult record"`

### Task 8: File I/O tools (FileReadTool, FileWriteTool, FileDeleteTool)

**Files:**
- Create: `src/main/java/com/codingharness/tools/FileReadTool.java`
- Create: `src/main/java/com/codingharness/tools/FileWriteTool.java`
- Create: `src/main/java/com/codingharness/tools/FileDeleteTool.java`
- Create: `src/test/java/com/codingharness/tools/FileToolsTest.java`

**Interfaces:**
- Consumes: `Tool` (T07)
- Produces: FileReadTool — reads file content, fails if path outside project; FileWriteTool — writes file, returns path; FileDeleteTool — deletes file, returns path

- [ ] **Step 1: Write failing tests** — read existing file, read nonexistent file, write new file, write outside project boundary, delete file, delete nonexistent file. All use `@TempDir`.

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement three tools** — each takes `ProjectContext` to resolve paths and check boundaries; FileReadTool returns file content; FileWriteTool creates parent dirs; FileDeleteTool only allows files inside project root.

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(tools): add FileReadTool, FileWriteTool, FileDeleteTool with boundary checks"`

### Task 9: ShellExecTool

**Files:**
- Create: `src/main/java/com/codingharness/tools/ShellExecTool.java`
- Create: `src/test/java/com/codingharness/tools/ShellExecToolTest.java`

**Interfaces:**
- Consumes: `Tool` (T07)
- Produces: ShellExecTool — executes shell command in project directory, captures stdout/stderr/exitCode

- [ ] **Step 1: Write failing test** — execute `echo hello`, capture stdout, exitCode=0; execute nonexistent command, capture error

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — `ProcessBuilder` with working directory = project root, redirect stderr to stdout, 30s timeout, return ToolResult with exit code and combined output

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(tools): add ShellExecTool with ProcessBuilder and timeout"`

### Task 10: TestRunTool

**Files:**
- Create: `src/main/java/com/codingharness/tools/TestRunTool.java`
- Create: `src/test/java/com/codingharness/tools/TestRunToolTest.java`

**Interfaces:**
- Consumes: `Tool` (T07)
- Produces: TestRunTool — runs `mvn test` in project, parses results

- [ ] **Step 1: Write failing test** — Given a mock project dir with a pom.xml (no tests), run TestRunTool, assert output contains "BUILD SUCCESS" or equivalent

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — Executes `mvn test` via ProcessBuilder, parses stdout for pass/fail/test counts, returns structured output

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(tools): add TestRunTool that runs mvn test and parses results"`

### Task 11: ScaffoldNewTool + DependencyAddTool + ProjectSummaryTool + MemoryTools

**Files:**
- Create: `src/main/java/com/codingharness/tools/ScaffoldNewTool.java`
- Create: `src/main/java/com/codingharness/tools/DependencyAddTool.java`
- Create: `src/main/java/com/codingharness/tools/ProjectSummaryTool.java`
- Create: `src/main/java/com/codingharness/tools/MemorySearchTool.java`
- Create: `src/main/java/com/codingharness/tools/MemorySaveTool.java`
- Create: `src/test/java/com/codingharness/tools/RemainingToolsTest.java`

**Interfaces:**
- Consumes: `Tool` (T07), `MemoryStore` (T19 — stub it)
- Produces: ScaffoldNewTool (creates project dir), DependencyAddTool (modifies pom.xml), ProjectSummaryTool (lists files), MemorySearchTool, MemorySaveTool

- [ ] **Step 1: Write failing tests** — scaffold creates directory skeleton; dependency add appends to pom.xml; project summary lists files; memory save+search round-trip (using InMemoryStore stub)

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement all five tools** — each minimal implementation, ScaffoldNewTool creates basic dir+files, DependencyAddTool inserts XML into pom.xml, etc.

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(tools): add ScaffoldNewTool, DependencyAddTool, ProjectSummaryTool, and memory tools"`

### Task 12: ToolRegistry

**Files:**
- Create: `src/main/java/com/codingharness/tools/ToolRegistry.java`
- Create: `src/test/java/com/codingharness/tools/ToolRegistryTest.java`

**Interfaces:**
- Consumes: `Tool` (T07)
- Produces: `ToolRegistry` — `register(Tool)`, `get(name) → Optional<Tool>`, `listAll() → List<Tool>`, `listForLLM() → List<LlmRequest.ToolDefinition>`

- [ ] **Step 1: Write failing test** — register tools, get by name, list all, convert to LLM format

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — HashMap-backed registry, `listForLLM()` converts each Tool to a ToolDefinition for LLM consumption

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(tools): add ToolRegistry for tool registration and LLM format conversion"`

### Task 13: Guard interface and GuardResult

**Files:**
- Create: `src/main/java/com/codingharness/guard/Guard.java`
- Create: `src/main/java/com/codingharness/guard/GuardResult.java`
- Create: `src/main/java/com/codingharness/core/Action.java`

**Interfaces:**
- Produces: `Guard` — `check(Action, ProjectContext) → GuardResult`
- Produces: `GuardResult` — `allowed`, `reason`, `requiredApproval` (SAFE/WARNING/CRITICAL)
- Produces: `Action` — record with `type`, `parameters` (Map<String,Object>)

- [ ] **Step 1: Write all three files** — Action is a simple record; GuardResult is a record with static factory `allow()`, `block(String reason)`, `requireApproval(String reason)`; Guard is the single-method interface.

- [ ] **Step 2: Verify** — `mvn compile` → BUILD SUCCESS

- [ ] **Step 3: Commit** — `git commit -m "feat(guard): add Guard interface, GuardResult, and Action record"`

### Task 14: FileGuard and ShellGuard

**Files:**
- Create: `src/main/java/com/codingharness/guard/FileGuard.java`
- Create: `src/main/java/com/codingharness/guard/ShellGuard.java`
- Create: `src/test/java/com/codingharness/guard/GuardTest.java`

**Interfaces:**
- Consumes: `Guard` (T13)
- Produces: FileGuard — checks path is inside project boundary, blocks sensitive file patterns (.env, .git); ShellGuard — blocks dangerous patterns (rm -rf, sudo, chmod 777), whitelist check

- [ ] **Step 1: Write failing test** — FileGuard: write inside project → allowed, write to /etc → blocked, write to .env → blocked; ShellGuard: `mvn test` → allowed, `rm -rf /` → blocked, `sudo rm` → blocked

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — FileGuard resolves canonical path and checks startsWith(projectRoot), also checks keyword blacklist on path; ShellGuard uses regex patterns for dangerous commands, checks command[0] against whitelist

- [ ] **Step 4: Run tests** → PASS (Note: these are deterministic Java logic — no LLM involved!)

- [ ] **Step 5: Commit** — `git commit -m "feat(guard): add FileGuard and ShellGuard with deterministic danger detection"`

### Task 15: GuardChain and HITL State Machine

**Files:**
- Create: `src/main/java/com/codingharness/guard/GuardChain.java`
- Create: `src/main/java/com/codingharness/guard/HitlStateMachine.java`
- Create: `src/test/java/com/codingharness/guard/GuardChainTest.java`

**Interfaces:**
- Consumes: `Guard` (T13)
- Produces: GuardChain — runs action through ordered list of Guards, first block wins; HitlStateMachine — states IDLE/AWAITING_APPROVAL/APPROVED/DENIED, timeout=120s

- [ ] **Step 1: Write failing test** — chain with FileGuard+ShellGuard, `rm -rf /` blocked by ShellGuard first, safe `echo hello` passes all; HITL: idle→awaiting→approved→idle, idle→awaiting→timeout→denied

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — GuardChain iterates guards, returns first blocking result; HitlStateMachine implements the state transitions per SPEC §三 module 4

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(guard): add GuardChain and HITL state machine"`

### Task 16: FeedbackSensor and TestFeedbackSensor

**Files:**
- Create: `src/main/java/com/codingharness/feedback/FeedbackSensor.java`
- Create: `src/main/java/com/codingharness/feedback/FeedbackResult.java`
- Create: `src/main/java/com/codingharness/feedback/TestFeedbackSensor.java`
- Create: `src/test/java/com/codingharness/feedback/TestFeedbackSensorTest.java`

**Interfaces:**
- Produces: `FeedbackSensor` — `sense(ProjectContext) → FeedbackResult`
- Produces: `FeedbackResult` — `allPassed`, `failures` (List<TestFailure>), `errors` (List<CompileError>), `warnings` (List<String>)
- Produces: TestFeedbackSensor — runs `mvn test`, parses JUnit output

- [ ] **Step 1: Write FeedbackSensor interface and FeedbackResult record** — FeedbackResult has nested TestFailure(file, testName, message) and CompileError(file, line, message) records

- [ ] **Step 2: Write failing test for TestFeedbackSensor** — given project with passing tests, assert allPassed=true; given project with failing test, assert test name and message captured

- [ ] **Step 3: Run to verify failure** → FAIL

- [ ] **Step 4: Implement TestFeedbackSensor** — runs `mvn test`, regex-parses JUnit output for failures and errors, returns structured FeedbackResult

- [ ] **Step 5: Run tests** → PASS

- [ ] **Step 6: Commit** — `git commit -m "feat(feedback): add FeedbackSensor interface and TestFeedbackSensor for mvn test parsing"`

---

## Phase 3: Memory System ★ (Main Contribution)

### Task 17: MemoryStore interface

**Files:**
- Create: `src/main/java/com/codingharness/memory/MemoryStore.java`
- Create: `src/main/java/com/codingharness/memory/MemoryEntry.java`

**Interfaces:**
- Produces: `MemoryStore` — `save(key, value, metadata)`, `search(query) → List<MemoryEntry>`, `get(key) → Optional<MemoryEntry>`, `delete(key)`, `listRecent(limit) → List<MemoryEntry>`
- Produces: `MemoryEntry` — `key`, `value`, `metadata` (Map<String,String>), `timestamp` (Instant)

- [ ] **Step 1: Write MemoryEntry record and MemoryStore interface** — MemoryEntry has factory `MemoryEntry.of(key, value)` with timestamp=now and empty metadata. MemoryStore is 5 methods as above.

- [ ] **Step 2: Verify** — `mvn compile` → BUILD SUCCESS

- [ ] **Step 3: Commit** — `git commit -m "feat(memory): add MemoryStore interface and MemoryEntry record"`

### Task 18: InMemoryStore

**Files:**
- Create: `src/main/java/com/codingharness/memory/InMemoryStore.java`
- Create: `src/test/java/com/codingharness/memory/InMemoryStoreTest.java`

**Interfaces:**
- Consumes: `MemoryStore` (T17)
- Produces: InMemoryStore — ConcurrentHashMap-backed, for unit tests

- [ ] **Step 1: Write failing test** — CRUD, search by keyword, search returns empty for no match, listRecent returns correct count in reverse chronological order

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — ConcurrentHashMap<String, MemoryEntry>, search() checks key and value for containsIgnoreCase, listRecent() sorts by timestamp desc

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(memory): add InMemoryStore for unit test usage"`

### Task 19: SQLiteStore

**Files:**
- Create: `src/main/java/com/codingharness/memory/SQLiteStore.java`
- Create: `src/test/java/com/codingharness/memory/SQLiteStoreTest.java`

**Interfaces:**
- Consumes: `MemoryStore` (T17)
- Produces: SQLiteStore — JDBC SQLite, file-based persistence

- [ ] **Step 1: Write failing test** — Same CRUD+search+listRecent tests as InMemoryStore, but uses SQLite file. Create table on init: `CREATE TABLE IF NOT EXISTS memories (key TEXT PRIMARY KEY, value TEXT, metadata TEXT, timestamp TEXT)`

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — JDBC prepared statements, Jackson for metadata JSON serialization, auto-creates DB file and table

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(memory): add SQLiteStore for persistent file-based memory"`

### Task 20: FileJsonStore

**Files:**
- Create: `src/main/java/com/codingharness/memory/FileJsonStore.java`
- Create: `src/test/java/com/codingharness/memory/FileJsonStoreTest.java`

**Interfaces:**
- Consumes: `MemoryStore` (T17)
- Produces: FileJsonStore — single JSON file per storage unit, zero-dependency format for generated projects

- [ ] **Step 1: Write failing test** — CRUD, file persistence survives JVM restart (write → create new store from same file → read), JSON file is valid and human-readable

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — Jackson reads/writes Map<String,MemoryEntry> as JSON array to a single file, auto-creates directory on write

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(memory): add FileJsonStore for zero-dependency project memory"`

### Task 21: HarnessMemory

**Files:**
- Create: `src/main/java/com/codingharness/memory/HarnessMemory.java`
- Create: `src/test/java/com/codingharness/memory/HarnessMemoryTest.java`

**Interfaces:**
- Consumes: `MemoryStore` (T17)
- Produces: HarnessMemory — domain-specific wrapper: `rememberProject(projectId, metadata)`, `getProjectPreferences(projectId) → Map<String,String>`, `recordDecision(projectId, context, decision, outcome)`, `searchDecisions(projectId, query) → List<DecisionRecord>`

- [ ] **Step 1: Write failing test** — save project preferences, retrieve them, record decisions and search them, preferences survive between HarnessMemory instances (using same SQLiteStore)

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — Wraps MemoryStore, uses key patterns like `project:{id}:prefs`, `project:{id}:decision:{uuid}`, search delegates to store.search() with projectId prefix filter

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(memory): add HarnessMemory for cross-project preferences and decisions"`

### Task 22: SlidingWindowManager

**Files:**
- Create: `src/main/java/com/codingharness/memory/SlidingWindowManager.java`
- Create: `src/test/java/com/codingharness/memory/SlidingWindowManagerTest.java`

**Interfaces:**
- Consumes: none (pure data structure)
- Produces: SlidingWindowManager — `addTurn(TurnRecord)`, `getWindowTurns() → List<TurnRecord>`, `windowSize`, `getFullHistory() → List<TurnRecord>`

- [ ] **Step 1: Write failing test** — add 30 turns, getWindowTurns returns last N (default 20), getFullHistory returns all 30; add turn beyond window, older turns still in fullHistory but not in window; overflow threshold triggers summary generation signal

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — ArrayList for full history, subList for window; `exceedsThreshold(threshold)` returns true when fullHistory.size() % threshold == 0; `getTurnsForSummarization()` returns oldest batch exceeding window

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(memory): add SlidingWindowManager for conversation window management"`

### Task 23: SummaryScheduler

**Files:**
- Create: `src/main/java/com/codingharness/memory/SummaryScheduler.java`
- Create: `src/test/java/com/codingharness/memory/SummarySchedulerTest.java`

**Interfaces:**
- Consumes: `LlmProvider` (T02), `MemoryStore` (T17)
- Produces: SummaryScheduler — `shouldSummarize(SlidingWindowManager) → boolean`, `generateSummary(List<TurnRecord>, LlmProvider) → ConversationSummary`, `storeSummary(ConversationSummary, MemoryStore)`

- [ ] **Step 1: Write failing test** — detect when summarization needed (turns > threshold), generate summary via MockLlmProvider (scripted response), store summary in InMemoryStore, verify summary content

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — When turns exceed threshold * batch number, takes oldest N turns, calls LLM with prompt "Summarize this conversation in 2-3 sentences: [turns]", stores result with metadata (startTurnId, endTurnId, topics[]). Uses MockLlmProvider in tests.

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(memory): add SummaryScheduler for automatic conversation compression"`

### Task 24: SemanticRetriever

**Files:**
- Create: `src/main/java/com/codingharness/memory/SemanticRetriever.java`
- Create: `src/test/java/com/codingharness/memory/SemanticRetrieverTest.java`

**Interfaces:**
- Consumes: `MemoryStore` (T17), embedding provider (optional, stubbable)
- Produces: SemanticRetriever — `retrieveRelevant(query, MemoryStore, topK) → List<ConversationSummary>`, cosine similarity computation in pure Java

- [ ] **Step 1: Write failing test** — store 5 summaries with pre-computed embedding vectors; query with a vector close to summary #3; assert #3 is in top-2 results; test cosine similarity math with known vectors

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — Pure Java cosine similarity: `dot(a,b) / (norm(a) * norm(b))`. Retrieves all summaries from store, computes similarity, returns top-K. Embedding lookup is through a stubbable `EmbeddingProvider` interface (single method `float[] embed(String text)`)

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(memory): add SemanticRetriever with pure Java cosine similarity"`

### Task 25: ProjectMemoryRuntime

**Files:**
- Create: `src/main/java/com/codingharness/memory/ProjectMemoryRuntime.java`
- Create: `src/test/java/com/codingharness/memory/ProjectMemoryRuntimeTest.java`

**Interfaces:**
- Consumes: `MemoryStore` (T17), `SlidingWindowManager` (T22), `SummaryScheduler` (T23), `SemanticRetriever` (T24)
- Produces: ProjectMemoryRuntime — the orchestration layer that combines all memory components into a single injectable module

- [ ] **Step 1: Write failing test** — Add conversation turns → window fills → summary generated → semantic retrieval finds relevant history. Full pipeline test with MockLlmProvider and InMemoryStore.

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — Orchestrates SlidingWindowManager + SummaryScheduler + SemanticRetriever. Main API: `addTurn(role, content)` → triggers summarization if needed → updates window; `getContextForLLM()` → returns window turns + top-K relevant summaries; `getImportantMoments()` → returns marked moments

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(memory): add ProjectMemoryRuntime as the unified memory orchestration layer"`

---

## Phase 4: Integration

### Task 26: ProjectContext

**Files:**
- Create: `src/main/java/com/codingharness/core/ProjectContext.java`

**Interfaces:**
- Produces: `ProjectContext` — `projectRoot` (Path), `projectName`, `createdAt`, `harnessMemory` (HarnessMemory), `config` (HarnessConfig)

- [ ] **Step 1: Write ProjectContext record** — simple record holding project state, with factory method `create(name, root, memory, config)`

- [ ] **Step 2: Verify** — `mvn compile` → BUILD SUCCESS

- [ ] **Step 3: Commit** — `git commit -m "feat(core): add ProjectContext record"`

### Task 27: ContextBuilder

**Files:**
- Create: `src/main/java/com/codingharness/core/ContextBuilder.java`
- Create: `src/test/java/com/codingharness/core/ContextBuilderTest.java`

**Interfaces:**
- Consumes: `ToolRegistry` (T12), `ProjectContext` (T26)
- Produces: ContextBuilder — `build(ToolRegistry, ProjectContext, List<TurnRecord>, FeedbackResult?) → LlmRequest`

- [ ] **Step 1: Write failing test** — build a request, verify system prompt is present, tools included, history included, feedback appended when present

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — Assemblies system prompt (coding agent role description + project context) → appends tool definitions → appends conversation history → appends feedback as `## Feedback\n...` block → builds LlmRequest with model from config

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(core): add ContextBuilder for LLM request assembly"`

### Task 28: ActionParser and StopJudge

**Files:**
- Create: `src/main/java/com/codingharness/core/ActionParser.java`
- Create: `src/main/java/com/codingharness/core/StopJudge.java`
- Create: `src/test/java/com/codingharness/core/ActionParserTest.java`
- Create: `src/test/java/com/codingharness/core/StopJudgeTest.java`

**Interfaces:**
- Consumes: `LlmResponse` (T02), `ToolRegistry` (T12)
- Produces: ActionParser — `parse(LlmResponse) → List<Action>`; StopJudge — `decide(history, feedback, maxTurns) → StopDecision` (enum: CONTINUE, SUCCESS, FAILED, NEEDS_HUMAN)

- [ ] **Step 1: Write failing tests** — ActionParser: parse response with tool_calls → Action list, parse response with content only → empty list; StopJudge: all tests pass + under maxTurns → SUCCESS, compile errors → CONTINUE, maxTurns exceeded → MAX_TURNS, 3 consecutive same-tool failures → NEEDS_HUMAN

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement** — ActionParser iterates LlmResponse.toolCalls(), maps to Actions; StopJudge is pure deterministic logic — checks feedback results, turns count, consecutive failure counter

- [ ] **Step 4: Run tests** → PASS

- [ ] **Step 5: Commit** — `git commit -m "feat(core): add ActionParser and StopJudge with deterministic logic"`

### Task 29: AgentLoop implementation

**Files:**
- Create: `src/main/java/com/codingharness/core/AgentLoop.java`
- Create: `src/main/java/com/codingharness/core/AgentLoopImpl.java`
- Create: `src/main/java/com/codingharness/core/TurnRecord.java`
- Create: `src/test/java/com/codingharness/core/AgentLoopTest.java`

**Interfaces:**
- Consumes: ALL previous modules
- Produces: AgentLoop — `run(ProjectContext, ToolRegistry, int maxTurns) → LoopResult`

- [ ] **Step 1: Write failing test with MockLlmProvider** — Script LLM to return "write file X" then "run tests" then "done". Verify loop executes 3 turns, tools are called, loop terminates with SUCCESS. Script LLM to return compile error → "fix code" → tests pass → verify self-correction works. Script LLM to always fail → verify max 3 retries → NEEDS_HUMAN.

- [ ] **Step 2: Run to verify failure** → FAIL

- [ ] **Step 3: Implement AgentLoopImpl** — The main loop from SPEC §三 module 2:
  1. ContextBuilder.build() → 2. LlmProvider.complete() → 3. ActionParser.parse()
  4. GuardChain.check() each action → 5. ToolExecutor.execute() approved actions
  6. FeedbackSensor.sense() → 7. StopJudge.decide() → loop or return

- [ ] **Step 4: Run tests** → PASS (with MockLlmProvider — no real LLM!)

- [ ] **Step 5: Commit** — `git commit -m "feat(core): implement AgentLoop with context-feedback-guard loop"`

### Task 30: CLI Main entry point

**Files:**
- Create: `src/main/java/com/codingharness/CliMain.java`

**Interfaces:**
- Consumes: ALL previous modules
- Produces: CLI with REPL: `new`, `continue`, `config`, `list`, `help`, `exit` commands

- [ ] **Step 1: Write CliMain** — Uses `System.console().readLine()` for REPL; `new "<desc>"` creates ProjectContext and calls AgentLoop.run(); `continue <name>` resumes existing project; `config` shows/adds/clears API key via CredentialStore; `list` shows all projects via HarnessMemory; `help` prints all commands; `exit` quits gracefully. First-run detection: if no API key configured, guide user through secure input (Console.readPassword()).

- [ ] **Step 2: Verify** — `mvn package -DskipTests` produces `target/coding-harness-1.0.0.jar`

- [ ] **Step 3: Commit** — `git commit -m "feat(cli): add REPL-based CLI with new/continue/config/list commands"`

### Task 31: Scaffolder (Mustache-based code generation)

**Files:**
- Create: `src/main/java/com/codingharness/scaffold/Scaffolder.java`
- Create: `src/main/java/com/codingharness/scaffold/MustacheRenderer.java`
- Create: `src/main/resources/templates/pom.xml.mustache`
- Create: `src/main/resources/templates/ChatController.java.mustache`
- Create: `src/main/resources/templates/application.yml.mustache`
- Create: `src/main/resources/templates/index.html.mustache`
- Create: `src/test/java/com/codingharness/scaffold/ScaffolderTest.java`

**Interfaces:**
- Consumes: ProjectContext (T26)
- Produces: MustacheRenderer — `render(templateName, model) → String`; Scaffolder — `scaffold(projectName, spec) → Path` (generated project root)

- [ ] **Step 1: Write templates** — pom.xml.mustache (Spring Boot Web + Thymeleaf + ProjectMemoryRuntime dep), ChatController.java.mustache (REST + injected memory), application.yml.mustache (port 8080), index.html.mustache (simple chat UI)

- [ ] **Step 2: Write MustacheRenderer** — loads templates from classpath resources, renders with Mustache compiler

- [ ] **Step 3: Write failing Scaffolder test** — scaffold a project, verify pom.xml has correct groupId/artifactId, verify ChatController imports ProjectMemoryRuntime, verify generated project compiles with `mvn compile`

- [ ] **Step 4: Run to verify failure** → FAIL

- [ ] **Step 5: Implement Scaffolder** — Creates directory, renders all templates, writes files

- [ ] **Step 6: Run tests** → PASS

- [ ] **Step 7: Commit** — `git commit -m "feat(scaffold): add Mustache-based project scaffold generator with templates"`

### Task 32: Integration test (end-to-end with MockLlmProvider)

**Files:**
- Create: `src/test/java/com/codingharness/IntegrationTest.java`

**Interfaces:**
- Consumes: ALL modules
- Produces: End-to-end test proving the full harness works with mocked LLM

- [ ] **Step 1: Write integration test** — Create MockLlmProvider with scripted sequence: (1) "I'll create the project structure" + scaffold tool call → (2) "Now let me add dependencies" + dependency_add tool call → (3) "Done, let me verify" + test_run tool call; Feedback returns allPassed. Assert loop terminates with SUCCESS, project directory exists, generated files are present.

- [ ] **Step 2: Run** — `mvn test -Dtest=IntegrationTest` → PASS

- [ ] **Step 3: Commit** — `git commit -m "test: add end-to-end integration test with MockLlmProvider"`

---

## Phase 5: Demos, CI, and Documentation

### Task 33: Mechanism Demo 1 — Guardrail interception

**Files:**
- Create: `src/test/java/com/codingharness/demo/GuardrailDemo.java`
- (runnable via `mvn test -Dtest=GuardrailDemo`)

**Goal:** Deterministically demonstrate guardrail intercepting a dangerous action, as required by SPEC §A.6.

- [ ] **Step 1: Write demo** — Creates FileGuard and ShellGuard; feeds them `Action("shell_exec", Map.of("command", "rm -rf /"))` → asserts blocked; feeds `Action("file_write", Map.of("path", "/etc/passwd", "content", "x"))` → asserts blocked (outside project); feeds `Action("file_write", Map.of("path", ".env", "content", "SECRET=xxx"))` → asserts blocked (sensitive file pattern). Prints results to stdout.

- [ ] **Step 2: Run** — `mvn test -Dtest=GuardrailDemo` → PASS (all assertions)

- [ ] **Step 3: Commit** — `git commit -m "demo: add guardrail interception demo (mechanism demo 1/3)"`

### Task 34: Mechanism Demo 2 — Feedback loop correction

**Files:**
- Create: `src/test/java/com/codingharness/demo/FeedbackLoopDemo.java`

**Goal:** Deterministically demonstrate feedback causing agent to change behavior.

- [ ] **Step 1: Write demo** — MockLlmProvider scripted: turn 1 returns "write file" action; TestFeedbackSensor returns compile error; turn 2 LLM receives feedback and returns "fix compile error" action. Assert turn 2 action targets the file with the error.

- [ ] **Step 2: Run** — `mvn test -Dtest=FeedbackLoopDemo` → PASS

- [ ] **Step 3: Commit** — `git commit -m "demo: add feedback loop correction demo (mechanism demo 2/3)"`

### Task 35: Mechanism Demo 3 — Memory mechanism (重点维度)

**Files:**
- Create: `src/test/java/com/codingharness/demo/MemoryMechanismDemo.java`

**Goal:** Deterministically demonstrate the full memory pipeline.

- [ ] **Step 1: Write demo** — (1) Create SlidingWindowManager, add 25 turns → assert window returns last 20, full history has 25; (2) Create SummaryScheduler with MockLlmProvider (scripted summary response "User discussed their favorite books"), feed turns → generate summary → assert summary stored in InMemoryStore; (3) Create SemanticRetriever, store summaries with known embedding vectors, query with similar vector → assert relevant summary retrieved; (4) Wire everything via ProjectMemoryRuntime → add turns → getContextForLLM → assert context contains both window turns and retrieved summary.

- [ ] **Step 2: Run** — `mvn test -Dtest=MemoryMechanismDemo` → PASS

- [ ] **Step 3: Commit** — `git commit -m "demo: add memory mechanism demo — sliding window + summary + retrieval (mechanism demo 3/3)"`

### Task 36: CI configuration (.gitlab-ci.yml)

**Files:**
- Create: `.gitlab-ci.yml`

**Goal:** CI that runs `mvn test` on every push, job named `unit-test`, last execution must pass.

- [ ] **Step 1: Write .gitlab-ci.yml**
```yaml
image: maven:3.9-eclipse-temurin-17

stages:
  - test

unit-test:
  stage: test
  script:
    - mvn test
  artifacts:
    when: always
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
```

- [ ] **Step 2: Commit** — `git commit -m "ci: add .gitlab-ci.yml with unit-test job"`

### Task 37: README.md

**Files:**
- Create: `README.md`

**Content must include per 通用要求 §五.4:**
- 项目简介 — what this harness does
- 安装 — `java -jar harness.jar` prerequisites (JDK 17+)
- 运行 — commands (new, continue, config, list, help, exit)
- 分发 — where to download JAR, how to build from source (`mvn package`)
- Key 安全配置 — how to set up DeepSeek API key (first-run guided input → Windows Credential Manager / AES fallback)
- 目录结构 — source tree overview
- 安全边界说明 — what the harness can/cannot do, guardrail explanation, credential threat model summary
- 已知限制 — Windows Credential Manager on Win only; JDK 17+ required; mvn in PATH

- [ ] **Step 1: Write README.md** with all required sections

- [ ] **Step 2: Commit** — `git commit -m "docs: add README.md with install, security, and distribution instructions"`

---

## Task Completion Record (任务完成记录)

### Phase 1: Foundation

| Task | 内容 | Commit | 状态 |
|------|------|--------|------|
| T01 | pom.xml + 目录结构 | `d06b259` | ✅ |
| T02 | LLM 接口 (LlmProvider/Request/Response) | `b914212` | ✅ |
| T03 | DeepSeekProvider | `cc7f2d5` | ✅ |
| T04 | MockLlmProvider | `6238f35` | ✅ |
| T05 | Config system (HarnessConfig + ConfigManager) | `da10cf3` | ✅ |
| T06 | Credential store (AesCredentialStore) | `c094887` | ✅ |

### Phase 2: Core Mechanics

| Task | 内容 | Commit | 状态 |
|------|------|--------|------|
| T07 | Tool interface + ToolResult | `16e37a8` | ✅ |
| T08 | File I/O tools | `3c8733d` | ✅ |
| T09 | ShellExecTool + TestRunTool | `8cdf2e2` | ✅ |
| T10 | ScaffoldNewTool + DependencyAddTool + ProjectSummaryTool | `c50a661` | ✅ |
| T11 | Memory tools | `c50a661` | ✅ |
| T12 | ToolRegistry | `c50a661` | ✅ |
| T13 | Guard interface + GuardResult + Action | `bda4dce` | ✅ |
| T14 | FileGuard + ShellGuard | `99eb9eb` | ✅ |
| T15 | GuardChain + HitlStateMachine | `b15bde9` | ✅ |
| T16 | FeedbackSensor + TestFeedbackSensor | `565295d` | ✅ |

### Phase 3: Memory System ★

| Task | 内容 | Commit | 状态 |
|------|------|--------|------|
| T17 | MemoryStore interface | `fcc2fc7` | ✅ |
| T18 | InMemoryStore + tests | `cdd9be7` | ✅ |
| T19 | SQLiteStore | `812fcb6` | ✅ |
| T20 | FileJsonStore | `bda7486` | ✅ |
| T21 | HarnessMemory (projects/preferences/decisions) | `ceaefbe` | ✅ |
| T22 | SlidingWindowManager | `0c1b1f4` | ✅ |
| T23 | SummaryScheduler | `2c2ef29` | ✅ |
| T24 | SemanticRetriever | `ff02681` | ✅ |
| T25 | ProjectMemoryRuntime | `e8c5fc8` | ✅ |

### Phase 4: Integration

| Task | 内容 | Commit | 状态 |
|------|------|--------|------|
| T26 | ProjectContext | `16e37a8` | ✅ |
| T27 | ContextBuilder | `7432697` | ✅ |
| T28 | ActionParser + StopJudge | `4909378` | ✅ |
| T29 | AgentLoopImpl | `7d08ff0` | ✅ |
| T30 | CLI Main | `9f160af` | ✅ |
| T31 | Scaffolder (Mustache) | `e39346f` | ✅ |
| T32 | Integration test | `1b695b3` | ✅ |

### Phase 5: Demos, CI, Docs

| Task | 内容 | Commit | 状态 |
|------|------|--------|------|
| T33 | Guardrail demo | `d7b8fa4` | ✅ |
| T34 | Feedback loop demo | `d7b8fa4` | ✅ |
| T35 | Memory mechanism demo | `d7b8fa4` | ✅ |
| T36 | CI (.gitlab-ci.yml) | `b2e8e4e` | ✅ |
| T37 | README.md | `e63307e` | ✅ |

### Additional

| 内容 | Commit | 状态 |
|------|--------|------|
| SPEC_PROCESS.md | `9b495c7` | ✅ |
| AGENT_LOG.md | `f91d990` | ✅ |
| Demo app + Dockerfile | `44552dc` | ✅ |
| GitHub Actions CI | `859a4a4` | ✅ |
| CLI real LLM support | `1d37c27` | ✅ |

---

## Final Checklist (对照交付物清单)

| # | 交付物 | 状态 |
|---|--------|------|
| 1 | SPEC.md | ✅ |
| 2 | PLAN.md | ✅ |
| 3 | SPEC_PROCESS.md | ✅ |
| 4 | 完整源码 + mock-LLM 单测 (124 tests) | ✅ |
| 5 | 分发产物 (fat JAR 23MB) | ✅ |
| 6 | README.md | ✅ |
| 7 | AGENT_LOG.md | ✅ |
| 8 | .gitlab-ci.yml + GitHub Actions | ✅ |
| 9 | CI/CD 执行记录 | ✅ (截图) |
| 10 | REFLECTION.md | ⏳ 学生手写 |
| 11 | 线上部署 URL | ⏳ natapp 临时 URL 已通，Render 永久部署未完成 |
| 12 | 机制演示 ×3 | ✅ |
| 13 | Git commit 历史 (35 commits) | ✅ |

