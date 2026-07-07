package com.codingharness.guard;

import com.codingharness.core.Action;
import com.codingharness.core.ProjectContext;

import java.nio.file.Path;
import java.util.List;

public class FileGuard implements Guard {

    private static final List<String> SENSITIVE_PATTERNS = List.of(
        ".env",
        ".git",
        "credentials",
        "/.git-credentials",
        "id_rsa",
        "*.pem",
        "*.key",
        "*.pfx",
        "*.keystore",
        "*.jks"
    );

    @Override
    public GuardResult check(Action action, ProjectContext ctx) {
        if (!"file_write".equals(action.type()) && !"file_delete".equals(action.type())) {
            return GuardResult.allow();
        }

        Object pathObj = action.parameters().get("path");
        if (pathObj == null) {
            return GuardResult.block("No path specified");
        }

        String pathStr = pathObj.toString();
        Path resolved = ctx.projectRoot().resolve(pathStr).normalize().toAbsolutePath();
        Path projectRoot = ctx.projectRoot().normalize().toAbsolutePath();

        if (!resolved.startsWith(projectRoot)) {
            return GuardResult.block("Path outside project boundary");
        }

        Path fileNamePath = resolved.getFileName();
        if (fileNamePath == null) {
            return GuardResult.requireApproval("Cannot determine filename for path: " + resolved, "WARNING");
        }
        String fileName = fileNamePath.toString();
        if (isSensitive(fileName, pathStr)) {
            return GuardResult.block("Sensitive file");
        }

        return GuardResult.allow();
    }

    private boolean isSensitive(String fileName, String fullPath) {
        String lowerName = fileName.toLowerCase();
        String lowerPath = fullPath.toLowerCase();
        for (String pattern : SENSITIVE_PATTERNS) {
            if (pattern.startsWith("/")) {
                if (lowerPath.contains(pattern.toLowerCase())) {
                    return true;
                }
            } else if (pattern.startsWith("*")) {
                String suffix = pattern.substring(1);
                if (lowerName.endsWith(suffix)) {
                    return true;
                }
            } else {
                if (lowerName.equals(pattern.toLowerCase()) || lowerName.contains(pattern.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
}
