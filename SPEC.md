# SPEC — Coding Agent Harness for AI Companion Applications

> **项目类型**：A · Coding Agent Harness  
> **提出者**：30461  
> **日期**：2026-07-07  
> **状态**：Draft（设计阶段，尚未实现）

---

## 一、问题陈述

### 要解决什么问题？

当前 AI 编码智能体（Claude Code、Cursor、GitHub Copilot 等）已经能完成大量编码工作，但大多数开发者只是把它们当作"高级代码补全"使用。在 AI 陪伴应用这一垂直领域，存在两个相互关联的问题：

1. **非技术用户无法快速构建 AI 陪伴应用**：一个想拥有"知心姐姐聊天伙伴"的用户，不具备从零搭建 Web 聊天应用 + LLM 集成 + 对话记忆系统的能力。
2. **AI 陪伴应用的核心难点——记忆——缺乏开箱即用的工程方案**：普通聊天应用可以靠向 LLM 回传原始历史的方式"记住"对话，但随着对话增长，上下文窗口和成本会爆炸。滑动窗口 + 摘要 + 语义检索是已知的最优实践，但很少有工具把它打包为可生成的代码模块。

### 目标用户

- 有基本命令行操作能力的用户（能运行 `java -jar`）
- 想快速构建、拥有一款个性化 AI 陪伴 Web 应用（如心理咨询伙伴、学习助手、虚拟朋友）
- 不需要理解 LLM API、向量检索、对话记忆等底层实现

### 为什么值得做？

- AI 陪伴是 LLM 在 C 端最高频的使用场景之一（Character.AI、Replika 均已验证需求）
- 现有的 AI 编码智能体都是"通用型"的——它们什么都能做，但在任何特定领域都不够"深"
- 本项目探索的是"领域特化 coding agent harness"是否可行：聚焦一个窄领域，把该领域的工程最佳实践编码进 harness 的模板和反馈机制里

---

## 二、用户故事

遵循 INVEST 原则（Independent, Negotiable, Valuable, Estimable, Small, Testable）。

| # | 用户故事 | 验收条件 |
|---|---------|----------|
| **US1** | 作为一个用户，我可以运行 `java -jar harness.jar` 启动 harness CLI，以便开始交互 | CLI 成功启动，显示欢迎信息和命令提示；无任何报错 |
| **US2** | 作为一个新用户，首次运行时我被引导安全地录入我的 DeepSeek API Key，key 存入系统凭据管理器 | 输入被遮蔽（不回显明文）；key 存储后可通过 `config status` 查看状态（不显示明文）；可更新和清除 |
| **US3** | 作为一个用户，我输入 `new "一个温柔的知心姐姐"`，harness 自动为我生成一个带 Web 聊天界面的陪伴应用项目 | 生成的项目目录结构完整；`mvn package` 成功；启动后可浏览器访问聊天界面 |
| **US4** | 作为一个用户，生成的陪伴应用能记住我和它之前的对话——当我关闭浏览器后重新打开，它知道我们聊过什么 | 重启应用后发送"还记得我们上次聊了什么吗"，AI 的回复引用到历史对话内容 |
| **US5** | 作为一个用户，当我输入一个模糊的需求描述（如"做个陪聊"），harness 会追问澄清细节（UI 风格、AI 性格、色调等） | harness 至少追问 3 个澄清问题后才开始生成代码 |
| **US6** | 作为一个用户，harness 在生成代码后会自动运行测试，如果测试失败会自动尝试修复 | 测试运行结果在终端可见；失败时可见修复尝试；最终生成的代码通过所有测试 |
| **US7** | 作为一个用户，我可以对已有项目说 `continue` 继续进行迭代开发，harness 记得这个项目之前的决策 | 再次进入同一个项目时，harness 加载历史决策并据此调整行为 |
| **US8** | 作为一个用户，如果 harness 想要执行危险操作（如删除文件），它应该暂停并请求我的确认 | 危险操作被拦截；终端显示"是否允许执行 X？(y/n)"；用户拒绝则跳过 |

