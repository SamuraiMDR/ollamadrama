package ntt.security.ollamadrama.mcp;

import static org.junit.Assert.assertFalse;

import java.time.Duration;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

public class MCPTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(MCPTest.class);

	// Requires running fetch MCP server locally
	// - cd scripts/mcp 
	// - docker build -t mcp-fetch-proxy .
	// - docker run --network=host -p 8080:8080 mcp-fetch-proxy
	@Test
	public void simple_HTTP_MCPTest() {

		HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder("http://localhost:8080")
				.sseEndpoint("/sse")
				.customizeClient(builder -> builder.connectTimeout(Duration.ofSeconds(30)))
				.build();

		McpSyncClient client = McpClient.sync(transport)
				.requestTimeout(Duration.ofSeconds(10))
				.capabilities(ClientCapabilities.builder().roots(true).build())
				.build();

		client.initialize();
		ListToolsResult tools = client.listTools();
		System.out.println("Available tools: " + tools);

		CallToolResult result = client.callTool(
				new CallToolRequest("fetch", Map.of(
						"url", "https://example.com",
						"raw", true
						))
				);
		System.out.println("Result: " + result);

		assertFalse("Fetch tool returned an error", result.isError());

	}

}
