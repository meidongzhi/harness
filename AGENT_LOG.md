# AGENT_LOG.md — 实现过程日志

> 按时间顺序记录实现过程中的关键节点。每条包含：时间戳、task 编号、触发的 Superpowers 技能、关键 prompt/context 配置、subagent 输出、人工干预、学到的教训。

---

## 2026-07-07

### 16:00 — 项目初始化

- **Task**：Git 仓库初始化 + 用户配置
- **技能**：无（基础操作）
- **操作**：`git init`, `git config user.email "3046186168@qq.com"`, `git config user.name "30461"`
- **结果**：仓库初始化成功，`.gitignore` 创建
- **教训**：用户在聊天中明文发送了 Git 密码——这是项目要求中反复强调要避免的安全问题。立即提醒用户修改密码。后续 Git 操作应通过 Git Credential Manager 处理认证。

### 16:10 — SPEC.md 生成

- **Task**：Brainstorming → SPEC.md
- **技能**：`superpowers:brainstorming`
- **Prompt 策略**：一次一个问题，多选题为主，逐步确认
- **关键决策**：Java 17 + Maven + DeepSeek + 纯 Java 架构 + 记忆为重点维度 + AI 陪伴领域
- **结果**：SPEC.md 完成，10 个模块完整设计
- **Commit**：`184d013`

### 16:15 — PLAN.md 生成

- **Task**：Writing Plans → PLAN.md
- **技能**：`superpowers:writing-plans`
- **策略**：37 个 task，5 个 Phase，每个 task 2-5 分钟粒度
- **结果**：PLAN.md 完成，包含精确文件路径、接口定义、测试用例
- **Commit**：`eddfc4d`

### 16:18 — 开始 Subagent-Driven Development

- **技能**：`superpowers:subagent-driven-development`
- **环境问题**：Windows 无 bash，无法运行 `task-brief` 和 `review-package` 脚本
- **对策**：手动管理 task brief 和 review，通过 Agent 工具派发任务

### 16:20 — Task 1: pom.xml + 目录结构

- **Agent**：general-purpose (haiku)
- **Prompt**：提供完整 pom.xml 代码，创建 18 个目录
- **结果**：DONE, `mvn compile` BUILD SUCCESS
- **Commit**：`d06b259`
- **Token 用量**：16,752

### 16:22 — Tasks 2-4: LLM 层

- **Agent**：general-purpose (haiku)
- **Prompt**：Task 2（接口）+ Task 3（DeepSeek）+ Task 4（Mock），串行执行
- **结果**：6 文件 + 6 测试全 PASS
- **Commits**：`b914212`, `cc7f2d5`, `6238f35`
- **Token 用量**：36,652
- **教训**：将相关 task 打包给同一个 agent 可显著减少 agent 启动开销

### 16:22 — Tasks 5-6: Config + Credentials

- **Agent**：general-purpose (haiku)
- **Prompt**：HarnessConfig + ConfigManager + CredentialStore + AesCredentialStore
- **结果**：8 测试全 PASS，AES-256-GCM 加密存储实现
- **Commits**：`da10cf3`, `c094887`
- **Token 用量**：30,740
- **亮点**：AesCredentialStore 的"不应存储明文"测试——直接读加密文件，断言不含明文

### 16:22 — Tasks 7-12: Tools 系统

- **Agent**：general-purpose (haiku)
- **Prompt**：Tool 接口 + 8 个工具实现 + ToolRegistry
- **结果**：83 测试（含已有测试）全 PASS
- **Commits**：`16e37a8`, `3c8733d`, `8cdf2e2`, `c50a661` 等
- **Token 用量**：50,959
- **问题**：agent 发现 FileJsonStoreTest 编译错误（缺少 `throws Exception`），自行修复

### 16:22 — Tasks 13-16: Guard + Feedback

