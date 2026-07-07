package com.codingharness.guard;

import com.codingharness.core.Action;
import com.codingharness.core.ProjectContext;

import java.util.List;

public class ShellGuard implements Guard {

    private static final List<String> DANGEROUS_PATTERNS = List.of(
        "rm -rf",
        "sudo",
        "chmod 777",
        "> /dev/",
        "mkfs",
        "dd if=",
        ":(){",
        "del /f",
        "del /s",
        "format",
        "reg delete",
        "diskpart",
        "rmdir /s"
    );

    @Override
    public GuardResult check(Action action, ProjectContext ctx) {
        if (!"shell_exec".equals(action.type())) {
            return GuardResult.allow();
        }

        Object cmdObj = action.parameters().get("command");
        if (cmdObj == null) {
            return GuardResult.block("No command specified");
        }

        String command = cmdObj.toString().trim();

        // Check for shell metacharacters that could allow command injection
        if (command.matches(".*[;&|`$].*")) {
            return GuardResult.requireApproval("Shell metacharacters detected in command", "WARNING");
        }

        for (String pattern : DANGEROUS_PATTERNS) {
            if (command.contains(pattern)) {
                return GuardResult.block("Dangerous shell command: " + pattern);
            }
        }

        List<String> whitelist = ctx.config().shellWhitelist();
        if (whitelist != null) {
            for (String allowed : whitelist) {
                if (command.startsWith(allowed)) {
                    return GuardResult.allow();
                }
            }
        }

        return GuardResult.requireApproval("Unknown command: " + command, "WARNING");
    }
}