---

## 三、功能规约

### 模块 1：CLI 入口（`cli`）

| 项 | 描述 |
|----|------|
| **输入** | 命令行参数或交互式 REPL 输入 |
| **行为** | 解析命令（`new`, `continue`, `config`, `exit`, `help`），分发到对应处理器 |
| **输出** | 终端展示结果（进度、成功/失败消息、错误详情） |
| **边界条件** | 无参数运行时进入 REPL 模式；`CTRL+C` 优雅退出 |
| **错误处理** | 非法命令给出提示和用法示例；异常不崩溃 |

**子命令**：

```
new "<描述>"          — 创建新的 AI 陪伴应用项目
continue <项目名>    — 继续迭代已有项目
config                — 查看/修改/清除 API key 配置
list                  — 列出所有已创建的项目
help                  — 显示帮助
exit                  — 退出
```

### 模块 2：Agent 主循环（`loop`）

| 项 | 描述 |
|----|------|
| **输入** | `ProjectContext`（项目路径、用户需求、对话历史） + `ToolRegistry` |
| **行为** | 1. `ContextBuilder` 构建本轮上下文（系统提示 + 工具定义 + 历史 + 反馈）→ 2. `LlmProvider.complete()` 调用 LLM → 3. 解析 LLM 响应中的 action → 4. `GuardChain.check(action)` → 5. 通过则分发执行 → 6. `FeedbackSensor.sense()` 收集客观信号 → 7. `StopJudge.decide()` 判断是否停机 |
| **输出** | `LoopResult`（SUCCESS / FAILED / NEEDS_HUMAN / MAX_TURNS） |
| **最大轮次** | 默认 30，可配置 |
| **重试机制** | 同一 task 失败后最多自动修正 3 轮，超过 → NEEDS_HUMAN |
| **停机条件** | 测试全绿 + 无编译错误 + StopJudge 判定完成 |

### 模块 3：工具系统（`tools`）

| 工具 | 参数 | 行为 | Guard 级别 | 反馈信号 |
|------|------|------|------------|----------|
| `file_read` | `path` | 读取项目内文件，返回内容 | SAFE | — |
| `file_write` | `path`, `content` | 创建或覆盖文件 | WARNING（超出项目目录时 CRITICAL） | 写入文件路径 |
| `file_delete` | `path` | 删除项目内文件 | CRITICAL（始终需确认） | 被删路径 |
| `shell_exec` | `command`, `cwd` | 在项目目录下执行命令 | WARNING（不在白名单 → CRITICAL） | stdout/stderr + exitCode |
| `test_run` | — | 运行 `mvn test`，解析结果 | SAFE | 测试通过/失败详情 |
| `dependency_add` | `groupId`, `artifactId`, `version` | 修改 pom.xml 添加依赖 | WARNING（仅允许白名单坐标） | 修改后的 pom.xml |
| `scaffold_new` | `projectName`, `template` | 用模板生成项目骨架 | SAFE | 生成的文件列表 |
| `project_summary` | — | 列出当前项目结构 | SAFE | 结构文本 |
| `memory_search` | `query` | 搜索 harness 记忆库 | SAFE | 匹配条目 |
| `memory_save` | `key`, `value`, `metadata` | 存入记忆 | SAFE | 确认 |
| `memory_inject` | `projectName` | 将 ProjectMemoryRuntime 注入目标项目 | SAFE | 注入的依赖和文件 |

### 模块 4：治理护栏（`guard`）

**护栏链**：`FileGuard → ShellGuard → NetworkGuard`

```java
public interface Guard {
    GuardResult check(Action action, ProjectContext ctx);
}

record GuardResult(boolean allowed, String reason, String requiredApproval);
```

