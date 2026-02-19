package ntt.security.ollamadrama.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for truncating large MCP tool responses to make them more digestible for LLMs.
 * Converts JSON to markdown, removes nulls, truncates long values, and limits array sizes.
 */
public class McpResponseTruncator {

	private static final Logger LOGGER = LoggerFactory.getLogger(McpResponseTruncator.class);
	
    private static final ObjectMapper mapper = new ObjectMapper();

    // Configuration defaults
    private int maxStringLength = 500;
    private int maxArrayElements = 10;
    private int maxDepth = 5;
    private int maxTotalChars = 15000;

    public McpResponseTruncator() {}

    public McpResponseTruncator maxStringLength(int length) {
        this.maxStringLength = length;
        return this;
    }

    public McpResponseTruncator maxArrayElements(int elements) {
        this.maxArrayElements = elements;
        return this;
    }

    public McpResponseTruncator maxDepth(int depth) {
        this.maxDepth = depth;
        return this;
    }

    public McpResponseTruncator maxTotalChars(int chars) {
        this.maxTotalChars = chars;
        return this;
    }

    /**
     * Main entry point - takes raw response string, returns truncated markdown.
     */
    public String truncate(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }

        try {
            // Try to parse as JSON
            JsonNode root = mapper.readTree(response);
            JsonNode cleaned = cleanNode(root, 0);
            String markdown = toMarkdown(cleaned, 0);
            
            // Final length check
            if (markdown.length() > maxTotalChars) {
                markdown = markdown.substring(0, maxTotalChars) + "\n\n... [truncated - response too long]";
            }
            
            return markdown;
        } catch (Exception e) {
            LOGGER.info("Not valid JSON, treating as plain text");
            return truncateString(response, maxTotalChars);
        }
    }

    /**
     * Recursively clean a JSON node - remove nulls, truncate strings, limit arrays.
     */
    private JsonNode cleanNode(JsonNode node, int depth) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        if (depth > maxDepth) {
            return mapper.createObjectNode().put("_truncated", "max depth exceeded");
        }

        if (node.isObject()) {
            ObjectNode cleaned = mapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode value = field.getValue();

                // Skip null/empty values
                if (value == null || value.isNull()) {
                    continue;
                }
                if (value.isTextual() && value.asText().isBlank()) {
                    continue;
                }
                if (value.isArray() && value.isEmpty()) {
                    continue;
                }
                if (value.isObject() && value.isEmpty()) {
                    continue;
                }

                JsonNode cleanedValue = cleanNode(value, depth + 1);
                if (cleanedValue != null) {
                    cleaned.set(key, cleanedValue);
                }
            }
            
            return cleaned.isEmpty() ? null : cleaned;
        }

        if (node.isArray()) {
            ArrayNode cleaned = mapper.createArrayNode();
            int count = 0;
            int total = node.size();
            
            for (JsonNode element : node) {
                if (count >= maxArrayElements) {
                    ObjectNode truncNote = mapper.createObjectNode();
                    truncNote.put("_note", String.format("... and %d more items", total - count));
                    cleaned.add(truncNote);
                    break;
                }
                
                JsonNode cleanedElement = cleanNode(element, depth + 1);
                if (cleanedElement != null) {
                    cleaned.add(cleanedElement);
                    count++;
                }
            }
            
            return cleaned.isEmpty() ? null : cleaned;
        }

        if (node.isTextual()) {
            String text = node.asText();
            if (text.length() > maxStringLength) {
                return mapper.getNodeFactory().textNode(
                        text.substring(0, maxStringLength) + "... [truncated]"
                );
            }
        }

        return node;
    }

    /**
     * Convert cleaned JSON to markdown format.
     */
    private String toMarkdown(JsonNode node, int indent) {
        if (node == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String prefix = "  ".repeat(indent);

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode value = field.getValue();

                if (value.isObject() || value.isArray()) {
                    sb.append(prefix).append("**").append(formatKey(key)).append(":**\n");
                    sb.append(toMarkdown(value, indent + 1));
                } else {
                    sb.append(prefix).append("- **").append(formatKey(key)).append(":** ");
                    sb.append(formatValue(value)).append("\n");
                }
            }
        } else if (node.isArray()) {
            int index = 0;
            for (JsonNode element : node) {
                if (element.isObject()) {
                    // Check if it's a simple note object
                    if (element.has("_note")) {
                        sb.append(prefix).append("- *").append(element.get("_note").asText()).append("*\n");
                    } else {
                        sb.append(prefix).append("- **[").append(index + 1).append("]**\n");
                        sb.append(toMarkdown(element, indent + 1));
                    }
                } else {
                    sb.append(prefix).append("- ").append(formatValue(element)).append("\n");
                }
                index++;
            }
        } else {
            sb.append(prefix).append(formatValue(node)).append("\n");
        }

        return sb.toString();
    }

    private String formatKey(String key) {
        // Convert snake_case to Title Case
        return Arrays.stream(key.split("_"))
                .map(word -> word.isEmpty() ? "" : 
                        Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(key);
    }

    private String formatValue(JsonNode node) {
        if (node.isTextual()) {
            String text = node.asText();
            // Escape markdown special chars and handle multiline
            if (text.contains("\n")) {
                return "\n  > " + text.replace("\n", "\n  > ");
            }
            return text;
        }
        if (node.isNumber()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? "✓" : "✗";
        }
        return node.toString();
    }

    private String truncateString(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [truncated]";
    }

}