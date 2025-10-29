package ntt.security.ollamadrama.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.objects.ToolCallRequest;
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
				true, // use MCP
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
				true, // use MCP
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
				true, // use MCP
				"llama3.1:70b,cogito:14b", // cogito:14b 
				"What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius."
				+ "\n"
				+ "Response from running tool_call fetch_temperature(\"location\"=\"Paris\", \"unit\"=\"Celcius\"):\n"
				+ "\n"
				+ " * content type     : text\n"
				+ " * content text     :\n"
				+ " ----------------------------"
				+"The temperature in Paris is currently 21 degrees",
				"21",
				true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard3.evaluate();
		scorecard3.print();

	}

	@Test
	public void simple_MCP_Tool_Request_Test() {
		
		String tcrs = "oneshot fetchA(url=\"https://www.ntt.com\", max_length=5000, start_index=0, raw=false),continous fetchB(url=\"https://www.ntt.com\", start_index=0, raw=false), oneshot fetchC(url=\"https://www.ntt.com\", max_length=5000, start_index=0)";
		ArrayList<ToolCallRequest> tool_calls = MCPUtils.parseToolCalls(tcrs);
		
		int toolindex = 1;
		for (ToolCallRequest tcr: tool_calls) {
			System.out.println(" - tcr toolname: " + tcr.getToolname());
			System.out.println(" - tcr calltype: " + tcr.getCalltype());
			System.out.println(" - tcr arg keys: " + tcr.getArguments().keySet());
			if (1 == toolindex) {
				assertEquals("Ensure tool name is correct", "fetchA", tcr.getToolname());
				assertEquals("Ensure tool calltype is correct", "oneshot", tcr.getCalltype());
				assertEquals("Ensure tool args keys is correct", "[start_index, raw, url, max_length]", tcr.getArguments().keySet().toString());
			}
			if (2 == toolindex) {
				assertEquals("Ensure tool name is correct", "fetchB", tcr.getToolname());
				assertEquals("Ensure tool calltype is correct", "continous", tcr.getCalltype());
				assertEquals("Ensure tool args keys is correct", "[start_index, raw, url]", tcr.getArguments().keySet().toString());
			}
			if (3 == toolindex) {
				assertEquals("Ensure tool name is correct", "fetchC", tcr.getToolname());
				assertEquals("Ensure tool calltype is correct", "oneshot", tcr.getCalltype());
				assertEquals("Ensure tool args keys is correct", "[start_index, url, max_length]", tcr.getArguments().keySet().toString());
			}
			toolindex++;
		}
	}
	
	@Test
	public void simple_HTTP_MCP_Tool_Test_fetch() {

		boolean make_tools_available = true;
		String model_name = "qwen2.5:72b"; // qwen3:14b cogito:14b
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setOllama_models(model_name);
		settings.setMcp_scan(true);
		settings.setMcp_blind_trust(true);
		settings.sanityCheck();
		OllamaService.getInstance(settings);

		// Launch strict session
		OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, make_tools_available);
		if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");
		
		// Make query with tools enabled
		SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion("Is the site https://www.ntt.com accessible? Answer with 'Yes' or 'No'.");
		assertEquals("Ensure tool_call to fetch() is run and validates site availability ", "Yes", ssr1.getResponse());

		System.out.println(a1.getChatHistory());
	}

	//@Ignore
	@Test
	public void simple_HTTP_MCP_ListTools_MANUAL_Test() {

		// vars
		String mcpURL = "http://127.0.0.1:7777"; // 192.168.100.95:8080 // http://127.0.0.1:8080

		// List all available tools
		ListToolsResult tools = MCPUtils.listToolFromMCPEndpoint(mcpURL);
		if (null == tools) {
			assertFalse("Make sure we see at least 1 exposed tool", true);
		} else {
			assertFalse("Make sure we see at least 1 exposed tool", tools.tools().isEmpty());
			String available_tools_str = MCPUtils.prettyPrint(tools);
			System.out.println(available_tools_str);
		}

	}

	@Ignore
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
				true, // use MCP
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
				OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, "", true);
				if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");

				// Make query
				String q1 = "What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius.\n" + available_tools_str;

				System.out.println("Question: " + q1);
				SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion(q1, false, settings.getOllama_timeout());
				ssr1 = OllamaUtils.applyResponseSanity(ssr1, model_name, false);
				ssr1.print();
			}
		}

		// Fake temperature data
		ModelsScoreCard scorecard3 = OllamaDramaUtils.populateScorecardsForOllamaModels(
				true,
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


	//@Ignore
	@Test
	public void simpleToolTestMeaningOfLife() {

		// Launch MCP service
		LOGGER.info("Starting MCP server on port {}", 5656);
		MCPServer.launchMcpService(5656);

		boolean make_tools_available = true;
		String model_name = "qwen2.5:72b"; // qwen3:32b
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setOllama_models(model_name);
		settings.setElevenlabs_apikey("");
		settings.setMcp_scan(true);
		settings.setMcp_blind_trust(true);
		settings.setMcp_ports_csv("5656");
		OllamaService.getInstance(settings);

		String initial_prompt = """
				You are an AI assistant known as 'poi', always eager to use available tools to answer user questions. 

				Key Guidelines:
				- Answer directly if you are confident in your answer with your current knowledge.
				- If you are hesitating in your own knowledge, dont hesitate to use any tools available to lookup information. 

				""";

		// Launch strict session
		OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, initial_prompt, make_tools_available);
		if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");

		String prompt = "What is the meaning of life?";
		SingleStringQuestionResponse ssqr = a1.askStrictChatQuestion(prompt, make_tools_available, 10);

		assertTrue("Ensure result is 42", "42".equals(ssqr.getResponse()));
	}

	
	@Test
	public void simpleToolTestCurrentTime() {

		// Launch MCP service
		LOGGER.info("Starting MCP server on port {}", 5656);
		MCPServer.launchMcpService(5656);

		boolean make_tools_available = true;
		String model_name = "qwen2.5:72b"; // qwen3:32b
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setOllama_models(model_name);
		settings.setElevenlabs_apikey("");
		settings.setMcp_scan(true);
		settings.setMcp_blind_trust(true);
		settings.setMcp_ports_csv("5656");
		OllamaService.getInstance(settings);

		String initial_prompt = """
				You are an AI assistant known as 'poi', always eager to use available tools to answer user questions. 

				Key Guidelines:
				- Answer directly if you are confident in your answer with your current knowledge.
				- If you are hesitating in your own knowledge, dont hesitate to use any tools available to lookup information. 

				""";

		// Launch strict session
		OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, initial_prompt, make_tools_available);
		if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");

		String prompt = "What is the current time?";
		SingleStringQuestionResponse ssqr1 = a1.askStrictChatQuestion(prompt, make_tools_available, 10);

		assertTrue("Ensure result starts with 20", ssqr1.getResponse().startsWith("20"));
	}
	
	
	@Test
	public void simpleToolDecodeTheHiddenFunction() {

		// Launch MCP service to expose tool use_hidden_algorithm_with_two_numbers()
		LOGGER.info("Starting MCP server on port {}", 5656);
		MCPServer.launchMcpService(5656);

		// Tool settings
		int max_recursive_toolcall_depth = 20;
		
		// Models used
		String agent1_model = "llama3.1:70b"; // qwen3:32b llama3.1:70b qwen3:32b
		String judge_model = "qwen3:32b"; // qwen3:32b llama3.1:70b qwen3:32b
		
		// OllamaDrama settings
		boolean make_tools_available = true;
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setOllama_models(agent1_model + "," + judge_model);
		settings.setElevenlabs_apikey("");
		settings.setMcp_scan(true);
		settings.setMcp_blind_trust(true);
		settings.setMcp_ports_csv("5656");
		OllamaService.getInstance(settings);

		// Agent1 task: Determine hidden function
		String initial_prompt1 = """
				You are an AI mathematical genius known as 'poi', always eager to take on challenges and unravel hidden secrets. 

				Challenge:
				- Call the tool use_hidden_algorithm_with_two_numbers() with numeric arguments of your choice and attempt to determine the underlying function.
				- Once you think you have found a pattern, you MUST verify your assumed function with additional tool calls
				- Only reply when you are 100% sure about the underlying function
				- Once you believe you have decoded the underlying function, call the function 3 more times to verify and then provide the function as your response. 
				- Your response MUST only be the actual function

				""";

		OllamaSession a1 = OllamaService.getStrictProtocolSession(agent1_model, initial_prompt1, make_tools_available);
		if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + agent1_model + "] is operational\n");
		
		String prompt1 = "What is your next action?";
		SingleStringQuestionResponse ssqr1 = a1.askStrictChatQuestion(prompt1, max_recursive_toolcall_depth);
		
		System.out.println("==========================================================");
		System.out.println("==================      JUDGE PHASE      =================");
		System.out.println("==========================================================");
		
		// Agent1 task: Determine hidden function
		String initial_prompt2 = """
				You are an AI mathematical genius known as 'poipoi', always eager to take on challenges and verify mathematical claims. 

				Challenge:
				- A competition is held where participants are tasked with unfolding the hidden mathematical function behind the tool use_hidden_algorithm_with_two_numbers()
				- Your task is to call the tool use_hidden_algorithm_with_two_numbers() with numeric arguments of your choice to verify participant claims for the underlying function.
				- You MUST verify the claim using multiple examples in order to be sure of the result
				- If you find that the participant function claim is false, respond with 'INCORRECT'
				- If you find that the participant function claim is true, respond with 'CORRECT' 

				""";

		OllamaSession a2 = OllamaService.getStrictProtocolSession(judge_model, initial_prompt2, make_tools_available);
		if (a2.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + judge_model + "] is operational\n");
		
		String prompt2 = "A participant claims that the underlying function is '" + ssqr1.getResponse() + ". What is your next action?";
		SingleStringQuestionResponse ssqr2 = a2.askStrictChatQuestion(prompt2, max_recursive_toolcall_depth);
		
		boolean correct_result = false;
		if (ssqr1.getResponse().contains("num1 + num2 + 1")) correct_result = true;
		if (ssqr1.getResponse().contains("x + y + 1")) correct_result = true;
		
		assertTrue("Ensure result exists", correct_result);
		assertTrue("Ensure result verified by judge", ssqr2.getResponse().equals("CORRECT"));
		
	}
	
	
}
