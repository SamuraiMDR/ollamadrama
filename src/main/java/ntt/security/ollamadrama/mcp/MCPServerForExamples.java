package ntt.security.ollamadrama.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ntt.security.ollamadrama.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

public final class MCPServerForExamples {

    private static final Logger log = LoggerFactory.getLogger(MCPServerForExamples.class);
    private static final ObjectMapper json = new ObjectMapper();

    private static final String CONTENT_TYPE_SSE = "text/event-stream; charset=utf-8";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final Map<String, OutputStream> sse_streams = new ConcurrentHashMap<>();
    private static final Map<String, BlockingQueue<String>> message_queues = new ConcurrentHashMap<>();

    private MCPServerForExamples() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void handle_sse(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        log.debug("MCP request: {} {}", method, path);

        try {
            if ("GET".equals(method) && "/sse".equals(path)) {
                open_sse_connection(exchange);
            } else if ("POST".equals(method) && ("/sse".equals(path) || path.startsWith("/messages"))) {
                handle_post(exchange);
            } else {
                send_not_found(exchange);
            }
        } catch (Exception e) {
            log.error("Unhandled exception in handler", e);
            try_send_error(exchange, 500, "Internal server error");
        }
    }

    private static void open_sse_connection(HttpExchange exchange) throws IOException {
        String session_id = java.util.UUID.randomUUID().toString().replace("-", "");

        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_SSE);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
        exchange.sendResponseHeaders(200, 0);

        OutputStream output = exchange.getResponseBody();
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        sse_streams.put(session_id, output);
        message_queues.put(session_id, queue);

        write_sse_event(output, "endpoint", "/messages/?session_id=" + session_id);

        Thread worker = new Thread(new SseWorker(session_id, output, queue));
        worker.setDaemon(true);
        worker.setName("SSE-" + session_id.substring(0, 8));
        worker.start();