| 护栏 | 触发条件 | 拦截行为 |
|------|----------|----------|
| **FileGuard** | 路径指向项目目录外；文件名匹配 `.env`/`.git`/凭据文件；系统目录写入 | 拦截 → 请求人工确认 |
| **ShellGuard** | 命令不在白名单中；包含 `rm -rf`, `chmod 777`, `sudo`, `> /dev/` 等危险模式 | 拦截 → 请求人工确认 |
| **NetworkGuard** | `shell_exec` 中检测到网络请求目标不在允许列表中 | 拦截 → 请求人工确认 |

**HITL 状态机**：

```
IDLE → (收到危险 action) → AWAITING_APPROVAL
      → (用户输入 y/yes)  → APPROVED → EXECUTING → IDLE
      → (用户输入 n/no)   → DENIED → SKIPPED → IDLE
      → (超时 120s)       → DENIED → SKIPPED → IDLE
```

**判据**：`guard.check(action)` 是纯 Java 逻辑，不依赖 LLM。Mock 测试：构造 `Action(command="rm -rf /")`，断言 `allowed=false`。

### 模块 5：反馈闭环（`feedback`）

| 项 | 描述 |
|----|------|
| **传感器** | `TestFeedbackSensor` — 运行 `mvn test`，解析输出，返回 `FeedbackResult` |
| **结果类型** | `allPassed`, `compileErrors[]`, `testFailures[]`, `lintWarnings[]` |
| **回灌格式** | 结构化文本注入下一轮 LLM context：`## Feedback\nCompile errors: ...\nTest failures: ...` |
| **失败优先级** | 编译错误 > 测试失败 > lint 警告 |
| **重试上限** | 同一 task 最多 3 次自我修正 |

**判据**：`TestFeedbackSensor.sense()` 是确定性的——它跑的是真实的 `mvn test`，结果来自 JUnit，不是 LLM 的自我评估。

### 模块 6：记忆系统（`memory`）★ 重点维度

#### 6.1 Harness 记忆（`HarnessMemory`）

- **功能**：跨项目记住用户偏好和历史决策
- **存储内容**：每个项目的元数据（名称、描述、创建时间、最后状态）；用户的全局偏好（默认 UI 风格、AI 性格偏好、色调）；harness 每次成功/失败的修复策略
- **检索方式**：key-value 精确查询 + 关键词模糊搜索
- **存储后端**：默认 SQLite（`~/.coding-harness/memory.db`），文件持久化

#### 6.2 项目记忆运行时（`ProjectMemoryRuntime`）

- **功能**：harness 生成陪伴应用时，将该项目记忆运行时注入到生成的项目中，提供开箱即用的对话记忆能力
- **存储内容**：完整对话历史（最近 N 轮完整保留）；每超过 M 轮的旧对话自动压缩为摘要；摘要向量化（使用 embedding API）存入索引；重要时刻标记（用户情绪转折、关键话题）
- **检索策略**：
  - **滑动窗口**：最近 N 轮完整上下文直接注入 LLM prompt（默认 N=20）
  - **语义检索**：当前对话与历史摘要做向量相似度匹配，召回最相关的 K 条历史话题（默认 K=5）
  - **重要性衰减**：历史话题按时间 + 交互深度计算权重衰减
- **注入方式**：作为 Maven 模块 / 代码片段注入生成项目
- **存储后端**：生成项目默认使用 JSON 文件存储（嵌入式，零外部依赖），可选升级 SQLite

#### 6.3 可插拔存储后端

```java
public interface MemoryStore {
    void save(String key, String value, Map<String, String> metadata);
    List<MemoryEntry> search(String query);  // 关键词匹配
    Optional<MemoryEntry> get(String key);
    void delete(String key);
    List<MemoryEntry> listRecent(int limit);
}
```

实现：
- **`InMemoryStore`** — HashMap，单元测试用
- **`SQLiteStore`** — JDBC SQLite，harness 默认
- **`FileJsonStore`** — JSON 文件，生成项目默认

### 模块 7：LLM 抽象层（`llm`）

