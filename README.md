# Coding Agent Harness

一个用于构建 AI 陪伴 Web 应用的领域特化 Coding Agent Harness。

## 项目简介

Coding Agent Harness 是一个 Java CLI 工具，帮助用户通过自然语言描述快速生成 AI 陪伴 Web 应用。用户输入如"一个温柔的知心姐姐聊天伙伴"，harness 自动生成完整的 Spring Boot Web 项目，包含聊天界面、LLM 集成、以及开箱即用的对话记忆能力。

核心特点：
- 自主编码：理解需求 → 生成代码 → 运行测试 → 自我修正 → 交付可运行项目
- 记忆系统：生成的每个陪伴应用都自带对话记忆（滑动窗口 + 摘要 + 语义检索）
- 安全护栏：危险操作拦截 + 人工确认机制
- 凭据安全：API Key 加密存储，绝不硬编码

## 安装与运行

### 前提条件

- JDK 17 或更高版本
- Maven 3.8+（用于构建生成的项目）

### 从源码构建

```bash
git clone <仓库地址>
cd coding-harness
mvn package -DskipTests
java -jar target/coding-harness-1.0.0.jar
```

### 下载预构建 JAR

从 [GitHub Releases](<仓库地址>/releases) 下载最新 `harness.jar`：

```bash
java -jar harness.jar
```

## 使用命令

| 命令 | 说明 |
|------|------|
| `new "<描述>"` | 创建新的 AI 陪伴应用项目 |
| `continue <项目名>` | 继续迭代已有项目 |
| `config` | 查看/设置/清除 DeepSeek API Key |
| `list` | 列出所有已创建的项目 |
| `help` | 显示帮助信息 |
| `exit` | 退出 |

## API Key 安全配置

首次运行 `java -jar harness.jar` 时，harness 会引导你安全录入 DeepSeek API Key：

1. 输入被终端遮蔽（不回显明文）
2. Key 存储于 Windows Credential Manager（Windows）或 AES-256-GCM 加密文件（macOS/Linux）
3. 配置文件 `~/.coding-harness/config.yml` 中**绝不**存储 Key
4. 使用 `config status` 查看状态（仅显示 `***-skxxxx`，不回显完整 Key）
5. 使用 `config clear` 清除 Key

**安全威胁模型：**
- Key 不进 Git（`.gitignore` 阻止 `.env` 和凭据文件）
- 日志自动脱敏 Key 模式（`sk-...` → `***`）
- 所有 LLM 调用强制 HTTPS
- 进程环境变量不暴露 Key（Key 从凭据管理器按需读取，不缓存明文）

## 目录结构

```
coding-harness/
├── SPEC.md                 # 设计文档
├── PLAN.md                 # 实现计划
├── README.md               # 本文件
├── .gitlab-ci.yml          # CI 配置
├── pom.xml                 # Maven POM
└── src/
    ├── main/java/com/codingharness/
    │   ├── CliMain.java           # CLI 入口 + REPL
    │   ├── core/                  # Agent 主循环、上下文构建、停机判断
    │   ├── llm/                   # LLM 抽象层（接口 + DeepSeek + Mock）
    │   ├── tools/                 # 工具系统（8 个工具 + 注册表）
    │   ├── guard/                 # 治理护栏（文件/Shell 守卫 + HITL）
    │   ├── feedback/              # 反馈闭环（测试传感器）
    │   ├── memory/                # 记忆系统 ★（3 种后端 + 窗口 + 摘要 + 检索）
    │   ├── config/                # YAML 配置管理
    │   ├── credentials/           # 凭据安全存储（AES-256-GCM）
    │   └── scaffold/              # 项目脚手架生成器
    └── test/java/com/codingharness/
        ├── demo/                  # 机制演示 ×3
        └── ...                    # 各模块单元测试
```

## 记忆系统（重点维度）

本项目记忆系统是 Main Contribution，实现双层记忆架构：

### Harness 层记忆
跨项目记住用户偏好、历史决策、项目元数据。默认使用 SQLite 持久化。

### 项目记忆运行时（ProjectMemoryRuntime）
生成的每个陪伴应用自动注入记忆能力：
- **滑动窗口**：最近 N 轮完整上下文直接注入 LLM prompt
- **自动摘要**：超出窗口的旧对话自动压缩为结构化摘要
- **语义检索**：当前对话与历史摘要做相似度匹配，召回相关话题
- **重要性衰减**：按时间 + 交互深度计算历史权重

## 安全边界

- Harness 只在项目目录内进行文件操作（超出边界被 FileGuard 拦截）
- Shell 命令执行受白名单约束（默认：`mvn`, `java`, `npm`）
- 危险命令（`rm -rf`, `sudo`, `chmod 777` 等）被 ShellGuard 自动拦截
- 所有护栏判定是确定性 Java 代码，不依赖 LLM 判断

## 已知限制

- 需要 JDK 17+
- `mvn` 命令需要在 PATH 中
- Windows Credential Manager 仅在 Windows 可用（其他平台使用 AES 加密文件 fallback）
- 生成的 Web 应用默认使用内存模式记忆（可升级为 SQLite）

## 技术栈

Java 17 · Maven · OkHttp 4 · Jackson 2 · SnakeYAML · SQLite JDBC · Mustache · SLF4J + Logback · JUnit 5 · Mockito · AssertJ

## 许可证

本项目为 AI4SE 课程期末项目。

## CI/CD

每次 push 自动运行 `mvn test`，CI 配置见 `.gitlab-ci.yml`。
