package ntt.security.ollamadrama.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class MCPUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(MCPUtils.class);

	public static ListToolsResult listToolFromMCPEndpoint(String _mcp_url) {
		return listToolFromMCPEndpoint(_mcp_url, "/sse", 30);
	}

	public static ListToolsResult listToolFromMCPEndpoint(String _mcp_url, long _timeout) {
		return listToolFromMCPEndpoint(_mcp_url, "/sse", _timeout);
	}

	public static ListToolsResult listToolFromMCPEndpoint(String _mcp_url, String _mcp_endpoint_path, long _timeout) {

		ListToolsResult tools = null;
		HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(_mcp_url)
				.sseEndpoint(_mcp_endpoint_path)
				.customizeClient(builder -> builder.connectTimeout(Duration.ofSeconds(30)))
				.build();
		McpSyncClient client = McpClient.sync(transport)
				.requestTimeout(Duration.ofSeconds(_timeout))
				.capabilities(ClientCapabilities.builder().roots(true).build())
				.build();

		boolean success = false;
		int retrycounter = 0;
		while (!success && retrycounter<=3) {
			try {
				client.initialize();
				success = true;
			} catch (Exception e) {
				LOGGER.debug("Unable to initialize client: " + e.getMessage());
			}
			retrycounter++;
		}
		if (!client.isInitialized()) {
			LOGGER.debug("Failed to initialize MCP client against " + _mcp_url + " with path " + _mcp_endpoint_path);
		} else {
			tools = client.listTools();
			client.close();
		}

		return tools;
	}

	public static CallToolResult callToolUsingMCPEndpoint(String _mcp_endpoint, String _mcp_endpoint_path, String _toolname, HashMap<String, Object> _arguments, long _timeout) {

		CallToolResult result = null;
		boolean success = false;
		int trycounter = 0;
		while (!success && (trycounter <= 10)) {
			HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(_mcp_endpoint)
					.sseEndpoint(_mcp_endpoint_path)
					.customizeClient(builder -> builder.connectTimeout(Duration.ofSeconds(30)))
					.build();
			McpSyncClient client = McpClient.sync(transport)
					.requestTimeout(Duration.ofSeconds(_timeout))
					.capabilities(ClientCapabilities.builder().roots(true).build())
					.build();
			client.initialize();

			result = client.callTool(
					new CallToolRequest(_toolname, _arguments)
					);

			if (!result.isError()) {
				success = true;
			} else {
				LOGGER.info("Caught error when calling " +_toolname + " trycounter: " + trycounter);
			}
			trycounter++;
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static String prettyPrint(ListToolsResult tools) {
		StringBuilder sb = new StringBuilder();
		for (Tool tool : tools.tools()) {
			sb.append("\n---\n");
			sb.append("Tool: ").append(tool.name()).append("\n");
			sb.append("Description: ").append(
					StringsUtils.cutAndPadStringToN(
							tool.description().split("\\.")[0], 100
							)
					).append("\n");

			JsonSchema inputSchema = tool.inputSchema();

			if (inputSchema == null || !"object".equals(inputSchema.type())) {
				LOGGER.error("Unsupported or missing input schema for tool: " + tool.name());
				SystemUtils.halt();
			}

			Map<String, Object> props = inputSchema.properties();
			Set<String> requiredKeys = inputSchema.required() != null
					? new HashSet<>(inputSchema.required())
							: Collections.emptySet();

			if (props == null) {
				LOGGER.error("Missing 'properties' for tool: " + tool.name());
				SystemUtils.halt();
			}

			sb.append("Inputs:\n");

			List<String> exampleArgs = new ArrayList<>();

			for (String key : props.keySet()) {
				Object value = props.get(key);

				if (!(value instanceof Map)) {
					LOGGER.error("Unexpected schema format for key: " + key);
					SystemUtils.halt();
				}

				Map<String, Object> propMap = (Map<String, Object>) value;
				String type = (String) propMap.get("type");
				String desc = (String) propMap.getOrDefault("description", "No description");
				Object defaultValue = propMap.get("default");

				if (type == null) {
					LOGGER.warn("Missing type for key: " + key + " in tool: " + tool.name() + ", fallback to string?");
				}

				// Append to prompt
				sb.append("  - ").append(key)
				.append(" (").append(type).append(")")
				.append(requiredKeys.contains(key) ? " [required]" : "")
				.append(": ").append(desc).append("\n");

				// Format the example argument
				String exampleVal;
				if (defaultValue != null) {
					// Convert to string literal, wrap in quotes unless boolean/number
					if (defaultValue instanceof Boolean || defaultValue instanceof Number) {
						exampleVal = defaultValue.toString();
					} else {
						exampleVal = "\"" + defaultValue.toString() + "\"";
					}
				} else {
					exampleVal = "\"...\"";
				}

				exampleArgs.add(key + "=" + exampleVal);
			}

			sb.append("Example usage: ")
			.append(tool.name())
			.append("(")
			.append(String.join(", ", exampleArgs))
			.append(")\n");
		}

		return sb.toString();
	}


	public static HashMap<String, Object> parseArguments(String _input) {
		HashMap<String, Object> arguments = new HashMap<>();

		// Extract everything inside the parentheses
		Pattern pattern = Pattern.compile("\\w+\\((.*)\\)");
		Matcher matcher = pattern.matcher(_input);
		if (matcher.find()) {
			String params = matcher.group(1);

			// Split by commas not inside quotes
			String[] pairs = params.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

			for (String pair : pairs) {
				String[] keyValue = pair.trim().split("=", 2);
				if (keyValue.length == 2) {
					String key = keyValue[0].trim();
					String value = keyValue[1].trim();

					// Try to convert the value to the right type
					Object parsedValue;
					if (value.startsWith("\"") && value.endsWith("\"")) {
						parsedValue = value.substring(1, value.length() - 1);
					} else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
						parsedValue = Boolean.parseBoolean(value);
					} else {
						try {
							parsedValue = Integer.parseInt(value);
						} catch (NumberFormatException e) {
							try {
								parsedValue = Double.parseDouble(value);
							} catch (NumberFormatException ex) {
								parsedValue = value; // fallback as raw string
							}
						}
					}

					arguments.put(key, parsedValue);
				}
			}
		}

		return arguments;
	}

	@SuppressWarnings("unchecked")
	public static String prettyPrintOLD(ListToolsResult tools) {

		StringBuffer sb = new StringBuffer();

		sb.append("Available tools:\n");
		for (Tool tool: tools.tools()) {
			sb.append(" - tool name: " + tool.name() + "\n");
			sb.append("   * descripton: " +  StringsUtils.cutAndPadStringToN(tool.description().split("\\.")[0], 100) + "\n");

			JsonSchema input_schema = tool.inputSchema();			
			if ("object".equals(input_schema.type())) {

				// Sanity check for required properties
				if (null == input_schema.type()) {
					LOGGER.error("Mandatory input schema type is not set");
					SystemUtils.halt();
				}
				if (null == input_schema.properties()) {
					LOGGER.error("Mandatory input schema properties is not set");
					SystemUtils.halt();
				}

				// Walk through the input properties
				for (String key : input_schema.properties().keySet()) {
					Object value = input_schema.properties().get(key);

					if (value instanceof Map) {
						Map<String, Object> propMap = (Map<String, Object>) value;
						String keytype = (String) propMap.get("type");

						if (null == keytype) {
							LOGGER.error("Anomaly, missing type for key " + key + " and tool " + tool.name());
							SystemUtils.halt();
						} else if (false ||
								"string".equals(keytype) ||
								"integer".equals(keytype) ||
								"boolean".equals(keytype) ||
								"number".equals(keytype) ||
								false) {
							// known types
						} else {
							LOGGER.error("Unknown type '" + keytype + "' for key " + key + " and tool " + tool.name());
							SystemUtils.halt();
						}

						sb.append("** input key: " + key + " [" + keytype + "]\n");
						for (Map.Entry<String, Object> entry : propMap.entrySet()) {
							sb.append("    " + entry.getKey() + " = " + entry.getValue() + "\n");
						}
					} else {                            
						LOGGER.error("Unexpected value type: " + value.getClass());
					}
				}
			} else {
				LOGGER.error("Not sure how to print input schema of type " + input_schema.type());
				SystemUtils.halt();
			}

			sb.append("   * input keys available: " + input_schema.properties().keySet());
			sb.append("   * input keys required: " + input_schema.required());
		}

		return sb.toString();
	}

	public static String prettyPrint(CallToolResult result) {
		StringBuffer sb = new StringBuffer();
		if (!result.isError()) {
			for (Content content: result.content()) {
				if ("text".equals(content.type())) {
					TextContent tcontent = (TextContent) content;
					sb.append(" * content type     : " + tcontent.type() + "\n");
					if (null != tcontent.priority()) sb.append(" * content priority : " + tcontent.priority() + "\n");
					if (null != tcontent.audience()) sb.append(" * content audience : " + tcontent.audience() + "\n");
					sb.append(" * content text     :\n");
					sb.append("----------------------------\n");
					sb.append(StringsUtils.cutAndPadStringToN(tcontent.text(), 20000) + "\n");
					sb.append("----------------------------\n");
				} else {
					LOGGER.info("Unhandled result type:" + content.type() + "\n");
				}
			}
		} else {
			System.out.println(result.toString());
			SystemUtils.halt();
		}
		return sb.toString();
	}

	public static String parseTool(String toolEntry) {
		if (toolEntry == null || toolEntry.trim().isEmpty()) {
			LOGGER.warn("Received an empty string!");
			return "";
		}

		// Match the method name before the first parenthesis
		String regex = "^(\\w+)\\s*\\(";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(toolEntry.trim());

		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return "";
		}
	}

}