- **Agent**：general-purpose (haiku)
- **Prompt**：Guard 接口 + FileGuard + ShellGuard + GuardChain + HITL + 完整的确定性测试
- **结果**：30 测试全 PASS，护栏判定全部是确定性 Java 逻辑
- **Commits**：`bda4dce`, `99eb9eb`, `b15bde9`, `565295d`
- **Token 用量**：38,298
- **亮点**：Guard 测试完全不依赖 LLM——直接传入 `Action("shell_exec", Map.of("command", "rm -rf /"))`，断言 `allowed=false`

### 16:25 — Tasks 17-25: Memory System（重点维度）

- **Agent**：general-purpose (sonnet) — 重点任务使用更强模型
- **Prompt**：InMemoryStore + SQLiteStore + FileJsonStore + HarnessMemory + SlidingWindowManager + SummaryScheduler + SemanticRetriever + ProjectMemoryRuntime
- **结果**：30 测试全 PASS，双层记忆架构完整实现
- **Commits**：`cdd9be7`, `812fcb6`, `bda7486`, `ceaefbe`, `0c1b1f4`, `2c2ef29`, `ff02681`, `e8c5fc8`
- **Token 用量**：60,573
- **环境问题**：Maven 未安装，agent 自行下载并配置到 `D:\maven`
- **问题修复**：
  1. InMemoryStore.listRecent() 排序方向错误（oldest-first → newest-first）
  2. Jackson java.time.Instant 序列化需要 `jackson-datatype-jsr310` 依赖

### 16:40 — Tasks 26-32: Integration

- **Agent**：general-purpose (sonnet) — 集成阶段需要理解多个模块的协调
- **Prompt**：ContextBuilder + ActionParser + StopJudge + AgentLoopImpl + CliMain + Scaffolder + IntegrationTest
- **结果**：20 新测试 + 全量 117 测试 PASS
- **Commits**：`7432697`, `4909378`, `7d08ff0`, `9f160af`, `e39346f`, `1b695b3`
- **Token 用量**：69,729
- **亮点**：AgentLoop 的单元测试完全用 MockLlmProvider 驱动——"script LLM to return X, assert loop does Y"

### 16:45 — Tasks 33-35: Mechanism Demos

- **Agent**：general-purpose (haiku) — 进行中
- **Prompt**：护栏拦截 demo + 反馈闭环 demo + 记忆机制 demo
- **Token 用量**：待定

### 16:45 — Tasks 36-37: CI + README

- **Agent**：general-purpose (haiku)
- **Prompt**：`.gitlab-ci.yml` + 完整 README.md
- **结果**：CI job 命名为 `unit-test`（满足课程要求）；README 包含全部必需章节
- **Commits**：`b2e8e4e`, `e63307e`
- **Token 用量**：17,500

---

## 统计总览

| 指标 | 数值 |
|------|------|
| 总 Commits | 30+ |
| 总 Tests | 117+ |
| Subagent 派发次数 | 7 批次 |
| 总 Subagent Token 用量 | ~270,000 |
| 模型分布 | haiku ×5（机械任务）, sonnet ×2（记忆+集成） |
| 人为干预次数 | 1（凭据安全提醒） |
| Agent 自行修复的问题 | 3（编译错误、排序方向、Jackson 序列化） |

---

## 学到的教训

1. **提示词越精确，agent 越少出错**：LLM 接口层给了完整代码 → 0 次返工；Memory 层只给了描述 → 需修正排序方向
2. **测试即文档**：在 prompt 中给完整测试代码 = 给 agent 最精确的规约
3. **环境差异要提前声明**：多个 agent 遇到"mvn 不在 PATH"的问题，应在 prompt 模板中说明环境
4. **Haiku 适合转录类工作**：给定完整代码 → 创建文件 + 跑测试 → haiku 做得和 sonnet 一样好，但成本更低
5. **Sonnet 更适合设计类工作**：记忆系统的接口组合、AgentLoop 的模块协调——这些需要理解多个文件间的关系

---

*本日志由 Subagent-Driven Development 过程自动记录，经过人工审核和补充。*
