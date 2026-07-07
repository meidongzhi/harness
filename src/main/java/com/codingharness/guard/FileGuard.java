package com.codingharness.guard;

import com.codingharness.core.Action;
import com.codingharness.core.ProjectContext;

import java.nio.file.Path;

public class FileGuard implements Guard {

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

        String fileName = resolved.getFileName().toString();
        if (".env".equals(fileName) || ".git".equals(fileName) || fileName.toLowerCase().contains("credentials")) {
            return GuardResult.block("Sensitive file");
        }

        return GuardResult.allow();
    }
}
