package ntt.security.ollamadrama.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.objects.sessions.OllamaSession;
import ntt.security.ollamadrama.singletons.OllamaService;
import ntt.security.ollamadrama.utils.MCPUtils;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;

public class MCPTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(MCPTest.class);

	// Requires running fetch MCP server locally
	@SuppressWarnings("serial")
	@Test
	public void simple_HTTP_MCP_CallTool_NoLLM_Test() {

		// vars
		String mcpURL = "http://localhost:8080";
		String mcpPATH = "/sse";
		ArrayList<String> suggested_actions = new ArrayList<String>() {{
			this.add("fetch(url=\"http://www.ntt.com\", max_length=5000, start_index=0, raw=false)");
		}};

		// List all available tools
		ListToolsResult tools = MCPUtils.listToolFromMCPEndpoint(mcpURL, "/sse", 30L);
		assertFalse("Make sure we see at least 1 exposed tool", tools.tools().isEmpty());
		String available_tools_str = MCPUtils.prettyPrint(tools);
		System.out.println(available_tools_str);

		// Call all tools and verify sane responses
		CallToolResult result = new CallToolResult("", true);
		for (String action: suggested_actions) {

			// Extract arguments from suggested action/tool calls
			HashMap<String, Object> arguments = MCPUtils.parseArguments(action);

			// Call tool
			result = MCPUtils.callToolUsingMCPEndpoint(mcpURL, mcpPATH, action, arguments, 30L);
			String result_str = MCPUtils.prettyPrint(result);
			System.out.println(result_str);
			assertFalse("Fetch tool returned an error", result.isError());
		}

	}

	@SuppressWarnings("serial")
	@Test
	public void simple_HTTP_MCP_Temperature_LLMToolCallOutput_Test() {

		// First lets ask without tool output, dont show the tools
		HashMap<String, Integer> acceptable_answers1 = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 2); 			// OK to set LOWPROBA since OOikiOOA is completely fictional
		}};
		ModelsScoreCard scorecard1 = OllamaDramaUtils.populateScorecardsForOllamaModels(
				"llama3.1:70b,cogito:14b", // cogito:14b 
				"What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius.",
				acceptable_answers1,
				false, false);
		System.out.println("SCORECARD:");
		scorecard1.evaluate();
		scorecard1.print();

		// Next provide fake information about tools available for the task
		HashMap<String, Integer> acceptable_answers2 = new HashMap<String, Integer>() {{
			this.put("TOOLCALL", 1); 
		}};
		ModelsScoreCard scorecard2 = OllamaDramaUtils.populateScorecardsForOllamaModels(
				"llama3.1:70b,cogito:14b", // cogito:14b 
				"What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius."
				+ "\n\n"
				+ "MCP TOOLS AVAILABLE:\n"
				+ "---\n"
				+ "Tool: fetch_temperature\n"
				+ "Description: Fetches the current temperature of a City in Celcius or Fahrenheit\n"
				+ "Inputs:\n" 
				+ "  - location (string) [required]: The City of interest\n"
				+ "  - unit (string): The temperature unit\n"
				+ "Example usage: fetch_temperature(location=\"New York\", unit=\"Celcius\")\n",
				acceptable_answers2,
				false, false);
		System.out.println("SCORECARD:");
		scorecard2.evaluate();
		scorecard2.print();

		// Finally provide prompt with tool output available
		ModelsScoreCard scorecard3 = OllamaDramaUtils.populateScorecardsForOllamaModels(
				"llama3.1:70b,cogito:14b", // cogito:14b 
				"What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius."
				+ "\n"
				+ "TOOL CALL OUTPUT:\n"
				+ "Tool call: fetch_temperature(\"location\"=\"Paris\", \"unit\"=\"Celcius\"\n"
				+ "Tool call output: The temperature in Paris is currently 21 degrees",
				"21",
				true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard3.evaluate();
		scorecard3.print();

	}

	@Test
	public void simple_HTTP_MCP_Tool_Test() {

		boolean make_tools_available = true;
		String model_name = "qwen3:14b"; // qwen3:14b cogito:14b
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setOllama_models(model_name);
		settings.sanityCheck();
		OllamaService.getInstance(settings);

		// Launch strict session
		OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name);
		if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");

		// Make query with tools enabled
		SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion("Is the site https://www.ntt.com accessible?", make_tools_available);
		ssr1.print();
		assertEquals("Ensure tool_call to fetch() is run and validates site availability ", "Yes", ssr1.getResponse());

	}

	@Test
	public void simple_HTTP_MCP_ListTools_MANUAL_Test() {

		// vars
		String mcpURL = "http://localhost:9000";

		// List all available tools
		ListToolsResult tools = MCPUtils.listToolFromMCPEndpoint(mcpURL);
		assertFalse("Make sure we see at least 1 exposed tool", tools.tools().isEmpty());
		String available_tools_str = MCPUtils.prettyPrint(tools);
		System.out.println(available_tools_str);

	}

	@SuppressWarnings("serial")
	@Test
	public void simple_HTTP_MCP_SiteUp_LLMToolCallTest() {

		// vars
		String mcpURL = "http://localhost:8080";

		// First lets ask without tool output
		HashMap<String, Integer> acceptable_answers1 = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 2); 			// OK to set LOWPROBA since OOikiOOA is completely fictional
		}};
		ModelsScoreCard scorecard1 = OllamaDramaUtils.populateScorecardsForOllamaModels(
				"llama3.1:70b,cogito:14b", // cogito:14b 
				"What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius.", 
				acceptable_answers1,
				true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard1.evaluate();
		scorecard1.print();

		// List all available tools
		ListToolsResult tools = MCPUtils.listToolFromMCPEndpoint(mcpURL, "/sse", 30L);
		assertFalse("Make sure we see at least 1 exposed tool", tools.tools().isEmpty());
		String available_tools_str = MCPUtils.prettyPrint(tools);
		System.out.println(available_tools_str);

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();
		OllamaService.getInstance(settings);
		for (String model_name: settings.getOllama_models().split(",")) {
			if (model_name.length()>=3) {

				// Launch strict session
				OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, false, true);
				if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");

				// Make query
				String q1 = "What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius.\n" + available_tools_str;

				System.out.println("Question: " + q1);
				SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion(q1, false, settings.getOllama_timeout());
				ssr1 = OllamaUtils.applyResponseSanity(ssr1, model_name, false);
				ssr1.print();
				System.exit(1);
			}
		}
		System.exit(1);


		/*
		ModelsScoreCard scorecard3 = OllamaDramaUtils.populateScorecardsForOllamaModels(
				"llama3.1:70b,cogito:14b", // cogito:14b 
				"What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius."
				+ "\n"
				+ "TOOL CALL OUTPUT:\n"
				+ "Tool call: fetch_temperature(\"location\"=\"Paris\", \"unit\"=\"Celcius\"\n"
				+ "Tool call output: The temperature in Paris is currently 21 degrees",
				"21",
				true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard3.evaluate();
		scorecard3.print();
		 */

	}

}
