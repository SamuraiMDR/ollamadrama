package ntt.security.ollamadrama.singletons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ollama4j.OllamaAPI;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.MCPEndpoint;
import ntt.security.ollamadrama.objects.MCPTool;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.SessionType;
import ntt.security.ollamadrama.objects.sessions.OllamaSession;
import ntt.security.ollamadrama.utils.*;

public class OllamaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaService.class);

	private static volatile OllamaService single_instance = null;
	private static OllamaDramaSettings settings = new OllamaDramaSettings();

	private static final int threadPoolCount = 20;
	private static final long mcp_listtools_timeout = 5L;

	private static ArrayList<String> service_cnets;

	// ollama url as key
	private static TreeMap<String, OllamaEndpoint> ollama_endpoints = new TreeMap<>();

	// key is schema://host:port-toolname
	private static TreeMap<String, MCPTool> mcp_tools = new TreeMap<>();

	private OllamaService(OllamaDramaSettings _settings) {
		super();

		if (null == _settings) {
			// fallback to env variables
			LOGGER.info("Getting Ollama settings from environment");
			_settings = ConfigUtils.parseConfigENV();
		}
		settings = _settings;

		// Block and find at least one ollama endpoint
		rescan(true);
	}

	public synchronized void rescan(boolean blockUntilReady) {
		ArrayList<String> serviceips = NetUtilsLocal.determineLocalIPv4s();
		if (serviceips.isEmpty()) {
			LOGGER.error("Unable to find any connected networks using NetUtils.determineLocalIPv4s()");
			SystemUtils.halt();
		}
		LOGGER.info("Owned ips: " + serviceips);

		ArrayList<String> service_cnets_temp = new ArrayList<>();
		for (String ip : serviceips) {
			String cnet = NetUtilsLocal.grabCnetworkSlice(ip);
			service_cnets_temp.add(cnet);
		}
		synchronized (OllamaService.class) {
			service_cnets = service_cnets_temp;
		}

		// before launch, make sure we can find at least one ollama server
		boolean found_ollamas = wireOllama(true);
		LOGGER.info("found_ollamas: " + found_ollamas);

		// optional check for mcp servers

		boolean found_mcps = false;
		if (settings.isMcp_scan()) {
			found_mcps = wireMCPs(false);
			LOGGER.info("found_mcps: " + found_mcps);
			if (found_mcps) {
				for (String tool: getMcp_tools().keySet()) {
					System.out.println(" - " + tool);
				}
			}
		}
	}


	@SuppressWarnings("serial")
	public static boolean wireMCPs(boolean blockUntilReady) {
		LOGGER.info("wireMCPs()");

		boolean found_mcps = false;
		int mcp_attempt_counter = 0;
		boolean mcp_abort = false;
		HashMap<String, Boolean> deduptool = new HashMap<>();

		// key is schema://host:port-toolname
		TreeMap<String, MCPTool> verified_tools = new TreeMap<>();

		// key is host:port
		TreeMap<String, MCPEndpoint> abandoned_mcps = new TreeMap<>();

		while (!found_mcps && !mcp_abort) {
			TreeMap<String, MCPEndpoint> mcps = new TreeMap<String, MCPEndpoint>();

			if (settings.isMcp_scan()) {
				if (blockUntilReady) LOGGER.info("Looking for open MCP server ports .. " + service_cnets);
				mcps = NetUtilsLocal.performTCPPortSweepForMCP(settings.getMcp_ports(), service_cnets, 1, 255, 10, threadPoolCount);
			} 

			if (mcps.isEmpty() && (null == settings.getSatellites())) {
				LOGGER.warn("Unable to find any hosts listening on MCP ports " + settings.getMcp_ports() + ", sweeped the networks " + service_cnets + ", will keep trying a couple of more times");
				SystemUtils.sleepInSeconds(5);
			} else {
				LOGGER.info("activeHosts for MCP ports " + settings.getMcp_ports().toString() + ": " + mcps.keySet());

				if (null != settings.getMcp_satellites() && !settings.getMcp_satellites().isEmpty()) {
					LOGGER.info("We have defined satellite mcp endpoints, first entry is " + settings.getMcp_satellites().get(0).getHost() + "..");
					for (MCPEndpoint oep: settings.getMcp_satellites()) {
						String mcpkey_noschema_nopath = oep.getHost() + ":" + oep.getPort();
						if (null != abandoned_mcps.get(mcpkey_noschema_nopath)) {
							LOGGER.info("We have defined satellite mcp endpoints, but its been temporarily abandoned .. " + mcpkey_noschema_nopath + " (will reset after 3 loops)");
						} else {
							if (null == mcps.get(mcpkey_noschema_nopath)) {
								LOGGER.info("Adding mcp endpoint " + mcpkey_noschema_nopath);
								mcps.put(mcpkey_noschema_nopath, oep);
							} else {
								LOGGER.info(mcpkey_noschema_nopath + " is already in our list of candidates");
							}
						}
					}
				}

				boolean mcp_listtools_reply = false;
				for (String mcpkey_noschema_nopath: mcps.keySet())  {
					MCPEndpoint oep = mcps.get(mcpkey_noschema_nopath);

					if (null != abandoned_mcps.get(mcpkey_noschema_nopath)) {
						LOGGER.info("Skipping MCP endpoint " + mcpkey_noschema_nopath + ", it is active but has been temporarily abandone (will reset after 10 loops)");
					} else {
						LOGGER.debug("Connecting to to MCP endpoint " + mcpkey_noschema_nopath + " .. which is not in the abandoned list: " + abandoned_mcps.keySet());

						ArrayList<String> schemas = new ArrayList<String>() {{
							this.add("http");
							this.add("https");
						}};

						ArrayList<String> endpoint_paths = settings.getMcp_sse_paths();
						if (null == endpoint_paths) endpoint_paths = new ArrayList<String>() {{
							this.add("/sse");
						}};
						if (endpoint_paths.isEmpty()) endpoint_paths = new ArrayList<String>() {{
							this.add("/sse");
						}};

						for (String endpoint_path: endpoint_paths) {
							boolean endpoint_success = false;
							for (String schema: schemas) {
								if (NetUtilsLocal.isValidIPV4(oep.getHost()) && "https".equals(schema)) {
									// dont attempt https + ip, wont be accepted
								} else {
									if (!endpoint_success) {
										String mcpURL = schema + "://" + oep.getHost() + ":" + oep.getPort();
										LOGGER.info("Running listTools() against " + mcpURL + " ... with a " + mcp_listtools_timeout + " second timeout");

										// List all available tools
										ListToolsResult tools = MCPUtils.listToolFromMCPEndpoint(mcpURL, endpoint_path, mcp_listtools_timeout);
										if (null != tools) {
											if (!tools.tools().isEmpty()) {
												for (Tool t: tools.tools()) {
													// key is schema://host:port-toolname
													String tool_str = MCPUtils.prettyPrint(tools, t.name());

													verified_tools.put(mcpURL + "-" + t.name(), new MCPTool(t.name(), tool_str, new MCPEndpoint(schema, oep.getHost(), oep.getPort(), endpoint_path)));
													if (null == deduptool.get(t.name())) {
														LOGGER.info("Found MCP tool " + t.name());
														deduptool.put(t.name(), true);
													}
													endpoint_success = true;
												}
											}
										}
									}
								}
							}
						}
					}
				}

				if (mcp_listtools_reply && verified_tools.isEmpty()) {
					LOGGER.warn("Found hosts listening on mcp ports " + settings.getMcp_ports() + ", but none of them seem to respond well to our listTools call");
					SystemUtils.sleepInSeconds(5);
				} else if (!mcp_listtools_reply && verified_tools.isEmpty()) {
					LOGGER.warn("Found no hosts listening on mcp ports " + settings.getMcp_ports() + ", we will need to keep looking (mcp_attempt_counter: " + mcp_attempt_counter + ")");
					SystemUtils.sleepInSeconds(30);
				} else {
					if (blockUntilReady) LOGGER.info("Verified mcp service endpoints: " + verified_tools.keySet());
					mcp_tools = verified_tools;
					found_mcps = true;
				}
			}

			mcp_attempt_counter++;

			// always give endpoints another chance
			if (mcp_attempt_counter>10) {
				if (!abandoned_mcps.isEmpty()) LOGGER.info("OK time to clear and retry with the abandoned MCP endpoints");
				abandoned_mcps = new TreeMap<>();
			}

			if (!blockUntilReady && (mcp_attempt_counter>=3)) {
				LOGGER.warn("Was attempting to update the list of mcp servers but found none");
				mcp_abort = true;
			}
		}

		return found_mcps;
	}


	public static boolean wireOllama(boolean blockUntilReady) {
		LOGGER.info("wireOllamas() - " + settings.getOllama_models());
		boolean found_ollamas = false;
		int ollama_attempt_counter = 0;
		boolean ollama_abort = false;
		TreeMap<String, OllamaEndpoint> verified_ollamas = new TreeMap<>();
		TreeMap<String, OllamaEndpoint> abandoned_ollamas = new TreeMap<>();
		while (!found_ollamas && !ollama_abort) {
			TreeMap<String, OllamaEndpoint> ollamas = new TreeMap<String, OllamaEndpoint>();

			if (settings.isOllama_scan()) {
				if (blockUntilReady) LOGGER.info("Looking for ollama servers .. " + service_cnets);
				ollamas = NetUtilsLocal.performTCPPortSweepForOllama(settings.getOllama_port(), service_cnets, 1, 255, 10, threadPoolCount, settings.getOllama_username(), settings.getOllama_password());
			} 

			if (ollamas.isEmpty() && (null == settings.getSatellites())) {
				LOGGER.warn("Unable to find any hosts listening on ollama port " + settings.getOllama_port() + ", sweeped the networks " + service_cnets + ", will keep trying");
				SystemUtils.sleepInSeconds(5);
			} else {
				if (blockUntilReady) LOGGER.info("activeHosts for ollama port " + settings.getOllama_port() + ": " + ollamas.keySet());

				if (null != settings.getSatellites() && !settings.getSatellites().isEmpty()) {
					LOGGER.info("We have defined satellite ollama endpoints, first entry is " + settings.getSatellites().get(0).getOllama_url() + "..");
					for (OllamaEndpoint oep: settings.getSatellites()) {
						if (null != abandoned_ollamas.get(oep.getOllama_url())) {
							LOGGER.info("We have defined satellite ollama endpoints, but its been temporarily abandoned .. " + oep.getOllama_url() + " (will reset after 10 loops)");
						} else {
							if (null == ollamas.get(oep.getOllama_url())) {
								LOGGER.info("Adding ollama endpoint " + oep.getOllama_url());
								ollamas.put(oep.getOllama_url(), oep);
							} else {
								LOGGER.info(oep.getOllama_url() + " is already in our list of candidates");
							}
						}
					}
				}

				boolean ollama_ping_reply = false;
				for (String ollama_url: ollamas.keySet())  {
					OllamaEndpoint oep = ollamas.get(ollama_url);

					if (null != abandoned_ollamas.get(oep.getOllama_url())) {
						LOGGER.info("Skipping ollama endpoint " + oep.getOllama_url() + ", it is active but has been temporarily abandone (will reset after 10 loops)");
					} else {
						LOGGER.debug("Connecting to to ollama URL " + oep.getOllama_url() + " .. which is not in the abandoned list: " + abandoned_ollamas.keySet());

						OllamaAPI ollamaAPI = OllamaUtils.createConnection(oep, settings.getOllama_timeout());

						try {
							if (ollamaAPI.ping()) {
								ollama_ping_reply = true;
								LOGGER.info("The ollama URL " + oep.getOllama_url() + " replied with a proper ping");
								LOGGER.info("Models available on " + oep.getOllama_url() + ": " + OllamaUtils.getModelsAvailable(ollamaAPI).toString());
								LOGGER.info("Making sure we have the models we need on " + oep.getOllama_url() + " ..");
								boolean model_exists = false;

								for (String modelname: settings.getOllama_models().split(",")) {
									if (modelname.length()>3) {
										if (null == abandoned_ollamas.get(oep.getOllama_url())) {
											if (!OllamaUtils.verifyModelAvailable(ollamaAPI, modelname)) {
												if (OllamaUtils.is_skip_model_autopull(settings.getAutopull_max_llm_size(), modelname)) {
													LOGGER.info("Ollamadrama is configured to skip autopull of the model " + modelname + ", try to pull it manually once you have ensured your endpoint has enough VRAM. Or change the ollamadrama 'autopull_max_llm_size' configuration. ");
													SystemUtils.sleepInSeconds(10);
													LOGGER.warn("Skipping the ollama endpoint " + ollama_url);
													abandoned_ollamas.put(oep.getOllama_url(), oep);
												} else {
													LOGGER.warn("Unable to find required model " + modelname + " on " + oep.getOllama_url() + ", will try to pull. This may take some time ..");
													boolean success_pull = OllamaUtils.pullModel(ollamaAPI, modelname);
													if (success_pull) {
														if (OllamaUtils.verifyModelAvailable(ollamaAPI, modelname)) model_exists = true;
													}
												}
											} else {
												model_exists = true;
											}
											if (model_exists) {
												LOGGER.info("Performing simple sanity check on Ollama model " + modelname + " on " + oep.getOllama_url());
												if (!OllamaUtils.verifyModelSanityUsingSingleWordResponse(oep.getOllama_url(), ollamaAPI, modelname, 
														Globals.createStrictOptionsBuilder(modelname, true, OllamaService.getSettings().getN_ctx_override()), "Is the capital city of France named Paris? You must reply with only a single word of Yes or No." + Globals.THREAT_TEMPLATE, 
														"Yes", 1, settings.getAutopull_max_llm_size())) {
													LOGGER.warn("Unable to pass simple sanity check for " + modelname + " on " + oep.getOllama_url() + ". Abandoning the node for now.");	
													abandoned_ollamas.put(oep.getOllama_url(), oep);
													verified_ollamas.remove(oep.getOllama_url(), oep);
												} else {
													if (null == abandoned_ollamas.get(oep.getOllama_url())) {
														LOGGER.info("Verified ollama URL (with a functional instance of our preferred model " + modelname + ") found on: " + oep.getOllama_url());
														verified_ollamas.put(oep.getOllama_url(), oep);
													} else {
														LOGGER.info("ollama host (with a functional instance of our preferred model " + modelname + ") found on: " + oep.getOllama_url() + " but its ABANDONED");
													}
												}

											}
										} else {
											LOGGER.info("Skipping model check for " + modelname + " since Ollama node has been abandoned.");
										}
									}
								}
							}
						} catch (Exception e) {
							LOGGER.warn("Caught exception while attempting ping() against " + oep.getOllama_url() + ", exception: " + e.getMessage());
						}
					}
				}

				if (ollama_ping_reply && verified_ollamas.isEmpty()) {
					LOGGER.warn("Found hosts listening on ollama port " + settings.getOllama_port() + ", but none of them seem to be running well behaving versions of our required models");
					SystemUtils.sleepInSeconds(5);
				} else if (!ollama_ping_reply && verified_ollamas.isEmpty()) {
					LOGGER.warn("Found no hosts listening on ollama port " + settings.getOllama_port() + ", we will need to keep looking (ollama_attempt_counter: " + ollama_attempt_counter + ")");
					SystemUtils.sleepInSeconds(30);
				} else {
					if (blockUntilReady) LOGGER.info("Verified ollama service endpoints (all models): " + verified_ollamas.keySet());
					ollama_endpoints = verified_ollamas;
					found_ollamas = true;
				}
			}

			ollama_attempt_counter++;

			// always give endpoints another chance
			if (ollama_attempt_counter>10) {
				LOGGER.info("OK time to clear and retry with the abandoned Ollama endpoints");
				abandoned_ollamas = new TreeMap<>();
			}

			if (!blockUntilReady && (ollama_attempt_counter>10)) {
				LOGGER.warn("Was attempting to update the list of ollama servers but found none");
				ollama_abort = true;
			}
		}

		return found_ollamas;
	}

	public static OllamaService getInstance(OllamaDramaSettings _settings) {
		_settings.sanityCheck();
		if (single_instance == null) { // First check (no locking)
			synchronized (OllamaService.class) {
				if (single_instance == null) { // Second check (with locking)
					_settings.setOllama_timeout(_settings.getOllama_timeout());
					single_instance = new OllamaService(_settings);
				}
			}
		}
		return single_instance;
	}

	public static OllamaService getInstance(String _model_names, OllamaDramaSettings _settings) {
		_settings.setOllama_models(_model_names);
		return getInstance(_settings);
	}

	public static OllamaService getInstance(String _model_names) {
		OllamaDramaSettings settings = new OllamaDramaSettings();
		settings.setOllama_models(_model_names);
		return getInstance(settings);
	}

	public static OllamaService getInstance() {
		return getInstance((OllamaDramaSettings) null);
	}

	public static OllamaDramaSettings getSettings() {
		return settings;
	}

	public static ArrayList<String> getService_cnets() {
		return service_cnets;
	}

	public static TreeMap<String, OllamaEndpoint> getollama_hosts() {
		return ollama_endpoints;
	}

	public static OllamaEndpoint getRandomActiveOllamaURL() {
		while (true) {
			int size = ollama_endpoints.size();
			while (ollama_endpoints.size() == 0) {
				LOGGER.warn("No Ollama hosts currently available ... blocking and will rescan every 30 seconds");
				SystemUtils.sleepInSeconds(30);
			}
			int selection = NumUtils.randomNumWithinRangeAsInt(1, size);
			int index = 1;
			for (String ollama_url: ollama_endpoints.keySet()) {
				OllamaEndpoint oep = ollama_endpoints.get(ollama_url);
				if (selection == index) return oep;
				index++;
			}

			LOGGER.warn("No Ollama hosts available ... blocking and rescanning in 30 seconds");
			SystemUtils.sleepInSeconds(30);
		}
	}

	public static OllamaSession getStrictProtocolSession(String _model_name) {
		return getStrictProtocolSession(_model_name, false, false, "", false);
	}

	public static OllamaSession getStrictProtocolSession(String _model_name, String _initial_prompt, boolean _make_tools_available) {
		return getStrictProtocolSession(_model_name, false, false, _initial_prompt, _make_tools_available);
	}

	public static OllamaSession getStrictProtocolSession(String _model_name, boolean hide_llm_reply_if_uncertain, boolean _use_random_seed, boolean _make_tools_available) {
		return getStrictProtocolSession(_model_name, hide_llm_reply_if_uncertain, _use_random_seed, "", _make_tools_available);
	}
	
	public static OllamaSession getStrictProtocolSession(String _model_name, boolean hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		return getStrictProtocolSession(_model_name, hide_llm_reply_if_uncertain, _use_random_seed, "", false);
	}

	public static OllamaSession getStrictProtocolSession(String _model_name, boolean hide_llm_reply_if_uncertain, boolean _use_random_seed, String _initial_prompt, boolean _make_tools_available) {
		boolean model_exists = false;
		if (settings == null) {
			LOGGER.error("Is OllamaDrama initialized properly");
			SystemUtils.halt();
		}
		if (_model_name.length() <= 3) {
			LOGGER.error("Specified model name is invalid: " + _model_name);
			SystemUtils.halt();
		}
		for (String existing_model: settings.getOllama_models().split(",")) {
			if (existing_model.equals(_model_name)) model_exists = true;
		}
		if (!model_exists) {
			LOGGER.error("Attempt to create session with model not listed in settings (" + _model_name + "), halting. Models listed in settings: " + settings.getOllama_models());
			SystemUtils.halt();
		}

		String system_prompt = "";
		if (_model_name.startsWith("cogito")) system_prompt = Globals.PROMPT_TEMPLATE_COGITO_DEEPTHINK;
		system_prompt = system_prompt + 
				Globals.PROMPT_TEMPLATE_STRICT_SIMPLEOUTPUT + 
				Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_STATEMENTS + 
				Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS + 
				Globals.THREAT_TEMPLATE;

		// Append initial tool index
		String mcp_str = "";
		if (_make_tools_available) {
			String available_tool_summary = OllamaService.getAllAvailableMCPTools();
			//FilesUtils.appendToFileUNIXNoException(available_tool_summary, "mcp_tool_overview.md");
			mcp_str = "\n\n" + available_tool_summary + "\n\n";
		} else {
			mcp_str = "\n\nNO MCP TOOLS AVAILABLE.\n\n"; 
		}

		return new OllamaSession(_model_name, OllamaService.getRandomActiveOllamaURL(),
				Globals.createStrictOptionsBuilder(_model_name, _use_random_seed, OllamaService.getSettings().getN_ctx_override()), OllamaService.getSettings(), 
				system_prompt + "\n\n" + _initial_prompt + "\n\n" + mcp_str,
				SessionType.STRICTPROTOCOL);
	}

	public static OllamaSession getStrictSession(String _model_name, Boolean _use_random_seed) {
		boolean model_exists = false;
		for (String existing_model: settings.getOllama_models().split(",")) {
			if (existing_model.equals(_model_name)) model_exists = true;
		}
		if (!model_exists) {
			LOGGER.error("Attempt to create session with model not listed in settings, halting (model requested: " + _model_name + ", settings: " + settings.getOllama_models() + ")");
			SystemUtils.halt();
		}
		return new OllamaSession(_model_name, OllamaService.getRandomActiveOllamaURL(),
				Globals.createStrictOptionsBuilder(_model_name, _use_random_seed, OllamaService.getSettings().getN_ctx_override()), OllamaService.getSettings(),
				Globals.PROMPT_TEMPLATE_STRICT_COMPLEXOUTPUT,
				SessionType.STRICT);
	}

	public static OllamaSession getCreativeSession(String _model_name) {
		boolean model_exists = false;
		for (String existing_model: settings.getOllama_models().split(",")) {
			if (existing_model.equals(_model_name)) model_exists = true;
		}
		if (!model_exists) {
			LOGGER.error("Attempt to create session with model not listed in settings, halting (model requested: " + _model_name + ", settings: " + settings.getOllama_models() + ")");
			SystemUtils.halt();
		}
		return new OllamaSession(_model_name, OllamaService.getRandomActiveOllamaURL(),
				Globals.createCreativeOptionsBuilder(_model_name, OllamaService.getSettings().getN_ctx_override()), OllamaService.getSettings(),
				Globals.PROMPT_TEMPLATE_CREATIVE,
				SessionType.CREATIVE);
	}

	public static OllamaSession getDefaultSession(String _model_name) {
		boolean model_exists = false;
		for (String existing_model: settings.getOllama_models().split(",")) {
			if (existing_model.equals(_model_name)) model_exists = true;
		}
		if (!model_exists) {
			LOGGER.error("Attempt to create session with model not listed in settings, halting (model: " + _model_name + ")");
			SystemUtils.halt();
		}
		return new OllamaSession(_model_name, OllamaService.getRandomActiveOllamaURL(),
				Globals.createDefaultOptionsBuilder(), OllamaService.getSettings(), "",
				SessionType.DEFAULT);
	}

	public static String getAllAvailableMCPTools() {
		HashMap<String, Boolean> uniqtool = new HashMap<>();
		StringBuffer sb = new StringBuffer();
		sb.append("MCP TOOLS AVAILABLE:\n");
		for (String toolid: mcp_tools.keySet()) {
			LOGGER.debug("toolid: " + toolid);
			MCPTool tool = mcp_tools.get(toolid);
			if (null == uniqtool.get(tool.getToolname())) {
				if (isMatchingMCPTool(tool.getToolname(), settings.getFiltered_mcp_toolnames_csv())) {
					//filtered
				} else {
					sb.append(tool.getTool_str() + "\n");
					uniqtool.put(tool.getToolname(), true);
				}
			}
		}

		return sb.toString().replace("\n\n\n", "\n\n");
	}

	public static TreeMap<String, MCPTool> getMcp_tools() {
		TreeMap<String, MCPTool> mcp_tools_filtered = new TreeMap<String, MCPTool>();
		for (String toolid: mcp_tools.keySet()) {
			MCPTool tool = mcp_tools.get(toolid);
			//System.out.println("comparing " + tool.getToolname() + " width " + settings.getFiltered_mcp_toolnames_csv());
			if (isMatchingMCPTool(tool.getToolname(), settings.getFiltered_mcp_toolnames_csv())) {
				//filtered
			} else {
				mcp_tools_filtered.put(toolid, tool);
			}
		}

		return mcp_tools_filtered;
	}

	public static void setMcp_tools(TreeMap<String, MCPTool> mcp_tools) {
		OllamaService.mcp_tools = mcp_tools;
	}

	public static MCPTool getMCPURLForTool(String toolname) {
		for (String toolid: mcp_tools.keySet()) {
			MCPTool tool = mcp_tools.get(toolid);
			if (toolname.equals(tool.getToolname())) return tool;
		}
		return null;
	}

	public static boolean isMatchingMCPTool(String toolname, String mcp_toolnames) {
		for (String indexedtoolname: mcp_toolnames.split(",")) {
			if (toolname.equals(indexedtoolname)) return true;
		}
		return false;
	}

}