| 项 | 描述 |
|----|------|
| **接口** | `LlmProvider.complete(LlmRequest) → LlmResponse` |
| **DeepSeekProvider** | HTTP 调用 DeepSeek API（OpenAI 兼容格式），使用 OkHttp |
| **MockLlmProvider** | 预设响应队列，支持"首次返回失败修复建议 → 第二次返回成功"的脚本化流程 |
| **请求结构** | `model`, `messages[]`, `tools[]`, `max_tokens`, `temperature` |
| **响应结构** | `content`, `toolCalls[]`, `finishReason`, `tokenUsage` |

### 模块 8：配置系统（`config`）

- **配置文件**：`~/.coding-harness/config.yml`（YAML 格式，SnakeYAML 解析）
- **内容**：`max_turns`, `auto_approve_level`, `model`, `provider`, `base_url`, `memory.backend`, `guard.shell_whitelist`
- **硬纪律**：API key 不在此文件中，走凭据管理器

### 模块 9：凭据管理（`credentials`）

- **实现方式**：Java 通过 `java.awt.SystemTray` / Windows Credential Manager 桥接；或使用跨平台的 `java.util.prefs.Preferences` + AES 加密（带主密码）作为 fallback
- **功能**：首次引导录入（隐藏输入）、查看状态（仅显示是否已配置 + 最后 4 位，绝不回显明文）、更新（覆盖）、清除
- **威胁模型**：见 §四

### 模块 10：项目代码生成（`scaffold`）

- **模板引擎**：Mustache（`com.github.spullara.mustache.java`），模板文件内嵌在 JAR 中
- **生成物**：标准 Maven Spring Boot Web 项目，包含：
  - `pom.xml`（含 Spring Boot Starter Web、Thymeleaf、memory 模块依赖）
  - 聊天 Web UI（Thymeleaf 模板 + 基础 CSS/JS）
  - `ChatController`（REST + WebSocket）
  - `MemoryConfig`（注入 ProjectMemoryRuntime）
  - `application.yml`（服务端口等）
  - 完整测试（`ChatControllerTest` 等）

---

## 四、非功能性需求

### 性能

- harness 自身启动时间 ≤ 3 秒（不含 LLM 调用）
- 单轮 LLM 调用超时 120 秒
- 生成项目的 Web 应用内存占用 ≤ 256MB

### 安全（含凭据威胁模型）

#### 威胁模型

| 威胁 | 风险等级 | 对策 |
|------|----------|------|
| API Key 被提交进 Git | 高 | `.gitignore` 阻断 `.env`；Key 存入系统凭据管理器，不落盘 |
| 凭据通过环境变量泄露 | 中 | 不通过命令行 `export` 传递；进程启动后从凭据管理器读取，不在 shell history 留痕 |
| 凭据文件被其他进程读取 | 中 | Windows Credential Manager 提供进程级访问控制 |
| harness 生成的项目中硬编码 key | 高 | 生成的模板中 key 从 `CREDENTIAL_STORE` 抽象读取，不在源码中出现 |
| 日志泄露 key | 中 | 日志过滤器脱敏：匹配 key 模式（`sk-...`）替换为 `***` |
| LLM 请求被中间人拦截 | 中 | 所有 LLM 调用强制 HTTPS；OkHttp 默认验证 TLS 证书 |

#### 凭据声明周期管理

```
录入 → 使用（从凭据管理器读取，不缓存明文）→ 更新 → 清除
                                               │
                                               ▼
                                    退出时提醒用户"清除还是保留"
```

### 可用性

- 首次运行体验 ≤ 3 步即可开始创建项目
- 所有错误消息附带"你可以尝试"的建议
- `help` 命令列出所有子命令和示例

### 可观测性

- 每轮 LLM 调用记录：token 用量、耗时、finishReason
- harness 自身日志（SLF4J + Logback），不记录 API key
- `--verbose` 模式显示完整 context 和 prompt

---

## 五、系统架构

### 组件图

