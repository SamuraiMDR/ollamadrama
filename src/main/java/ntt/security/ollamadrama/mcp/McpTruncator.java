package ntt.security.ollamadrama.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * Lightweight MCP response truncator - converts large JSON responses to concise markdown.
 */
public class McpTruncator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private static final int DEFAULT_MAX_STRING = 400;
    private static final int DEFAULT_MAX_ARRAY = 8;
    private static final int DEFAULT_MAX_TOTAL = 12000;
    private static final int DEFAULT_MAX_DEPTH = 4;
    
    // Fields that are typically noise for LLM understanding
    private static final Set<String> SKIP_FIELDS = Set.of(
        "id", "author_id", "parent_id", "place_id", "uri", "url",
        "created_at", "updated_at", "you_follow"
    );

    /**
     * Truncate with default settings.
     */
    public static String truncate(String json) {
        return truncate(json, DEFAULT_MAX_STRING, DEFAULT_MAX_ARRAY, DEFAULT_MAX_TOTAL);
    }

    /**
     * Truncate with custom limits.
     */
    public static String truncate(String json, int maxString, int maxArray, int maxTotal) {
        if (json == null || json.isBlank()) return "";
        
        try {
            JsonNode root = MAPPER.readTree(json);
            StringBuilder sb = new StringBuilder();
            render(root, sb, 0, maxString, maxArray, DEFAULT_MAX_DEPTH);
            
            String result = sb.toString();
            if (result.length() > maxTotal) {
                return result.substring(0, maxTotal) + "\n\n...[response truncated]";
            }
            return result;
        } catch (Exception e) {
            // Not JSON - return truncated plain text
            return json.length() > maxTotal 
                ? json.substring(0, maxTotal) + "...[truncated]" 
                : json;
        }
    }

    private static void render(JsonNode node, StringBuilder sb, int depth, 
                               int maxString, int maxArray, int maxDepth) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (depth > maxDepth) {
            sb.append("...[nested content omitted]");
            return;
        }

        String indent = "  ".repeat(depth);

        if (node.isObject()) {
            renderObject(node, sb, depth, indent, maxString, maxArray, maxDepth);
        } else if (node.isArray()) {
            renderArray(node, sb, depth, indent, maxString, maxArray, maxDepth);
        } else if (node.isTextual()) {
            String text = node.asText();
            if (text.length() > maxString) {
                sb.append(text.substring(0, maxString)).append("...[truncated]");
            } else {
                sb.append(text);
            }
        } else {
            sb.append(node.asText());
        }
    }

    private static void renderObject(JsonNode node, StringBuilder sb, int depth, String indent,
                                     int maxString, int maxArray, int maxDepth) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            // Skip noise fields and nulls
            if (SKIP_FIELDS.contains(key)) continue;
            if (value == null || value.isNull()) continue;
            if (value.isTextual() && value.asText().isBlank()) continue;
            if ((value.isArray() || value.isObject()) && value.isEmpty()) continue;

            String label = toLabel(key);

            if (value.isObject()) {
                sb.append(indent).append("**").append(label).append(":**\n");
                render(value, sb, depth + 1, maxString, maxArray, maxDepth);
            } else if (value.isArray()) {
                sb.append(indent).append("**").append(label).append("** (")
                  .append(value.size()).append(" items):\n");
                render(value, sb, depth + 1, maxString, maxArray, maxDepth);
            } else {
                sb.append(indent).append("- **").append(label).append(":** ");
                render(value, sb, depth, maxString, maxArray, maxDepth);
                sb.append("\n");
            }
        }
    }

    private static void renderArray(JsonNode node, StringBuilder sb, int depth, String indent,
                                    int maxString, int maxArray, int maxDepth) {
        int total = node.size();
        int shown = 0;

        for (JsonNode element : node) {
            if (shown >= maxArray) {
                sb.append(indent).append("  *...and ").append(total - shown).append(" more*\n");
                break;
            }

            if (element.isObject()) {
                // Try to get a summary line for the object
                String summary = extractSummary(element);
                if (summary != null) {
                    sb.append(indent).append("- ").append(summary).append("\n");
                    // Optionally show nested content for first few items
                    if (shown < 3 && depth < maxDepth - 1) {
                        renderCompact(element, sb, depth + 1, maxString);
                    }
                } else {
                    sb.append(indent).append("- [item ").append(shown + 1).append("]\n");
                    render(element, sb, depth + 1, maxString, maxArray, maxDepth);
                }
            } else {
                sb.append(indent).append("- ");
                render(element, sb, depth, maxString, maxArray, maxDepth);
                sb.append("\n");
            }
            shown++;
        }
    }

    /**
     * Extract a one-line summary from an object (looks for common fields).
     */
    private static String extractSummary(JsonNode obj) {
        // Try author.name + content combo (for comments)
        if (obj.has("author") && obj.has("content")) {
            JsonNode author = obj.get("author");
            String name = author.has("name") ? author.get("name").asText() : "unknown";
            String content = obj.get("content").asText();
            int karma = author.has("karma") ? author.get("karma").asInt() : 0;
            String preview = content.length() > 80 ? content.substring(0, 80) + "..." : content;
            return String.format("**%s** (karma:%d): %s", name, karma, preview);
        }
        
        // Try title
        if (obj.has("title")) {
            return "**" + obj.get("title").asText() + "**";
        }
        
        // Try name
        if (obj.has("name")) {
            String name = obj.get("name").asText();
            if (obj.has("description")) {
                return "**" + name + "**: " + truncateText(obj.get("description").asText(), 60);
            }
            return "**" + name + "**";
        }

        return null;
    }

    /**
     * Render object compactly - just key fields on one line.
     */
    private static void renderCompact(JsonNode obj, StringBuilder sb, int depth, int maxString) {
        String indent = "  ".repeat(depth);
        
        // Show upvotes/downvotes if present
        if (obj.has("upvotes") || obj.has("downvotes")) {
            sb.append(indent).append("  ↑").append(obj.path("upvotes").asInt(0));
            sb.append(" ↓").append(obj.path("downvotes").asInt(0)).append("\n");
        }
    }

    private static String toLabel(String key) {
        // Convert snake_case to Title Case
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : key.toCharArray()) {
            if (c == '_') {
                result.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String truncateText(String text, int max) {
        if (text == null) return "";
        text = text.replace("\n", " ").trim();
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}