        log.debug("Legacy SSE session opened: {}", session_id);
    }

    private static class SseWorker implements Runnable {
        private final String session_id;
        private final OutputStream output;
        private final BlockingQueue<String> queue;

        SseWorker(String session_id, OutputStream output, BlockingQueue<String> queue) {
            this.session_id = session_id;
            this.output = output;
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (sse_streams.containsKey(session_id)) {
                    String message = queue.poll(30, TimeUnit.SECONDS);
                    if (message == null) {
                        output.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                        output.flush();
                        continue;
                    }
                    output.write(message.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                }
            } catch (IOException e) {
                log.debug("Client closed SSE connection: {}", session_id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                cleanup_session(session_id);
                close_quietly(output);
            }
        }
    }

    private static void handle_post(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) {
            send_bad_request(exchange, "Empty request body");
            return;
        }
        String payload = new String(body, StandardCharsets.UTF_8);

        String session_id = extract_session_id(exchange.getRequestURI().getQuery());
        boolean legacy = session_id != null && sse_streams.containsKey(session_id);

        if (legacy) {
            handle_legacy_post(exchange, payload, session_id);
        } else {
            handle_modern_post(exchange, payload);
        }
    }

    private static void handle_legacy_post(HttpExchange exchange, String payload, String session_id) throws IOException {
        byte[] accepted = "Accepted".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(202, accepted.length);
        exchange.getResponseBody().write(accepted);
        exchange.getResponseBody().close();

        process_jsonrpc_legacy(payload, session_id);
    }

    private static void handle_modern_post(HttpExchange exchange, String payload) throws IOException {
        // Java 17 safe: getFirst() returns null if header missing
        String accept = exchange.getRequestHeaders().getFirst("Accept");
        boolean wants_sse = accept != null && accept.contains("text/event-stream");

        JsonNode root = json.readTree(payload);
        if (root.isArray()) {
            process_batch(exchange, (ArrayNode) root, wants_sse);
        } else {
            process_single(exchange, (ObjectNode) root, wants_sse);
        }
    }

    private static void process_jsonrpc_legacy(String payload, String session_id) {
        try {
            ObjectNode req = (ObjectNode) json.readTree(payload);
            String method = req.has("method") ? req.get("method").asText("") : "";
            Object id = extract_id(req);

            if ("initialize".equals(method)) {
                queue_response(session_id, id, create_initialize_response());
            } else if ("tools/list".equals(method)) {
                queue_response(session_id, id, create_tools_list_response());
            } else if ("tools/call".equals(method)) {
                queue_response(session_id, id, handle_tool_call(req));
            } else if (method.startsWith("notifications/")) {
                // ignore
            } else {
                queue_error(session_id, id, "Unknown method: " + method);
            }
        } catch (Exception e) {
            log.warn("Failed to process legacy request", e);
            queue_error(session_id, null, "Invalid request");
        }
    }

    private static void process_single(HttpExchange exchange, ObjectNode req, boolean sse) throws IOException {
        String method = req.has("method") ? req.get("method").asText("") : "";
        Object id = extract_id(req);
        ObjectNode result;

        if ("initialize".equals(method)) {
            result = create_initialize_response();
        } else if ("tools/list".equals(method)) {
            result = create_tools_list_response();
        } else if ("tools/call".equals(method)) {
            result = handle_tool_call(req);
        } else {
            result = create_error_result("Unknown method: " + method);
        }

        send_response(exchange, create_success_response(id, result), sse);
    }

    private static void process_batch(HttpExchange exchange, ArrayNode batch, boolean sse) throws IOException {
        ArrayNode responses = json.createArrayNode();
        for (JsonNode node : batch) {
            if (!node.isObject()) continue;
            ObjectNode req = (ObjectNode) node;
            String method = req.has("method") ? req.get("method").asText("") : "";
            Object id = extract_id(req);
            ObjectNode result;

            if ("initialize".equals(method)) {
                result = create_initialize_response();
            } else if ("tools/list".equals(method)) {
                result = create_tools_list_response();
            } else if ("tools/call".equals(method)) {
                result = handle_tool_call(req);
            } else {
                result = create_error_result("Unknown method: " + method);
            }

            responses.add(create_success_response(id, result));
        }
        send_response(exchange, responses, sse);
    }

    private static ObjectNode handle_tool_call(ObjectNode request) {
        JsonNode params = request.path("params");
        if (!params.isObject() || !params.has("name") || !params.has("arguments")) {
            return tool_error("Missing 'name' or 'arguments' in tool call");
        }

        String tool_name = params.get("name").asText();
        ObjectNode args = (ObjectNode) params.get("arguments");

        if ("get_the_meaning_of_life".equals(tool_name)) {
            return tool_text("42");
        }
        if ("get_current_time_in_UTC".equals(tool_name)) {
            return tool_text(DateUtils.nowTimeStamp());
        }
        if ("use_hidden_algorithm_with_two_numbers".equals(tool_name)) {
            return hidden_algorithm_tool(args);
        }
        return tool_error("Unknown tool: " + tool_name);
    }

    private static ObjectNode hidden_algorithm_tool(ObjectNode args) {
        if (!args.has("num1") || !args.has("num2")) {
            return tool_error("Missing required parameters: num1 and num2");
        }
        try {
            int a = args.get("num1").asInt();
            int b = args.get("num2").asInt();
            int result = a + b + 1;
            return tool_text(String.valueOf(result));
        } catch (Exception e) {
            return tool_error("num1 and num2 must be valid integers");
        }
    }

    private static ObjectNode tool_text(String text) {
        ArrayNode content = json.createArrayNode();
        content.add(json.createObjectNode().put("type", "text").put("text", text));

        ObjectNode result = json.createObjectNode();
        result.set("content", content);
        result.put("isError", false);
        return result;
    }

    private static ObjectNode tool_error(String message) {
        ObjectNode obj = tool_text(message);
        obj.put("isError", true);
        return obj;
    }

    private static ObjectNode create_error_result(String message) {
        ObjectNode err = json.createObjectNode();
        err.put("code", -32603);
        err.put("message", message);
        return err;
    }

    private static String extract_session_id(String query) {
        if (query == null || !query.contains("session_id=")) return null;
        int start = query.indexOf("session_id=") + 11;
        int end = query.indexOf('&', start);
        return end == -1 ? query.substring(start) : query.substring(start, end);
    }

    private static Object extract_id(ObjectNode node) {
        JsonNode id_node = node.get("id");
        if (id_node == null || id_node.isNull()) return null;
        if (id_node.isIntegralNumber()) return id_node.asLong();
        return id_node.asText();
    }

    private static void queue_response(String session_id, Object id, ObjectNode data) {
        queue_sse(session_id, create_success_response(id, data));
    }

    private static void queue_error(String session_id, Object id, String error) {
        queue_sse(session_id, create_error_response(id, error));
    }

    private static void queue_sse(String session_id, ObjectNode payload) {
        BlockingQueue<String> queue = message_queues.get(session_id);
        if (queue == null) return;
        try {
            String json_str = json.writeValueAsString(payload);
            queue.offer("event: message\ndata: " + json_str + "\n\n");
        } catch (Exception e) {
            log.warn("Failed to queue SSE message", e);
        }
    }

    private static void write_sse_event(OutputStream os, String event, String data) throws IOException {
        String msg = "event: " + event + "\ndata: " + data + "\n\n";
        os.write(msg.getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    private static void cleanup_session(String session_id) {
        close_quietly(sse_streams.remove(session_id));
        message_queues.remove(session_id);
        log.debug("Session cleaned up: {}", session_id);
    }

    private static void close_quietly(OutputStream os) {
        if (os != null) {
            try { os.close(); } catch (IOException ignored) {}
        }
    }

    private static ObjectNode create_success_response(Object id, ObjectNode result) {
        ObjectNode msg = json.createObjectNode();
        msg.put("jsonrpc", "2.0");
        if (id instanceof Number) {
            msg.put("id", ((Number) id).longValue());
        } else if (id != null) {
            msg.put("id", id.toString());
        }
        msg.set("result", result);
        return msg;
    }

    private static ObjectNode create_error_response(Object id, String error) {
        ObjectNode msg = json.createObjectNode();
        msg.put("jsonrpc", "2.0");
        if (id instanceof Number) {
            msg.put("id", ((Number) id).longValue());
        } else if (id != null) {
            msg.put("id", id.toString());
        }
        ObjectNode err = json.createObjectNode();
        err.put("code", -32603);
        err.put("message", error);
        msg.set("error", err);
        return msg;
    }

    private static void send_response(HttpExchange exchange, JsonNode payload, boolean sse) throws IOException {
        if (sse) {
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_SSE);
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            String data = json.writeValueAsString(payload);
            os.write(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
            os.close();
        } else {
            byte[] bytes = json.writeValueAsBytes(payload);
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_JSON);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }

    private static void send_not_found(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, 0);
        exchange.close();
    }

    private static void send_bad_request(HttpExchange exchange, String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void try_send_error(HttpExchange exchange, int code, String msg) {
        try {
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (Exception ignored) {
        } finally {
            exchange.close();
        }
    }

    private static ObjectNode create_initialize_response() {
        ObjectNode result = json.createObjectNode();
        ObjectNode info = json.createObjectNode();
        info.put("name", "Samurai MCP Server");
        info.put("version", "1.0.0");
        result.set("serverInfo", info);
        result.set("capabilities", json.createObjectNode().set("tools", json.createObjectNode()));
        result.put("protocolVersion", "2024-11-05");
        return result;
    }

    private static ObjectNode create_tools_list_response() {
        ArrayNode tools = json.createArrayNode();

        tools.add(simple_tool("get_the_meaning_of_life", "Tells you the meaning of life."));
        tools.add(simple_tool("get_current_time_in_UTC", "Returns current UTC time in yyyy-MM-dd HH:mm:ss format."));

        ObjectNode algo = json.createObjectNode();
        algo.put("name", "use_hidden_algorithm_with_two_numbers");
        algo.put("description", "Uses a hidden algorithm to calculate a new number from two integers.");

        ObjectNode schema = json.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = json.createObjectNode();
        props.set("num1", json.createObjectNode().put("type", "integer").put("description", "First number"));
        props.set("num2", json.createObjectNode().put("type", "integer").put("description", "Second number"));
        schema.set("properties", props);
        schema.set("required", json.createArrayNode().add("num1").add("num2"));
        algo.set("inputSchema", schema);
        tools.add(algo);

        ObjectNode result = json.createObjectNode();
        result.set("tools", tools);
        return result;
    }

    private static ObjectNode simple_tool(String name, String description) {
        ObjectNode t = json.createObjectNode();
        t.put("name", name);
        t.put("description", description);
        t.set("inputSchema", json.createObjectNode()
                .put("type", "object")
                .set("properties", json.createObjectNode()));
        return t;
    }

    public static void launch_mcp_service(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/sse", MCPServerForExamples::handle_sse);
            server.createContext("/messages", MCPServerForExamples::handle_sse);
            server.setExecutor(null);
            server.start();
            log.info("MCPServerForExamples is running on port {}", port);
        } catch (Exception e) {
            log.error("Failed to start MCP server on port {}", port, e);
            System.exit(1);
        }
    }
}