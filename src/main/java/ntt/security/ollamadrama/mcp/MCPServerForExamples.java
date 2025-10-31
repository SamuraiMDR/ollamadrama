package ntt.security.ollamadrama.mcp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import ntt.security.ollamadrama.utils.DateUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPServerForExamples {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MCPServerForExamples.class);
	
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final boolean DEBUG = false;

	// For OLD SSE protocol - track multiple sessions
	private static final Map<String, OutputStream> sseOutputStreams = new ConcurrentHashMap<>();
	private static final Map<String, BlockingQueue<String>> messageQueues = new ConcurrentHashMap<>();

	public static void handleSSE(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		String path = exchange.getRequestURI().getPath();

		debug("=== MCP Request Received ===");
		debug("Request Method: " + method);
		debug("Request Path: " + path);
		String acceptHeader = exchange.getRequestHeaders().getFirst("Accept");
		debug("Accept header: " + acceptHeader);

		try {
			if ("GET".equals(method) && "/sse".equals(path)) {
				handleGetRequest(exchange);
			} else if ("POST".equals(method) && (path.equals("/sse") || path.startsWith("/messages"))) {
				handlePostRequest(exchange, acceptHeader);
			} else {
				debug("ERROR: Unsupported method/path combination");
				exchange.sendResponseHeaders(404, 0);
				exchange.close();
			}
		} catch (Exception e) {
			debug("ERROR: Exception caught: " + e.getMessage());
			e.printStackTrace();
		}

		debug("=== MCP Request Complete ===\n");
	}

	private static void handleGetRequest(HttpExchange exchange) throws IOException {
		debug("  - Handling GET (SSE connection for OLD protocol)");

		// Generate a session ID
		String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
		debug("  - Generated session ID: " + sessionId);

		// Set SSE headers
		exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		exchange.getResponseHeaders().set("Connection", "keep-alive");
		exchange.getResponseHeaders().set("x-accel-buffering", "no");

		exchange.sendResponseHeaders(200, 0);
		final OutputStream os = exchange.getResponseBody();

		// Create message queue for this session
		BlockingQueue<String> sessionQueue = new LinkedBlockingQueue<>();
		messageQueues.put(sessionId, sessionQueue);
		sseOutputStreams.put(sessionId, os);

		debug("  - Session registered: " + sessionId);

		// Send endpoint event immediately
		try {
			String messageEndpoint = "/messages/?session_id=" + sessionId;
			String endpointEvent = "event: endpoint\ndata: " + messageEndpoint + "\n\n";
			debug("  - Sending endpoint event bytes: " + endpointEvent.replace("\n", "\\n"));
			os.write(endpointEvent.getBytes(StandardCharsets.UTF_8));
			os.flush();
			debug("  - Endpoint event sent: " + messageEndpoint);
			debug("  - Flushed output stream");
		} catch (IOException e) {
			debug("ERROR sending endpoint event: " + e.getMessage());
			cleanupSession(sessionId);
			try { os.close(); } catch (IOException ignored) {}
			return;
		}

		// Handle SSE stream in a separate thread to not block the server
		Thread sseThread = new Thread(() -> {
			try {
				debug("  - SSE stream thread started for session: " + sessionId);
				// Keep connection alive and send messages
				while (sseOutputStreams.containsKey(sessionId)) {
					try {
						String message = sessionQueue.poll(30, TimeUnit.SECONDS);
						if (message == null) {
							os.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
							os.flush();
							debug("  - Sent keepalive for session: " + sessionId);
							continue;
						}

						debug("  - Sending queued message on SSE stream for session: " + sessionId);
						os.write(message.getBytes(StandardCharsets.UTF_8));
						os.flush();
					} catch (InterruptedException e) {
						debug("  - SSE connection interrupted for session: " + sessionId);
						break;
					}
				}
			} catch (IOException e) {
				debug("  - Client closed SSE connection for session: " + sessionId + " - " + e.getMessage());
			} finally {
				cleanupSession(sessionId);
				try {
					os.close();
				} catch (IOException ignored) {}
				debug("  - SSE stream thread ended for session: " + sessionId);
			}
		});
		sseThread.setDaemon(true);
		sseThread.setName("SSE-" + sessionId);
		sseThread.start();
		debug("  - GET handler returning (SSE stream continues in background thread)");
	}

	private static void cleanupSession(String sessionId) {
		sseOutputStreams.remove(sessionId);
		messageQueues.remove(sessionId);
		debug("  - Cleaned up session: " + sessionId);
	}

	private static void handlePostRequest(HttpExchange exchange, String acceptHeader) throws IOException {
		byte[] requestBytes = exchange.getRequestBody().readAllBytes();

		if (requestBytes.length == 0) {
			debug("ERROR: No request body");
			exchange.sendResponseHeaders(400, -1);
			exchange.close();
			return;
		}

		String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
		debug("Request Body: " + requestBody);

		// Extract session ID from query parameters
		String query = exchange.getRequestURI().getQuery();
		String sessionId = null;
		if (query != null && query.contains("session_id=")) {
			sessionId = query.substring(query.indexOf("session_id=") + 11);
			if (sessionId.contains("&")) {
				sessionId = sessionId.substring(0, sessionId.indexOf("&"));
			}
			debug("  - Extracted session ID: " + sessionId);
		}

		// Check if we have an active SSE connection for this session (OLD protocol)
		boolean hasSSEConnection = (sessionId != null && sseOutputStreams.containsKey(sessionId));
		debug("Has SSE connection for session " + sessionId + ": " + hasSSEConnection);

		if (hasSSEConnection) {
			debug("  - Using OLD SSE protocol (POST with SSE response stream)");
			handleOldProtocolPost(exchange, requestBody, sessionId);
		} else {
			debug("  - Using NEW Streamable HTTP protocol (POST with direct response)");
			handleNewProtocolPost(exchange, requestBody, acceptHeader);
		}
	}

	private static void handleOldProtocolPost(HttpExchange exchange, String requestBody, String sessionId) throws IOException {
		debug("  - In handleOldProtocolPost for session: " + sessionId);

		try {
			// OLD protocol: respond 202 Accepted with "Accepted" body
			byte[] responseBody = "Accepted".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "text/plain");
			debug("  - Sending 202 Accepted response");
			exchange.sendResponseHeaders(202, responseBody.length);
			OutputStream os = exchange.getResponseBody();
			os.write(responseBody);
			os.flush();
			os.close();
			debug("  - 202 response sent and closed");
		} catch (Exception e) {
			debug("ERROR sending 202 response: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}

		try {
			ObjectNode request = (ObjectNode) mapper.readTree(requestBody);
			String rpcMethod = request.has("method") ? request.get("method").asText() : null;
			Object id = extractId(request);

			debug("Parsed Method: " + rpcMethod);
			debug("Parsed ID: " + id);

			if ("initialize".equals(rpcMethod)) {
				debug(">>> Handling INITIALIZE");
				queueResponse(sessionId, id, createInitializeResponse());
			} else if ("tools/list".equals(rpcMethod)) {
				debug(">>> Handling TOOLS/LIST");
				queueResponse(sessionId, id, createToolsListResponse());
			} else if ("tools/call".equals(rpcMethod)) {
				debug(">>> Handling TOOLS/CALL");
				queueResponse(sessionId, id, handleToolCallInternal(request));
			} else if (rpcMethod != null && rpcMethod.startsWith("notifications/")) {
				debug(">>> Ignoring notification: " + rpcMethod);
			} else {
				debug(">>> Unknown method: " + rpcMethod);
				queueError(sessionId, id, "Unknown method: " + rpcMethod);
			}
		} catch (Exception e) {
			debug("ERROR processing request: " + e.getMessage());
			e.printStackTrace();
			try {
				queueError(sessionId, null, "Invalid request: " + e.getMessage());
			} catch (Exception ex) {
				debug("ERROR queueing error: " + ex.getMessage());
			}
		}
	}

	private static void handleNewProtocolPost(HttpExchange exchange, String requestBody, String acceptHeader) throws IOException {
		boolean wantsSSE = acceptHeader != null && acceptHeader.contains("text/event-stream");

		try {
			JsonNode requestNode = mapper.readTree(requestBody);

			if (requestNode.isArray()) {
				handleBatchRequest(exchange, (ArrayNode) requestNode);
			} else {
				handleSingleRequest(exchange, (ObjectNode) requestNode, wantsSSE);
			}
		} catch (Exception e) {
			debug("ERROR: " + e.getMessage());
			e.printStackTrace();
			exchange.sendResponseHeaders(400, -1);
			exchange.close();
		}
	}

	private static void handleSingleRequest(HttpExchange exchange, ObjectNode request, boolean wantsSSE) throws IOException {
		String rpcMethod = request.has("method") ? request.get("method").asText() : null;
		Object id = extractId(request);

		debug("Parsed Method: " + rpcMethod);
		debug("Parsed ID: " + id);
		debug("Wants SSE: " + wantsSSE);

		ObjectNode response = null;

		if ("initialize".equals(rpcMethod)) {
			debug(">>> Handling INITIALIZE");
			response = createSuccessResponse(id, createInitializeResponse());
		} else if ("tools/list".equals(rpcMethod)) {
			debug(">>> Handling TOOLS/LIST");
			response = createSuccessResponse(id, createToolsListResponse());
		} else if ("tools/call".equals(rpcMethod)) {
			debug(">>> Handling TOOLS/CALL");
			response = createSuccessResponse(id, handleToolCallInternal(request));
		} else {
			debug(">>> Unknown method: " + rpcMethod);
			response = createErrorResponse(id, "Unknown method: " + rpcMethod);
		}

		if (wantsSSE) {
			exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
			exchange.getResponseHeaders().set("Cache-Control", "no-cache");
			exchange.sendResponseHeaders(200, 0);

			OutputStream os = exchange.getResponseBody();
			String json = mapper.writeValueAsString(response);
			String sseMessage = "data: " + json + "\n\n";
			os.write(sseMessage.getBytes(StandardCharsets.UTF_8));
			os.flush();
			os.close();
		} else {
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, 0);

			OutputStream os = exchange.getResponseBody();
			String json = mapper.writeValueAsString(response);
			os.write(json.getBytes(StandardCharsets.UTF_8));
			os.close();
		}

		debug("  - Response sent");
	}

	private static void handleBatchRequest(HttpExchange exchange, ArrayNode requests) throws IOException {
		ArrayNode responses = mapper.createArrayNode();

		for (JsonNode requestNode : requests) {
			if (requestNode.isObject()) {
				ObjectNode request = (ObjectNode) requestNode;
				String rpcMethod = request.has("method") ? request.get("method").asText() : null;
				Object id = extractId(request);

				ObjectNode response = null;

				if ("initialize".equals(rpcMethod)) {
					response = createSuccessResponse(id, createInitializeResponse());
				} else if ("tools/list".equals(rpcMethod)) {
					response = createSuccessResponse(id, createToolsListResponse());
				} else if ("tools/call".equals(rpcMethod)) {
					response = createSuccessResponse(id, handleToolCallInternal(request));
				} else {
					response = createErrorResponse(id, "Unknown method: " + rpcMethod);
				}

				responses.add(response);
			}
		}

		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, 0);

		OutputStream os = exchange.getResponseBody();
		String json = mapper.writeValueAsString(responses);
		os.write(json.getBytes(StandardCharsets.UTF_8));
		os.close();
	}

	private static Object extractId(ObjectNode request) {
		if (request.has("id")) {
			JsonNode idNode = request.get("id");
			debug("ID node type: " + idNode.getNodeType() + ", value: " + idNode);

			if (idNode.isIntegralNumber()) {
				return idNode.asLong();
			} else if (idNode.isTextual()) {
				return idNode.asText();
			} else if (idNode.isNull()) {
				return null;
			}
		}
		return null;
	}

	private static ObjectNode createInitializeResponse() {
		ObjectNode result = mapper.createObjectNode();

		ObjectNode serverInfo = mapper.createObjectNode();
		serverInfo.put("name", "Samurai MCP Server");
		serverInfo.put("version", "1.0.0");

		ObjectNode capabilities = mapper.createObjectNode();
		ObjectNode toolsCap = mapper.createObjectNode();
		capabilities.set("tools", toolsCap);

		result.set("serverInfo", serverInfo);
		result.set("capabilities", capabilities);
		result.put("protocolVersion", "2024-11-05");

		return result;
	}

	private static ObjectNode createToolsListResponse() {
		ArrayNode toolsList = mapper.createArrayNode();

		// get_the_meaning_of_life
		ObjectNode tool1 = mapper.createObjectNode();
		tool1.put("name", "get_the_meaning_of_life");
		tool1.put("description", "Tells you the meaning of life.");
		ObjectNode inputSchema1 = mapper.createObjectNode();
		inputSchema1.put("type", "object"); 
		inputSchema1.set("properties", mapper.createObjectNode());
		tool1.set("inputSchema", inputSchema1);
		toolsList.add(tool1);
		
		// get_current_time_in_UTC
		ObjectNode tool2 = mapper.createObjectNode();
		tool2.put("name", "get_current_time_in_UTC");
		tool2.put("description", "Gets the current time in UTC in format yyyy-MM-dd HH:mm:ss.");
		ObjectNode inputSchema2 = mapper.createObjectNode();
		inputSchema2.put("type", "object");
		inputSchema2.set("properties", mapper.createObjectNode());
		tool2.set("inputSchema", inputSchema2);
		toolsList.add(tool2);
		
		// use_hidden_algorithm_with_two_numbers
		ObjectNode tool3 = mapper.createObjectNode();
		tool3.put("name", "use_hidden_algorithm_with_two_numbers");
		tool3.put("description", "Uses a hidden algorithm to calculate a new number from the given two arguments.");
		ObjectNode nameParam3 = mapper.createObjectNode();
		nameParam3.put("type", "integer");
		nameParam3.put("description", "First argument");
		ObjectNode nameParam4 = mapper.createObjectNode();
		nameParam4.put("type", "integer");
		nameParam4.put("description", "Second argument");
		ObjectNode properties3 = mapper.createObjectNode();
		properties3.set("num1", nameParam3);
		properties3.set("num2", nameParam4);
		ObjectNode inputSchema3 = mapper.createObjectNode();
		inputSchema3.put("type", "object");
		inputSchema3.set("properties", properties3);
		ArrayNode required3 = mapper.createArrayNode();
		required3.add("num1");
		required3.add("num2");
		inputSchema3.set("required", required3);
		tool3.set("inputSchema", inputSchema3);
		toolsList.add(tool3);

		// reply
		ObjectNode result = mapper.createObjectNode();
		result.set("tools", toolsList);

		return result;
	}

	private static ObjectNode handleToolCallInternal(ObjectNode request) {
		ObjectNode params = request.has("params") ? (ObjectNode) request.get("params") : null;

		if (params == null || !params.has("name") || !params.has("arguments")) {
			return createToolErrorResponse("Tool call must contain params with 'name' and 'arguments' fields");
		}

		String toolName = params.get("name").asText();
		ObjectNode args = (ObjectNode) params.get("arguments");
		ArrayNode content = null;
		
		
		// get_the_meaning_of_life
		if ("get_the_meaning_of_life".equals(toolName)) {
			content = mapper.createArrayNode();
			ObjectNode textContent = mapper.createObjectNode();
			textContent.put("type", "text");
			textContent.put("text", "42");
			content.add(textContent);
		} else if ("get_current_time_in_UTC".equals(toolName)) {
			String timestamp= DateUtils.nowTimeStamp();
			content = mapper.createArrayNode();
			ObjectNode textContent = mapper.createObjectNode();
			textContent.put("type", "text");
			textContent.put("text", timestamp);
			content.add(textContent);
		} else if ("use_hidden_algorithm_with_two_numbers".equals(toolName)) {
		    if (args == null || !args.has("num1") || !args.has("num2")) {
		        return createToolErrorResponse("use_hidden_algorithm_with_two_numbers tool requires 'num1' and 'num2' arguments");
		    }
		    String num1 = args.get("num1").asText();
		    String num2 = args.get("num2").asText();
		    Integer num1_int = null;
		    Integer num2_int = null;
		    try {
		        num1_int = Integer.parseInt(num1);
		        num2_int = Integer.parseInt(num2);
		    } catch (Exception e) {
		        return createToolErrorResponse("use_hidden_algorithm_with_two_numbers tool requires 'num1' and 'num2' arguments (type integer)");
		    }
		    if (num1_int == null || num2_int == null) {  // Fixed: was checking num1/num2 strings and had typo __num2__
		        return createToolErrorResponse("use_hidden_algorithm_with_two_numbers tool requires 'num1' and 'num2' arguments (valid integers)");
		    }
		    debug("  - Calling use_hidden_algorithm_with_two_numbers with num1: " + num1 + " and num2 " + num2);
		    
		    long alg_long = (long) num1_int + (long) num2_int + 1L;
		    if (alg_long > Integer.MAX_VALUE || alg_long < Integer.MIN_VALUE) {
		        return createToolErrorResponse("Integer overflow: result " + alg_long + " exceeds integer range");
		    }
		    int alg1_int = (int) alg_long;
		    
		    content = mapper.createArrayNode();
		    ObjectNode textContent = mapper.createObjectNode();
		    textContent.put("type", "text");  // Changed from "int" to "text"
		    textContent.put("text", String.valueOf(alg1_int));  // Convert int to String
		    content.add(textContent);
		}
		
		if (null == content) {
			return createToolErrorResponse("Unknown tool: " + toolName);
		} else {
			ObjectNode result = mapper.createObjectNode();
			result.set("content", content);
			result.put("isError", false);
			return result;
		}
	}

	private static ObjectNode createToolErrorResponse(String errorMessage) {
		ArrayNode content = mapper.createArrayNode();
		ObjectNode textContent = mapper.createObjectNode();
		textContent.put("type", "text");
		textContent.put("text", errorMessage);
		content.add(textContent);

		ObjectNode result = mapper.createObjectNode();
		result.set("content", content);
		result.put("isError", true);

		return result;
	}

	private static void queueResponse(String sessionId, Object id, ObjectNode data) throws IOException {
		BlockingQueue<String> queue = messageQueues.get(sessionId);
		if (queue == null) {
			debug("ERROR: No message queue for session: " + sessionId);
			return;
		}

		ObjectNode message = createSuccessResponse(id, data);
		String jsonResponse = mapper.writeValueAsString(message);
		debug("  - Queueing response for session: " + sessionId);
		String sseMessage = "event: message\ndata: " + jsonResponse + "\n\n";
		queue.offer(sseMessage);
	}

	private static void queueError(String sessionId, Object id, String error) throws IOException {
		BlockingQueue<String> queue = messageQueues.get(sessionId);
		if (queue == null) {
			debug("ERROR: No message queue for session: " + sessionId);
			return;
		}

		ObjectNode message = createErrorResponse(id, error);
		String jsonError = mapper.writeValueAsString(message);
		debug("  - Queueing error for session: " + sessionId);
		String sseMessage = "event: message\ndata: " + jsonError + "\n\n";
		queue.offer(sseMessage);
	}

	private static ObjectNode createSuccessResponse(Object id, ObjectNode data) {
		ObjectNode message = mapper.createObjectNode();
		message.put("jsonrpc", "2.0");
		if (id != null) {
			if (id instanceof Integer) {
				message.put("id", (Integer) id);
			} else if (id instanceof Long) {
				message.put("id", (Long) id);
			} else {
				message.put("id", id.toString());
			}
		}
		message.set("result", data);
		return message;
	}

	private static ObjectNode createErrorResponse(Object id, String error) {
		ObjectNode message = mapper.createObjectNode();
		message.put("jsonrpc", "2.0");
		if (id != null) {
			if (id instanceof Integer) {
				message.put("id", (Integer) id);
			} else if (id instanceof Long) {
				message.put("id", (Long) id);
			} else {
				message.put("id", id.toString());
			}
		}

		ObjectNode errorObj = mapper.createObjectNode();
		errorObj.put("code", -32603);
		errorObj.put("message", error);
		message.set("error", errorObj);

		return message;
	}

	private static void debug(String message) {
		if (DEBUG) {
			System.out.println("[MCP-DEBUG] " + message);
		}
	}

	public static void launchMcpService(int mcpport) {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(mcpport), 0);
			server.createContext("/sse", MCPServerForExamples::handleSSE);
			server.createContext("/messages", MCPServerForExamples::handleSSE); 
			server.setExecutor(null);
			server.start();
			LOGGER.info("MCP Server running on port " + mcpport);
		} catch (Exception e) {
			LOGGER.error("Caught exception while starting MCP server .." + e.getMessage());
			System.exit(1);
		}
	}
	
}