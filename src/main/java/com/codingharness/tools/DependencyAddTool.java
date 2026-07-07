package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyAddTool implements Tool {
    @Override
    public String name() { return "dependency_add"; }

    @Override
    public String description() { return "Add a dependency to the project's build file"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "groupId", Map.of("type", "string", "description", "Maven groupId or npm package name"),
                "artifactId", Map.of("type", "string", "description", "Maven artifactId (optional for npm)"),
                "version", Map.of("type", "string", "description", "Dependency version")
            ),
            "required", java.util.List.of("groupId", "version")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        String groupId = (String) args.get("groupId");
        String artifactId = (String) args.get("artifactId");
        String version = (String) args.get("version");

        if (groupId == null || groupId.isBlank()) {
            return ToolResult.failure("groupId parameter is required");
        }
        if (version == null || version.isBlank()) {
            return ToolResult.failure("version parameter is required");
        }

        try {
            Path pomFile = ctx.projectRoot().resolve("pom.xml");
            if (!Files.exists(pomFile)) {
                return ToolResult.failure("pom.xml not found in project root");
            }

            String pom = Files.readString(pomFile);

            // Check if dependency already exists
            String depId = artifactId != null ? artifactId : groupId;
            if (pom.contains("<artifactId>" + depId + "</artifactId>")) {
                return ToolResult.success("dependency already exists: " + depId);
            }

            // Find </dependencies> or insert before </project>
            String dependencyXml;
            if (artifactId != null && !artifactId.isBlank()) {
                dependencyXml = "        <dependency>\n"
                    + "            <groupId>" + escapeXml(groupId) + "</groupId>\n"
                    + "            <artifactId>" + escapeXml(artifactId) + "</artifactId>\n"
                    + "            <version>" + escapeXml(version) + "</version>\n"
                    + "        </dependency>\n";
            } else {
                dependencyXml = "        <dependency>\n"
                    + "            <groupId>" + escapeXml(groupId) + "</groupId>\n"
                    + "            <version>" + escapeXml(version) + "</version>\n"
                    + "        </dependency>\n";
            }

            String updated;
            if (pom.contains("</dependencies>")) {
                updated = pom.replace("</dependencies>", dependencyXml + "    </dependencies>");
            } else {
                // Insert before </project>
                String depsBlock = "    <dependencies>\n" + dependencyXml + "    </dependencies>\n\n";
                updated = pom.replace("</project>", depsBlock + "</project>");
            }

            Files.writeString(pomFile, updated);
            return ToolResult.success("added dependency: " + depId + ":" + version);
        } catch (Exception e) {
            return ToolResult.failure("error adding dependency: " + e.getMessage());
        }
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