```
┌─────────────────────────────────────────────────────┐
│                   Harness CLI                        │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐ │
│  │  CliMain  │  │  Config  │  │  CredentialStore  │ │
│  │  (REPL)   │  │  Manager │  │  (WinCredMgr)     │ │
│  └─────┬─────┘  └──────────┘  └───────────────────┘ │
│        │                                              │
│  ┌─────┴──────────────────────────────────────────┐  │
│  │                AgentLoop                         │  │
│  │  ┌────────────┐  ┌──────────┐  ┌────────────┐ │  │
│  │  │ContextBldr │→│ LLM.call │→│ ActionParser│ │  │
│  │  └────────────┘  └──────────┘  └─────┬──────┘ │  │
│  │                      ↑               │         │  │
│  │              ┌───────┴───────┐       │         │  │
│  │              │ LlmProvider   │       ▼         │  │
│  │              │ (iface)       │  ┌──────────┐   │  │
│  │              ├───────────────┤  │ GuardChain│  │  │
│  │              │ DeepSeek│Mock │  └────┬─────┘   │  │
│  │              └───────────────┘       │         │  │
│  │                                      ▼         │  │
│  │              ┌──────────┐  ┌────────────────┐  │  │
│  │              │Feedback  │◀─│  ToolExecutor  │  │  │
│  │              │Sensor    │  │  (ToolRegistry) │  │  │
│  │              └────┬─────┘  └────────────────┘  │  │
│  │                   │                             │  │
│  │              ┌────┴─────┐                       │  │
│  │              │StopJudge │                       │  │
│  │              └──────────┘                       │  │
│  └─────────────────────────────────────────────────┘  │
│        │                                              │
│  ┌─────┴──────────┐  ┌──────────────┐                │
│  │  MemorySystem   │  │  Scaffolder  │                │
│  │  ├HarnessMemory │  │  (Mustache)  │                │
│  │  └ProjMemRuntime│  └──────────────┘                │
│  └─────────────────┘                                  │
└──────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────┐
│                   Generated Web App                   │
│  ┌─────────┐  ┌──────────┐  ┌────────────────────┐  │
│  │Web UI   │  │Chat Ctrl │  │ProjectMemoryRuntime│  │
│  │(HTML/JS)│  │(REST/WS) │  │(对话记忆+检索)      │  │
│  └─────────┘  └──────────┘  └────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### 数据流

```
用户输入 → CliMain → AgentLoop:
                      1. ContextBuilder 拼装上下文
                      2. LlmProvider 调用 LLM
                      3. LLM 返回 action
                      4. GuardChain 检查
                      5. ToolExecutor 执行
                      6. FeedbackSensor 采集反馈
                      7. 结果回灌到第1步（下一轮）
                      8. StopJudge 判定停机
```

### 外部依赖

- **DeepSeek API**（`api.deepseek.com`）：LLM 推理
- **DeepSeek Embedding API**（可选）：生成项目中的向量检索
- **Maven Central**：运行时下载依赖（仅生成项目时）
- **无需数据库服务**：SQLite 是嵌入式文件数据库

---

## 六、数据模型

### HarnessMemory 实体

```
UserProject {
    id: String (UUID)
    name: String
    description: String
    createdAt: DateTime
    lastAccessedAt: DateTime
    status: Enum[ACTIVE, ARCHIVED, FAILED]
    preferences: Map<String, String>  // LLM 性格、色调等
}

UserPreference {
    key: String              // e.g., "default_tone", "default_color"
    value: String
    updatedAt: DateTime
}

DecisionRecord {
    id: String
    projectId: String
    context: String          // 当时在做什么
    decision: String         // 做了什么决定
    outcome: String          // 结果如何
    timestamp: DateTime
}
```

### ProjectMemoryRuntime 实体（生成项目中使用）

```
ConversationTurn {
    id: String
    role: Enum[USER, ASSISTANT, SYSTEM]
    content: String
    timestamp: DateTime
    emotion: String?         // 可选：情绪标注
}

ConversationSummary {
    id: String
    startTurnId: String
    endTurnId: String
    summary: String          // LLM 生成的摘要
    embedding: float[]       // 向量（用于语义检索）
    topics: List<String>     // 话题标签
    importance: float        // 重要性权重
    createdAt: DateTime
}

