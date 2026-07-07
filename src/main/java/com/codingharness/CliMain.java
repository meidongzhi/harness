package com.codingharness;

import com.codingharness.config.ConfigManager;
import com.codingharness.config.HarnessConfig;
import com.codingharness.core.*;
import com.codingharness.credentials.AesCredentialStore;
import com.codingharness.credentials.CredentialStore;
import com.codingharness.feedback.FeedbackSensor;
import com.codingharness.feedback.FeedbackResult;
import com.codingharness.guard.*;
import com.codingharness.llm.*;
import com.codingharness.memory.*;
import com.codingharness.tools.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * REPL-based CLI for the Coding Agent Harness.
 *
 * <p>Commands:</p>
 * <ul>
 *   <li>{@code new "description"} -- create a new project and run the agent loop</li>
 *   <li>{@code continue <name>}   -- resume an existing project</li>
 *   <li>{@code config}            -- show configuration status</li>
 *   <li>{@code config set-key <key>} -- store an API key</li>
 *   <li>{@code config clear-key}   -- clear stored API key</li>
 *   <li>{@code list}              -- list all known projects</li>
 *   <li>{@code help}              -- show available commands</li>
 *   <li>{@code exit}              -- quit the harness</li>
 * </ul>
 */
public class CliMain {

    private static final Path CONFIG_DIR = Paths.get(
        System.getProperty("user.home"), ".coding-harness");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.yml");
    private static final Path PROJECTS_DIR = CONFIG_DIR.resolve("projects");

    private final ConfigManager configManager;
    private final HarnessMemory memory;
    private final CredentialStore credentialStore;
    private final Scanner scanner;
    private HarnessConfig config;

    public CliMain() {
        this.configManager = new ConfigManager(CONFIG_FILE);
        this.memory = new HarnessMemory(new InMemoryStore());
        // WARNING: This machine-local passphrase is NOT cryptographically secure.
        // Anyone with filesystem access and knowledge of the system username/home
        // can derive the same key. For production, use OS keychain (Windows Credential
        // Manager / macOS Keychain / Linux Secret Service) or a user-provided passphrase.
        String masterPw = System.getProperty("user.name", "harness") + "-" +
            System.getProperty("user.home", "").hashCode();
        this.credentialStore = new AesCredentialStore(CONFIG_DIR, masterPw);
        this.scanner = new Scanner(System.in);
        this.config = configManager.load();
    }

    public static void main(String[] args) {
        CliMain cli = new CliMain();
        cli.showWelcome();
        cli.runRepl();
    }

