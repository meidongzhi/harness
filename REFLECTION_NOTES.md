# REFLECTION.md 思路笔记

> 这份文件是帮你写 REFLECTION.md 的思路素材，不是最终报告。最终报告你必须自己写。

---

## 1. Brainstorming 的作用

**可以写的点：**
- 7 个关键决策，智能体一次一个问题，帮你从模糊想法走到了清晰设计
- 最有价值的问题：「六个维度选哪个做深」「两层记忆都做还是聚焦一层」
- 假如没有 brainstorming，你可能在实现到一半时才发现漏了关键设计

**你的真实感受写进去**：你前面说"brainstorming 还是很有用的，至少让我全面地规划了任务"

---

## 2. TDD —— 帮助还是走过场？

**事实：**
- 124 个测试，subagent 写的，你个人没手写测试
- MockLlmProvider 让所有核心机制可以不联网测试
- 护栏测试是纯 Java 断言：`action("rm -rf /") → allowed=false`
- 反馈闭环测试：mock 返回失败 → 断言 agent 下一步修正

**你可以反思：**
- TDD 在 AI 协作场景下的价值是什么？是"保证代码正确"还是"给 agent 最精确的规约"？
- 测试代码比自然语言描述更精确——agent 看测试能理解你的期望

---

## 3. Subagent-Driven 工作流的得与失

**事实：**
- 37 个 task，7 批次 subagent，总计 ~270K tokens
- Haiku 做机械转录（LLM 接口），Sonnet 做记忆和集成
- 3 个 agent 可以同时跑（互不依赖时）

**好：**
- 速度：纯手工写可能一周，agent 一下午
- 隔离：一个 agent 干砸了不影响其他模块

**不好：**
- InMemoryStore 排序方向不一致——两个 agent 对"最近"的理解不同
- Agent 有时会自作主张——FileJsonStore 引用了还没创建的类
- 没有 PR review 过程，代码直接进 master

---

## 4. SPEC 质量如何影响实现质量

**具体案例 1 — InMemoryStore 排序：**
SPEC 说「返回最近 N 条」。Haiku agent 实现为 oldest-first，预期是 newest-first。这是 SPEC 没写清楚。

**具体案例 2 — ProjectContext 重复创建：**
PLAN 没标注哪些文件"已存在，不可重新创建"，导致多个 agent 各自创建 ProjectContext，产生了编译冲突。

**教训：**
- 即使"最近"这种日常词汇也要在 SPEC 明确定义
- PLAN 必须标注已有文件清单

---

## 5. Prompt 策略

**最有效的：**
- 完整代码模板 → agent 一次过（LLM 接口层，0 返工）
- 测试即规约 → agent 面向测试实现，不会跑偏

**不太有效的：**
- 只给自然语言描述 → agent 需要大量自主判断，容易出偏差

---

## 6. 代码审查的价值

**事实：**
- 第一轮审查发现 8 个致命问题：
  - 硬编码密码（`"coding-harness-master"`）
  - AES 解密失败静默吞错
  - DeepSeekProvider 对 API 响应无判空
  - AgentLoop 对 tool 执行异常无保护
  - 等等
- 全部是代码级别的 bug，不是"写的不好"——是真的会导致崩溃/安全问题

**可以写：**
- 审查前后代码质量差异
- "如果不是审查，这些 bug 会留到什么时候被发现"

---

## 7. 凭据与分发让你想清楚了什么

**凭据：**
- 初始实现用硬编码密码 `"coding-harness-master"`，审查发现后退化为机器本地派生
- SPEC 说用 Windows Credential Manager，实际只实现了 AES 加密文件
- fat JAR + Dockerfile 两种分发方式，工具链选型是工程决策

**你本来会忽略的问题：**
- API key 出现在异常消息里怎么办（DeepSeekProvider 修复）
- 日志脱敏（maskedDisplay）
- .env 不进 Git（.gitignore 防范）

---

## 8. 如果重做会改什么

**可以选几个点：**
- 选别的语言？（Go 分发更方便？）
- 选别的重点维度？（护栏比记忆更容易出代码效果？）
- Worktree + PR 工作流补上？
- SPEC 写得更精确（定义"最近"的排序方向）？

---

## 9. 对 Superpowers 方法论的批判

**它的假设：**
- 拆分 + TDD + subagent + review = 高质量代码

**这个假设在你的项目里成立吗？**
- 拆分：成立，37 个 task 确实可管理
- TDD：部分成立，测试确实帮了 agent 理解期望，但你个人对 TDD 的真实感受……
- Subagent：成立但有代价（agent 间信息不对齐）
- Review：**没做到**——两阶段 review（spec 合规 + 代码质量）只在第一轮审查才真正执行

**形式大于实质的地方：**
- Worktree 工作流——要求的，但 subagent 直接干 master 更高效
- 「每个 task 开一个 worktree 对应一个 PR」——对单人项目而言太重
- Commit 标注"由哪个 subagent 完成"——有用但容易忘

---

## 10. 对「工程师的真正价值」的回答

题目问的是：当 LLM 能完成大部分编码工作时，一个工程师的价值在哪？

**你的项目给出了一个答案：**
- LLM = CPU，Harness = 操作系统。CPU 很快但需要工程层去管
- 你需要判断：什么可以做（领域设计），什么是正确的（SPEC + 验收标准），什么是安全的（凭据 + 护栏）
- 代码审查证明了——LLM 生成的代码看起来很对，但里面有硬编码密码、空 catch、NPE

**用你的项目数据说话：**
- 8 个致命 bug 是 LLM 写的
- Harness 的 6 个核心机制才是你的工程
- "移除 LLM 后还剩多少"——124 个 test