ImportantMoment {
    id: String
    turnId: String
    label: String            // e.g., "user_shared_trauma", "breakthrough"
    note: String
    createdAt: DateTime
}
```

---

## 七、凭据与分发设计

### 凭据方案

- **方案**：Windows Credential Manager（主）+ AES 加密 Preferences（fallback，跨平台）
- **流程**：首次运行 → 终端隐蔽输入（`System.console().readPassword()`）→ 写入 Credential Manager → 使用时不缓存明文
- **验证**：启动时从 Credential Manager 读取，尝试一次简单的 API ping，确认 key 有效；无效则提示重新录入
- **配置文件 `~/.coding-harness/config.yml` 中绝不存储 key**

### 分发方案

- **形态**：fat JAR（`harness.jar`），Maven Shade Plugin 打包所有依赖
- **获取**：GitHub Releases 下载 `harness.jar`
- **运行**：`java -jar harness.jar`（需要 JDK 17+）
- **目标平台**：Windows 10/11 + JDK 17；Linux/macOS 理论兼容（凭据管理器 fallback 到 AES 加密文件）
- **已知限制**：需要 JDK 17+；`mvn` 命令需要在 PATH 中（用于生成项目的编译和测试）

---

## 八、技术选型与理由

| 项目 | 选择 | 理由 |
|------|------|------|
| **语言** | Java 17 | 强类型系统、成熟测试生态（JUnit+Mockito）、JDBC 内建、企业级可维护性，与"机制必须是代码"的硬要求高度契合 |
| **构建工具** | Maven | 用户偏好；shade plugin 成熟；依赖管理清晰 |
| **HTTP 客户端** | OkHttp 4 | 成熟、高效、支持拦截器（日志脱敏） |
| **JSON** | Jackson 2 | Java 生态事实标准 |
| **模板引擎** | Mustache（spullara） | 轻量、逻辑-less 模板，适合代码生成场景 |
| **LLM 供应商** | DeepSeek | 用户偏好；OpenAI 兼容格式，便于扩展其他供应商 |
| **记忆存储** | SQLite（JDBC）+ JSON | 零运维、嵌入式、文本型存储适合对话数据 |
| **日志** | SLF4J + Logback | 标准组合，支持日志脱敏 |
| **测试** | JUnit 5 + Mockito + AssertJ | Java 测试标准栈 |
| **前端（生成项目）** | Thymeleaf + 原生 JS | 服务端渲染简单、无需 npm 生态、Java 开发者直接上手 |

**前端豁免**：本项目为纯 CLI 工具（harness 自身），不涉及前端开发，依通用要求豁免 Open Design。

---

## 九、验收标准

| 功能 | 验收标准 |
|------|----------|
| CLI 启动 | `java -jar harness.jar` 显示欢迎信息，`help` 列出所有命令 |
| 凭据管理 | 首次运行引导输入 key；`config status` 显示"已配置 (***-xxxx)"；`config clear` 后消失 |
| 创建项目 | `new "温柔知心姐姐"` 生成完整 Maven 项目，`mvn package` 通过 |
| 生成项目可运行 | `java -jar target/app.jar` 后 `curl localhost:8080` 返回 200 |
| 生成项目记忆 | 连续对话 5 轮 → 关闭 → 重启 → 问"还记得之前聊什么吗" → 回复引用历史内容 |
| 测试通过 | `mvn test` 全部绿色，覆盖核心逻辑 |
| 护栏拦截 | mock 模式下：构造 `rm -rf /` 命令 → 被拦截 → 等待确认 |
| 反馈闭环 | mock 模式下：注入编译错误 → agent 收到反馈 → 下一步动作为"修复编译错误" |
| Mock 单测 | 替换为 MockLlmProvider 后，`mvn test` 可在离线环境下全部通过 |
| 分发 | `java -jar harness.jar` 在仅安装 JDK 17 的干净机器上可运行 |
| CI | push 后 GitHub Actions / GitLab CI 自动运行 `mvn test` 并 pass |

---

## 十、领域与机制设计（A 项目专节）

### 领域分析

本 harness 聚焦 **AI 陪伴应用构建** 这一垂直领域。

#### 反馈信号
- **编译结果**（`mvn compile`）：最基础的门槛，代码必须能编译
- **测试结果**（`mvn test`）：JUnit 测试的确定性通过/失败信号
- **生成项目可运行性**：`java -jar` 能否成功启动并监听端口
- **HTTP 可达性**：`GET /` 返回 200 表示 Web 应用已正常运行

#### 危险动作
- 删除生成项目目录外的任何文件
- 执行不在白名单中的 shell 命令
- 修改 harness 自身的配置文件
- 在生成的代码中硬编码 API key

#### 所需工具
- 文件读写、删除（受护栏约束）
- Shell 执行（白名单：`mvn`、`java`、`npm`（仅打包））
- 项目脚手架生成
- Maven 依赖管理
- 测试运行与结果解析

#### 记忆需求
- **Harness 层**：用户全局偏好 + 每个项目的元数据 + 历史决策
- **项目层**：对话历史 + 摘要 + 向量检索 + 重要时刻

### 重点维度：记忆

记忆是本项目的 Main Contribution。深入实现的理由：

1. **垂直领域的核心价值**：AI 陪伴应用的核心竞争力是"记住对话、建立关系"，记忆系统的质量直接决定陪伴体验的好坏
2. **最需要工程化的维度**：滑动窗口 + 摘要 + 语义检索的组合在概念上简单，但实际工程实现涉及的摘要调度、向量存储、检索融合都有可观的代码量
3. **双层记忆的设计挑战**：harness 自身记忆 + 生成项目的运行时记忆，需要统一接口 + 可插拔后端，但场景完全不同
4. **天然适合 Java 实现**：接口/多态模式、JDBC 存储、集合框架等 Java 基本能力能直接发挥

### 机制编码实现计划

| 机制 | 实现方式 | 可单测？ |
|------|----------|----------|
| 滑动窗口 | `SlidingWindowManager` 类：维护固定大小的对话轮次环形缓冲 | ✅ mock LLM 喂入对话，断言 context size |
| 摘要调度 | `SummaryScheduler` 类：超过阈值轮次触发摘要生成（调用一次 LLM 生成摘要 → 持久化 → 清空旧轮次） | ✅ mock LLM 返回预设摘要，断言摘要存储 |
| 语义检索 | `SemanticRetriever` 类：当前 query → embedding → 与历史摘要做余弦相似度匹配 → 返回 top-K | ✅ mock embedding API，断言检索结果 |
| Harness 记忆 | `HarnessMemory` 类：SQLite + 关键词搜索 | ✅ InMemoryStore 替换 SQLite，断言 CRUD |

---

## 十一、风险与未决问题

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| **DeepSeek API 不稳定或限流** | LLM 调用失败导致任务中断 | 重试+退避策略；mock 模式保证开发和测试不受影响 |
| **生成的 Web 应用质量参差** | 不同需求生成的代码质量波动大 | 模板约束骨架质量；测试反馈确保至少基本可运行 |
| **嵌入向量成本** | 生成项目中的语义检索需要 embedding API，增加用户成本 | 默认使用关键词匹配；语义检索作为可选增强 |
| **Windows Credential Manager 跨平台兼容** | macOS/Linux 上不可用 | 实现 AES 加密文件 fallback |
| **用户需求过于模糊** | LLM 无法从一句话中提取足够信息 | 在 AgentLoop 中内建追问机制（US5），强制至少收集 3 个澄清点 |
| **生成的代码量超出 LLM 单次输出限制** | 大型项目无法一次生成 | 分模块生成（先骨架→再控制器→再前端），每轮生成一个文件 |
| **向量检索性能** | 纯 Java 实现余弦相似度在大数据量下效率低 | 初期使用阈值过滤（仅比较最近 N 条摘要）；后续可引入 HNSW 索引 |

---

## 十二、附录

### 目录结构（规划）

```
coding-harness/
├── SPEC.md                     # 本文件
├── PLAN.md                     # 实现计划（writing-plans 产出）
├── SPEC_PROCESS.md             # brainstorming 过程记录
├── AGENT_LOG.md                # 实现过程日志
├── README.md                   # 项目说明
├── REFLECTION.md               # 反思报告
├── .gitignore
├── .gitlab-ci.yml              # CI 配置
├── pom.xml                     # Maven POM
├── Dockerfile                  # 可选容器分发
└── src/
    ├── main/java/com/codingharness/
    │   ├── CliMain.java           # CLI 入口 + REPL
    │   ├── core/
    │   │   ├── AgentLoop.java     # 主循环
    │   │   ├── ContextBuilder.java
    │   │   ├── ActionParser.java
    │   │   ├── StopJudge.java
    │   │   └── ToolExecutor.java
    │   ├── tools/
    │   │   ├── Tool.java          # 接口
    │   │   ├── ToolRegistry.java
    │   │   ├── FileReadTool.java
    │   │   ├── FileWriteTool.java
    │   │   ├── FileDeleteTool.java
    │   │   ├── ShellExecTool.java
    │   │   ├── TestRunTool.java
    │   │   ├── DependencyAddTool.java
    │   │   ├── ScaffoldNewTool.java
    │   │   └── ProjectSummaryTool.java
    │   ├── guard/
    │   │   ├── Guard.java         # 接口
    │   │   ├── GuardChain.java
    │   │   ├── FileGuard.java
    │   │   ├── ShellGuard.java
    │   │   ├── NetworkGuard.java
    │   │   └── HitlStateMachine.java
    │   ├── feedback/
    │   │   ├── FeedbackSensor.java  # 接口
    │   │   ├── FeedbackResult.java
    │   │   └── TestFeedbackSensor.java
    │   ├── memory/
    │   │   ├── MemoryStore.java     # 接口
    │   │   ├── MemoryEntry.java
    │   │   ├── HarnessMemory.java
    │   │   ├── ProjectMemoryRuntime.java
    │   │   ├── SlidingWindowManager.java
    │   │   ├── SummaryScheduler.java
    │   │   ├── SemanticRetriever.java
    │   │   ├── InMemoryStore.java
    │   │   ├── SQLiteStore.java
    │   │   └── FileJsonStore.java
    │   ├── llm/
    │   │   ├── LlmProvider.java     # 接口
    │   │   ├── LlmRequest.java
    │   │   ├── LlmResponse.java
    │   │   ├── DeepSeekProvider.java
    │   │   └── MockLlmProvider.java
    │   ├── config/
    │   │   ├── ConfigManager.java
    │   │   └── HarnessConfig.java
    │   ├── credentials/
    │   │   ├── CredentialStore.java  # 接口
    │   │   ├── WinCredentialStore.java
    │   │   └── AesCredentialStore.java
    │   └── scaffold/
    │       ├── Scaffolder.java
    │       ├── TemplateEngine.java
    │       └── templates/           # Mustache 模板
    │           ├── pom.xml.mustache
    │           ├── ChatController.java.mustache
    │           ├── memory-module/
    │           └── web-ui/
    └── test/java/com/codingharness/
        ├── core/AgentLoopTest.java
        ├── tools/ToolRegistryTest.java
        ├── guard/
        │   ├── FileGuardTest.java
        │   ├── ShellGuardTest.java
        │   └── GuardChainTest.java
        ├── feedback/TestFeedbackSensorTest.java
        ├── memory/
        │   ├── InMemoryStoreTest.java
        │   ├── SQLiteStoreTest.java
        │   ├── SlidingWindowManagerTest.java
        │   └── SemanticRetrieverTest.java
        ├── llm/MockLlmProviderTest.java
        └── demo/
            ├── GuardrailDemo.java       # 机制演示①
            ├── FeedbackLoopDemo.java    # 机制演示②
            └── MemoryMechanismDemo.java # 机制演示③
```