    private void showWelcome() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("   Coding Agent Harness v1.0.0");
        System.out.println("   AI-powered web application generator");
        System.out.println("==============================================");
        System.out.println("   Config: " + CONFIG_FILE);
        System.out.println("   Provider: " + config.provider());
        System.out.println("   Model: " + config.model());
        System.out.println("   Max Turns: " + config.maxTurns());
        System.out.println("   Memory Backend: " + config.memoryBackend());
        System.out.println("==============================================");
        System.out.println("Type 'help' for available commands.");
        System.out.println();
    }

    private void runRepl() {
        while (true) {
            System.out.print("harness> ");
            String line;
            try {
                if (!scanner.hasNextLine()) break;
                line = scanner.nextLine().trim();
            } catch (Exception e) {
                break;
            }
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 2);
            String command = parts[0].toLowerCase();
            String args = parts.length > 1 ? parts[1] : "";

            try {
                switch (command) {
                    case "new" -> handleNew(args);
                    case "continue" -> handleContinue(args);
                    case "config" -> handleConfig(args);
                    case "list" -> handleList();
                    case "help" -> handleHelp();
                    case "exit", "quit" -> { System.out.println("Goodbye."); return; }
                    default -> System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void handleNew(String args) {
        // Parse description (must be quoted)
        String description = args.replaceAll("^\"|\"$", "").trim();
        if (description.isEmpty()) {
            System.out.println("Usage: new \"<project description>\"");
            return;
        }

        String projectName = sanitizeProjectName(description);
        Path projectDir = PROJECTS_DIR.resolve(projectName);

        System.out.println("Creating project '" + projectName + "'...");
        System.out.println("  Description: " + description);
        System.out.println("  Directory: " + projectDir);
        System.out.println();

        try {
            Files.createDirectories(projectDir);

            // Remember the project
            memory.rememberProject(projectName, projectName, description);

            // Build context
            ProjectContext ctx = ProjectContext.create(projectName, projectDir, memory, config);

            // Build the agent loop with dry-run MockLlmProvider
            AgentLoopImpl loop = buildAgentLoop();

            System.out.println("Running agent loop (dry-run mode with MockLlmProvider)...");
            System.out.println();

            var result = loop.run(ctx);

            switch (result) {
                case SUCCESS -> System.out.println("Project generation SUCCEEDED.");
                case MAX_TURNS -> System.out.println("Project generation reached MAX_TURNS limit.");
                case NEEDS_HUMAN -> System.out.println("Project generation NEEDS HUMAN intervention.");
                case FAILED -> System.out.println("Project generation FAILED.");
            }

            System.out.println("Project directory: " + projectDir);
        } catch (Exception e) {
            System.out.println("Failed to create project: " + e.getMessage());
        }
    }

    private void handleContinue(String args) {
        String projectName = args.trim();
        if (projectName.isEmpty()) {
            System.out.println("Usage: continue <project-name>");
            return;
        }

        Path projectDir = PROJECTS_DIR.resolve(projectName);
        if (!Files.exists(projectDir)) {
            System.out.println("Project not found: " + projectName);
            return;
        }

        System.out.println("Continuing project '" + projectName + "'...");
        System.out.println("  Directory: " + projectDir);

        ProjectContext ctx = ProjectContext.create(projectName, projectDir, memory, config);
        AgentLoopImpl loop = buildAgentLoop();
        var result = loop.run(ctx);
        System.out.println("Result: " + result);
    }

    private void handleConfig(String args) {
        args = args.trim();
        if (args.isEmpty()) {
            // Show config status
            System.out.println("=== Configuration ===");
            System.out.println("  Provider:  " + config.provider());
            System.out.println("  Model:     " + config.model());
            System.out.println("  Base URL:  " + config.baseUrl());
            System.out.println("  Max Turns: " + config.maxTurns());
            System.out.println("  Auto Approve Level: " + config.autoApproveLevel());
            System.out.println("  Memory Backend: " + config.memoryBackend());
            System.out.println("  Memory DB Path: " + config.memoryDbPath());
            System.out.println("  Shell Whitelist: " + config.shellWhitelist());
            System.out.println("  API Key: " + credentialStore.maskedDisplay("api_key"));
            return;
        }

        if (args.startsWith("set-key ")) {
            String key = args.substring("set-key ".length()).trim();
            credentialStore.store("api_key", key);
            System.out.println("API key stored.");
        } else if (args.equals("clear-key")) {
            credentialStore.delete("api_key");
            System.out.println("API key cleared.");
        } else {
            System.out.println("Unknown config sub-command: " + args);
            System.out.println("  config                -- show status");
            System.out.println("  config set-key <key>  -- store API key");
            System.out.println("  config clear-key      -- clear API key");
        }
    }

    private void handleList() {
        List<String> projects = memory.listProjects();
        if (projects.isEmpty()) {
            System.out.println("No projects found.");
        } else {
            System.out.println("Projects:");
            for (String p : projects) {
                System.out.println("  - " + p);
                Map<String, String> prefs = memory.getProjectPreferences(p);
                if (!prefs.isEmpty()) {
                    System.out.println("    description: " + prefs.getOrDefault("description", "N/A"));
                }
            }
        }
    }

    private void handleHelp() {
        System.out.println("Available commands:");
        System.out.println("  new \"<description>\"   Create a new project and run agent loop");
        System.out.println("  continue <name>       Resume an existing project");
        System.out.println("  config                Show configuration");
        System.out.println("  config set-key <key>  Store API key");
        System.out.println("  config clear-key      Clear API key");
        System.out.println("  list                  List all projects");
        System.out.println("  help                  Show this help");
        System.out.println("  exit, quit            Exit the harness");
        System.out.println();
        System.out.println("Note: Currently running in dry-run mode with MockLlmProvider.");
        System.out.println("Set an API key via 'config set-key <key>' to use a real LLM.");
    }

    private AgentLoopImpl buildAgentLoop() {
        // Determine LLM provider: real DeepSeek if API key exists, otherwise mock
        LlmProvider llmProvider;
        FeedbackSensor feedbackSensor;

        Optional<String> apiKeyOpt = credentialStore.retrieve("api_key");
        if (apiKeyOpt.isPresent()) {
            String apiKey = apiKeyOpt.get();
            String baseUrl = config.baseUrl();
            System.out.println("Using DeepSeek API (real mode)");
            llmProvider = new DeepSeekProvider(apiKey, baseUrl);
            feedbackSensor = new com.codingharness.feedback.TestFeedbackSensor();
        } else {
            System.out.println("No API key configured — using Mock mode (dry-run)");
            System.out.println("Set API key: config set-key <your-deepseek-key>");
            LlmResponse mockResponse = new LlmResponse(
                "I'll create the project structure and source files.",
                List.of(
                    new LlmResponse.ToolCall("id1", "file_write",
                        Map.of("path", "README.md", "content", "# Generated Project\n\nThis app was generated by Coding Agent Harness."))
                ),
                "tool_calls",
                new LlmResponse.TokenUsage(50, 25, 75)
            );
            llmProvider = new MockLlmProvider(List.of(mockResponse));
            feedbackSensor = ctx -> FeedbackResult.allGood();
        }

        // Build tool registry
        ToolRegistry tools = new ToolRegistry();
        tools.register(new FileWriteTool());
        tools.register(new FileReadTool());
        tools.register(new FileDeleteTool());
        tools.register(new TestRunTool());
        tools.register(new ShellExecTool());
        tools.register(new ScaffoldNewTool());
        tools.register(new DependencyAddTool());
        tools.register(new ProjectSummaryTool());
        tools.register(new MemorySaveTool());
        tools.register(new MemorySearchTool());

        // Build guard chain
        GuardChain guardChain = new GuardChain(List.of(new FileGuard(), new ShellGuard()));

        return new AgentLoopImpl(llmProvider, tools, guardChain, feedbackSensor,
            config.maxTurns(), 3);
    }

    private String sanitizeProjectName(String description) {
        // Generate a simple name from the description
        String name = description.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        if (name.isEmpty()) {
            name = "project-" + System.currentTimeMillis();
        }
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        return name;
    }
}
