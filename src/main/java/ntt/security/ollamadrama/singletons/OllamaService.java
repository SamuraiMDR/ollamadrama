package ntt.security.ollamadrama.singletons;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

/**
 * Singleton service for managing Ollama and MCP (Model Context Protocol) endpoints.
 * This service handles endpoint discovery, validation, and session creation.
 */
public class OllamaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaService.class);

    private static volatile OllamaService single_instance = null;
    private static OllamaDramaSettings settings = new OllamaDramaSettings();

    private static final int THREAD_POOL_COUNT = 20;
    private static final Duration MCP_LIST_TOOLS_TIMEOUT = Duration.ofSeconds(5L);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);
    private static final Duration LONG_RETRY_DELAY = Duration.ofSeconds(30);
    private static final int MAX_RETRY_ATTEMPTS = 10;
    private static final int MIN_MODEL_NAME_LENGTH = 3;

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    private static List<String> service_cnets = new ArrayList<>();
    private static final Map<String, OllamaEndpoint> ollama_endpoints = new TreeMap<>();
    private static final Map<String, MCPTool> mcp_tools = new TreeMap<>();

    private OllamaService(OllamaDramaSettings settings_param) {
        if (settings_param == null) {
            LOGGER.info("Getting Ollama settings from environment");
            settings_param = ConfigUtils.parseConfigENV();
        }
        settings = settings_param;
        rescan(true);
    }

    /**
     * Rescans for available Ollama and MCP endpoints.
     * 
     * @param block_until_ready if true, blocks until at least one Ollama endpoint is found
     */
    public void rescan(boolean block_until_ready) {
        List<String> service_ips = NetUtilsLocal.determineLocalIPv4s();
        if (service_ips.isEmpty()) {
            LOGGER.error("Unable to find any connected networks using NetUtils.determineLocalIPv4s()");
            SystemUtils.halt();
        }
        LOGGER.info("Owned IPs: {}", service_ips);

        List<String> service_cnets_temp = service_ips.stream()
                .map(NetUtilsLocal::grabCnetworkSlice)
                .toList();

        lock.writeLock().lock();
        try {
            service_cnets = new ArrayList<>(service_cnets_temp);
        } finally {
            lock.writeLock().unlock();
        }

        boolean found_ollamas = wire_ollama(block_until_ready);
        LOGGER.info("Found Ollamas: {}", found_ollamas);

        if (settings.isMcp_scan()) {
            boolean found_mcps = wire_mcps(false);
            LOGGER.info("Found MCPs: {}", found_mcps);
            
            if (found_mcps) {
                LOGGER.debug("Available MCP tools: {}", get_mcp_tools().keySet());
            }
        }
    }

    /**
     * Discovers and validates MCP endpoints.
     * 
     * @param block_until_ready if true, blocks until MCP endpoints are found
     * @return true if at least one MCP endpoint was found and validated
     */
    public static boolean wire_mcps(boolean block_until_ready) {
        LOGGER.info("Starting MCP endpoint discovery");

        boolean found_mcps = false;
        int attempt_counter = 0;
        boolean abort = false;
        Map<String, Boolean> dedup_tool = new HashMap<>();
        Map<String, MCPTool> verified_tools = new TreeMap<>();
        Map<String, MCPEndpoint> abandoned_mcps = new TreeMap<>();

        while (!found_mcps && !abort) {
            Map<String, MCPEndpoint> mcps = discover_mcp_endpoints(abandoned_mcps);

            if (mcps.isEmpty() && settings.getSatellites() == null) {
                LOGGER.warn("Unable to find MCP hosts on ports {} in networks {}", 
                        settings.getMcp_ports(), service_cnets);
                SystemUtils.sleepInSeconds((int) RETRY_DELAY.toSeconds());
            } else {
                LOGGER.info("Active MCP hosts on ports {}: {}", 
                        settings.getMcp_ports(), mcps.keySet());

                validate_mcp_endpoints(mcps, abandoned_mcps, verified_tools, dedup_tool);

                if (!verified_tools.isEmpty()) {
                    lock.writeLock().lock();
                    try {
                        mcp_tools.clear();
                        mcp_tools.putAll(verified_tools);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    
                    found_mcps = true;
                    String tool_summary = get_all_available_mcp_tools();
                    LOGGER.info("MCP Tool Index:\n\n{}\n", tool_summary);
                } else {
                    LOGGER.warn("Found MCP hosts but none responded properly to listTools");
                    SystemUtils.sleepInSeconds((int) LONG_RETRY_DELAY.toSeconds());
                }
            }

            attempt_counter++;

            if (attempt_counter > MAX_RETRY_ATTEMPTS) {
                if (!abandoned_mcps.isEmpty()) {
                    LOGGER.info("Clearing abandoned MCP endpoints for retry");
                }
                abandoned_mcps.clear();
            }

            if (!block_until_ready && attempt_counter >= 3) {
                LOGGER.warn("Aborting MCP discovery after {} attempts", attempt_counter);
                abort = true;
            }
        }

        return found_mcps;
    }

    private static Map<String, MCPEndpoint> discover_mcp_endpoints(Map<String, MCPEndpoint> abandoned_mcps) {
        Map<String, MCPEndpoint> mcps = new TreeMap<>();

        if (settings.isMcp_scan()) {
            mcps = NetUtilsLocal.performTCPPortSweepForMCP(
                    settings.getMcp_ports(), 
                    service_cnets, 
                    1, 
                    255, 
                    10, 
                    THREAD_POOL_COUNT);
        }

        add_satellite_mcps(mcps, abandoned_mcps);
        return mcps;
    }

    private static void add_satellite_mcps(Map<String, MCPEndpoint> mcps, 
                                          Map<String, MCPEndpoint> abandoned_mcps) {
        if (settings.getMcp_satellites() == null || settings.getMcp_satellites().isEmpty()) {
            return;
        }

        LOGGER.info("Processing satellite MCP endpoints");
        
        for (MCPEndpoint endpoint : settings.getMcp_satellites()) {
            String key = endpoint.getHost() + ":" + endpoint.getPort();
            
            if (abandoned_mcps.containsKey(key)) {
                LOGGER.debug("Skipping abandoned satellite: {}", key);
                continue;
            }
            
            if (!mcps.containsKey(key)) {
                LOGGER.info("Adding satellite MCP endpoint: {}", key);
                mcps.put(key, endpoint);
            }
        }
    }

    private static void validate_mcp_endpoints(Map<String, MCPEndpoint> mcps,
                                              Map<String, MCPEndpoint> abandoned_mcps,
                                              Map<String, MCPTool> verified_tools,
                                              Map<String, Boolean> dedup_tool) {
        List<String> schemas = List.of("http", "https");
        List<String> endpoint_paths = get_mcp_endpoint_paths();

        for (var entry : mcps.entrySet()) {
            String key = entry.getKey();
            MCPEndpoint endpoint = entry.getValue();

            if (abandoned_mcps.containsKey(key)) {
                LOGGER.debug("Skipping abandoned endpoint: {}", key);
                continue;
            }

            validate_single_mcp_endpoint(endpoint, schemas, endpoint_paths, 
                                        verified_tools, dedup_tool);
        }
    }

    private static List<String> get_mcp_endpoint_paths() {
        List<String> paths = settings.getMcp_sse_paths();
        if (paths == null || paths.isEmpty()) {
            return List.of("/sse");
        }
        return paths;
    }

    private static void validate_single_mcp_endpoint(MCPEndpoint endpoint,
                                                    List<String> schemas,
                                                    List<String> endpoint_paths,
                                                    Map<String, MCPTool> verified_tools,
                                                    Map<String, Boolean> dedup_tool) {
        for (String path : endpoint_paths) {
            for (String schema : schemas) {
                if (should_skip_schema(endpoint.getHost(), schema)) {
                    continue;
                }

                String mcp_url = String.format("%s://%s:%d", schema, endpoint.getHost(), endpoint.getPort());
                LOGGER.info("Testing MCP endpoint: {} with path: {}", mcp_url, path);

                try {
                    ListToolsResult tools = MCPUtils.listToolFromMCPEndpoint(
                            mcp_url, path, MCP_LIST_TOOLS_TIMEOUT.toSeconds());
                    
                    if (tools != null && !tools.tools().isEmpty()) {
                        register_mcp_tools(tools, mcp_url, endpoint, schema, path, 
                                         verified_tools, dedup_tool);
                        return; // Success, no need to try other schemas
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to list tools from {}: {}", mcp_url, e.getMessage());
                }
            }
        }
    }

    private static boolean should_skip_schema(String host, String schema) {
        return NetUtilsLocal.isValidIPV4(host) && "https".equals(schema);
    }

    private static void register_mcp_tools(ListToolsResult tools,
                                          String mcp_url,
                                          MCPEndpoint endpoint,
                                          String schema,
                                          String path,
                                          Map<String, MCPTool> verified_tools,
                                          Map<String, Boolean> dedup_tool) {
        for (Tool tool : tools.tools()) {
            String tool_str = MCPUtils.prettyPrint(tools, tool.name());
            String tool_key = mcp_url + "-" + tool.name();
            
            MCPEndpoint tool_endpoint = new MCPEndpoint(
                    schema, endpoint.getHost(), endpoint.getPort(), path);
            
            verified_tools.put(tool_key, new MCPTool(tool.name(), tool_str, tool_endpoint));
            
            if (!dedup_tool.containsKey(tool.name())) {
                LOGGER.info("Discovered MCP tool: {}", tool.name());
                dedup_tool.put(tool.name(), true);
            }
        }
    }

    /**
     * Discovers and validates Ollama endpoints.
     * 
     * @param block_until_ready if true, blocks until Ollama endpoints are found
     * @return true if at least one Ollama endpoint was found and validated
     */
    public static boolean wire_ollama(boolean block_until_ready) {
        LOGGER.info("Starting Ollama endpoint discovery for models: {}", settings.getOllama_models());

        boolean found_ollamas = false;
        int attempt_counter = 0;
        boolean abort = false;
        Map<String, OllamaEndpoint> verified_ollamas = new TreeMap<>();
        Map<String, OllamaEndpoint> abandoned_ollamas = new TreeMap<>();

        while (!found_ollamas && !abort) {
            Map<String, OllamaEndpoint> candidates = discover_ollama_endpoints(abandoned_ollamas);

            if (candidates.isEmpty() && settings.getSatellites() == null) {
                LOGGER.warn("No Ollama hosts found on port {} in networks {}", 
                        settings.getOllama_port(), service_cnets);
                SystemUtils.sleepInSeconds((int) RETRY_DELAY.toSeconds());
            } else {
                if (block_until_ready) {
                    LOGGER.info("Active Ollama hosts on port {}: {}", 
                            settings.getOllama_port(), candidates.keySet());
                }

                validate_ollama_endpoints(candidates, abandoned_ollamas, verified_ollamas);

                if (!verified_ollamas.isEmpty()) {
                    lock.writeLock().lock();
                    try {
                        ollama_endpoints.clear();
                        ollama_endpoints.putAll(verified_ollamas);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    
                    found_ollamas = true;
                    if (block_until_ready) {
                        LOGGER.info("Verified Ollama endpoints: {}", verified_ollamas.keySet());
                    }
                } else {
                    LOGGER.warn("Found Ollama hosts but none validated properly");
                    SystemUtils.sleepInSeconds((int) LONG_RETRY_DELAY.toSeconds());
                }
            }

            attempt_counter++;

            if (attempt_counter > MAX_RETRY_ATTEMPTS) {
                LOGGER.info("Clearing abandoned Ollama endpoints for retry");
                abandoned_ollamas.clear();
            }

            if (!block_until_ready && attempt_counter > MAX_RETRY_ATTEMPTS) {
                LOGGER.warn("Aborting Ollama discovery after {} attempts", attempt_counter);
                abort = true;
            }
        }

        return found_ollamas;
    }

    private static Map<String, OllamaEndpoint> discover_ollama_endpoints(
            Map<String, OllamaEndpoint> abandoned_ollamas) {
        Map<String, OllamaEndpoint> endpoints = new TreeMap<>();

        if (settings.isOllama_scan()) {
            endpoints = NetUtilsLocal.performTCPPortSweepForOllama(
                    settings.getOllama_port(),
                    service_cnets,
                    1,
                    255,
                    10,
                    THREAD_POOL_COUNT,
                    settings.getOllama_username(),
                    settings.getOllama_password());
        }

        add_satellite_ollamas(endpoints, abandoned_ollamas);
        return endpoints;
    }

    private static void add_satellite_ollamas(Map<String, OllamaEndpoint> endpoints,
                                             Map<String, OllamaEndpoint> abandoned_ollamas) {
        if (settings.getSatellites() == null || settings.getSatellites().isEmpty()) {
            return;
        }

        LOGGER.info("Processing satellite Ollama endpoints");
        
        for (OllamaEndpoint endpoint : settings.getSatellites()) {
            String url = endpoint.getOllama_url();
            
            if (abandoned_ollamas.containsKey(url)) {
                LOGGER.debug("Skipping abandoned satellite: {}", url);
                continue;
            }
            
            if (!endpoints.containsKey(url)) {
                LOGGER.info("Adding satellite Ollama endpoint: {}", url);
                endpoints.put(url, endpoint);
            }
        }
    }

    private static void validate_ollama_endpoints(Map<String, OllamaEndpoint> candidates,
                                                 Map<String, OllamaEndpoint> abandoned_ollamas,
                                                 Map<String, OllamaEndpoint> verified_ollamas) {
        for (var entry : candidates.entrySet()) {
            String url = entry.getKey();
            OllamaEndpoint endpoint = entry.getValue();

            if (abandoned_ollamas.containsKey(url)) {
                LOGGER.debug("Skipping abandoned endpoint: {}", url);
                continue;
            }

            if (validate_single_ollama_endpoint(endpoint, abandoned_ollamas)) {
                verified_ollamas.put(url, endpoint);
            }
        }
    }

    private static boolean validate_single_ollama_endpoint(OllamaEndpoint endpoint,
                                                          Map<String, OllamaEndpoint> abandoned_ollamas) {
        String url = endpoint.getOllama_url();
        OllamaAPI api = OllamaUtils.createConnection(endpoint, settings.getOllama_timeout());

        try {
            if (!api.ping()) {
                LOGGER.debug("Ping failed for: {}", url);
                return false;
            }

            LOGGER.info("Ollama endpoint {} responded to ping", url);
            LOGGER.info("Available models on {}: {}", url, OllamaUtils.getModelsAvailable(api));

            if (settings.isOllama_skip_paris_validation()) {
                LOGGER.info("Quick boot mode: skipping model validation for {}", url);
                return true;
            }

            return validate_required_models(api, endpoint, abandoned_ollamas);
            
        } catch (Exception e) {
            LOGGER.warn("Exception while validating {}: {}", url, e.getMessage());
            return false;
        }
    }

    private static boolean validate_required_models(OllamaAPI api,
                                                   OllamaEndpoint endpoint,
                                                   Map<String, OllamaEndpoint> abandoned_ollamas) {
        String[] models = settings.getOllama_models().split(",");
        
        for (String model_name : models) {
            if (model_name.length() <= MIN_MODEL_NAME_LENGTH) {
                continue;
            }

            if (!validate_model(api, endpoint, model_name, abandoned_ollamas)) {
                return false;
            }
        }
        
        return true;
    }

    private static boolean validate_model(OllamaAPI api,
                                         OllamaEndpoint endpoint,
                                         String model_name,
                                         Map<String, OllamaEndpoint> abandoned_ollamas) {
        String url = endpoint.getOllama_url();

        if (!OllamaUtils.verifyModelAvailable(api, model_name)) {
            return handle_missing_model(api, endpoint, model_name, abandoned_ollamas);
        }

        LOGGER.info("Performing sanity check on model {} at {}", model_name, url);
        
        boolean passes_sanity_check = OllamaUtils.verifyModelSanityUsingSingleWordResponse(
                url, 
                api, 
                model_name,
                Globals.createStrictOptionsBuilder(model_name, true, settings.getN_ctx_override()),
                "Is the capital city of France named Paris? Reply with only Yes or No." + 
                        Globals.THREAT_TEMPLATE,
                "Yes",
                1,
                settings.getAutopull_max_llm_size());

        if (!passes_sanity_check) {
            LOGGER.warn("Sanity check failed for model {} at {}. Abandoning endpoint.", 
                    model_name, url);
            abandoned_ollamas.put(url, endpoint);
            return false;
        }

        LOGGER.info("Successfully verified model {} at {}", model_name, url);
        return true;
    }

    private static boolean handle_missing_model(OllamaAPI api,
                                               OllamaEndpoint endpoint,
                                               String model_name,
                                               Map<String, OllamaEndpoint> abandoned_ollamas) {
        String url = endpoint.getOllama_url();

        if (OllamaUtils.is_skip_model_autopull(settings.getAutopull_max_llm_size(), model_name)) {
            LOGGER.warn("Autopull skipped for {} due to size constraints. Pull manually.", model_name);
            SystemUtils.sleepInSeconds(10);
            abandoned_ollamas.put(url, endpoint);
            return false;
        }

        LOGGER.warn("Model {} not found on {}. Attempting to pull...", model_name, url);
        boolean pull_success = OllamaUtils.pullModel(api, model_name);

        if (pull_success && OllamaUtils.verifyModelAvailable(api, model_name)) {
            LOGGER.info("Successfully pulled model {}", model_name);
            return true;
        }

        LOGGER.error("Failed to pull model {} on {}", model_name, url);
        abandoned_ollamas.put(url, endpoint);
        return false;
    }

    /**
     * Gets the singleton instance with custom settings.
     * 
     * @param settings_param custom settings, or null to use environment variables
     * @return the singleton instance
     */
    public static OllamaService getInstance(OllamaDramaSettings settings_param) {
        if (settings_param != null) {
            settings_param.sanityCheck();
        }
        
        if (single_instance == null) {
            synchronized (OllamaService.class) {
                if (single_instance == null) {
                    single_instance = new OllamaService(settings_param);
                    LOGGER.info("Created new OllamaService with models: {}", 
                            settings.getOllama_models());
                }
            }
        } else {
            LOGGER.info("Returning existing OllamaService with models: {}", 
                    settings.getOllama_models());
        }
        
        return single_instance;
    }

    /**
     * Gets the singleton instance with specified model names.
     * 
     * @param model_names comma-separated model names
     * @return the singleton instance
     */
    public static OllamaService getInstance(String model_names) {
        var fresh_settings = new OllamaDramaSettings();
        fresh_settings.setOllama_models(model_names);
        return getInstance(fresh_settings);
    }

    /**
     * Gets the singleton instance with default settings.
     * 
     * @return the singleton instance
     */
    public static OllamaService getInstance() {
        return getInstance((OllamaDramaSettings) null);
    }

    /**
     * Creates a new session with strict protocol settings.
     */
    public static OllamaSession get_strict_protocol_session(String model_name) {
        return get_strict_protocol_session(
                model_name, 
                false, 
                false, 
                "You will get additional input soon, just reply with OKIDOKI for now.", 
                false);
    }

    public static OllamaSession get_strict_protocol_session(String model_name, 
                                                            boolean make_tools_available) {
        return get_strict_protocol_session(
                model_name, 
                false, 
                false,
                "You will get additional input soon, just reply with OKIDOKI for now.",
                make_tools_available);
    }

    public static OllamaSession get_strict_protocol_session(String model_name,
                                                            String initial_prompt,
                                                            boolean make_tools_available) {
        return get_strict_protocol_session(
                model_name, 
                false, 
                false, 
                initial_prompt,
                make_tools_available);
    }

    public static OllamaSession get_strict_protocol_session(String model_name,
                                                            boolean hide_llm_reply_if_uncertain,
                                                            boolean use_random_seed,
                                                            boolean make_tools_available) {
        return get_strict_protocol_session(
                model_name,
                hide_llm_reply_if_uncertain,
                use_random_seed,
                "You will get additional input soon, just reply with OKIDOKI for now.",
                make_tools_available);
    }

    public static OllamaSession get_strict_protocol_session(String model_name,
                                                            boolean hide_llm_reply_if_uncertain,
                                                            boolean use_random_seed) {
        return get_strict_protocol_session(
                model_name,
                hide_llm_reply_if_uncertain,
                use_random_seed,
                "You will get additional input soon, just reply with OKIDOKI for now.",
                false);
    }

    public static OllamaSession get_strict_protocol_session(String model_name,
                                                            boolean hide_llm_reply_if_uncertain,
                                                            boolean use_random_seed,
                                                            String initial_prompt,
                                                            boolean make_tools_available) {
        validate_model_name(model_name);
        validate_model_in_settings(model_name);

        String system_prompt = build_system_prompt(model_name, make_tools_available, initial_prompt);

        return new OllamaSession(
                model_name,
                get_random_active_ollama_url(),
                Globals.createStrictOptionsBuilder(model_name, use_random_seed, settings.getN_ctx_override()),
                settings,
                system_prompt,
                SessionType.STRICTPROTOCOL,
                make_tools_available);
    }

    private static String build_system_prompt(String model_name, 
                                             boolean make_tools_available,
                                             String initial_prompt) {
        StringBuilder prompt = new StringBuilder();

        if (model_name.startsWith("cogito")) {
            prompt.append(Globals.PROMPT_TEMPLATE_COGITO_DEEPTHINK);
        }

        prompt.append(Globals.PROMPT_TEMPLATE_STRICT_SIMPLEOUTPUT)
              .append(Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_STATEMENTS)
              .append(Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS)
              .append(Globals.THREAT_TEMPLATE)
              .append("\n\n")
              .append(initial_prompt)
              .append("\n\n");

        if (make_tools_available) {
            String tool_summary = get_all_available_mcp_tools();
            prompt.append(tool_summary).append("\n\n");
        } else {
            prompt.append("NO MCP TOOLS AVAILABLE.\n\n");
        }

        return prompt.toString();
    }

    /**
     * Creates a new creative session.
     */
    public static OllamaSession get_creative_session(String model_name, String initial_prompt) {
        validate_model_name(model_name);
        validate_model_in_settings(model_name);

        return new OllamaSession(
                model_name,
                get_random_active_ollama_url(),
                Globals.createCreativeOptionsBuilder(model_name, settings.getN_ctx_override()),
                settings,
                Globals.PROMPT_TEMPLATE_CREATIVE + "\n\n" + initial_prompt,
                SessionType.CREATIVE,
                false);
    }

    public static OllamaSession getCreativeSession(String model_name, String initial_prompt) {
        return get_creative_session(model_name, initial_prompt);
    }

    /**
     * Creates a new default session.
     */
    public static OllamaSession get_default_session(String model_name) {
        validate_model_name(model_name);
        validate_model_in_settings(model_name);

        return new OllamaSession(
                model_name,
                get_random_active_ollama_url(),
                Globals.createDefaultOptionsBuilder(),
                settings,
                "",
                SessionType.DEFAULT,
                false);
    }

    private static void validate_model_name(String model_name) {
        Objects.requireNonNull(settings, "OllamaDrama not properly initialized");
        
        if (model_name == null || model_name.length() <= MIN_MODEL_NAME_LENGTH) {
            LOGGER.error("Invalid model name: {}", model_name);
            SystemUtils.halt();
        }
    }

    private static void validate_model_in_settings(String model_name) {
        boolean model_exists = false;
        for (String existing_model : settings.getOllama_models().split(",")) {
            if (existing_model.equals(model_name)) {
                model_exists = true;
                break;
            }
        }

        if (!model_exists) {
            LOGGER.warn("Model {} not in settings. Available models: {}", 
                    model_name, settings.getOllama_models());
            SystemUtils.halt();
        }
    }

    /**
     * Gets a random active Ollama endpoint.
     * Blocks until at least one endpoint is available.
     * 
     * @return a random active Ollama endpoint
     */
    public static OllamaEndpoint get_random_active_ollama_url() {
        while (true) {
            lock.readLock().lock();
            try {
                int size = ollama_endpoints.size();
                
                if (size == 0) {
                    lock.readLock().unlock();
                    LOGGER.warn("No Ollama hosts available. Rescanning in 30 seconds...");
                    SystemUtils.sleepInSeconds((int) LONG_RETRY_DELAY.toSeconds());
                    lock.readLock().lock();
                    continue;
                }

                int selection = NumUtils.randomNumWithinRangeAsInt(1, size);
                int index = 1;
                
                for (var entry : ollama_endpoints.entrySet()) {
                    if (selection == index) {
                        return entry.getValue();
                    }
                    index++;
                }
            } finally {
                lock.readLock().unlock();
            }

            LOGGER.warn("Failed to select Ollama host. Retrying...");
            SystemUtils.sleepInSeconds((int) LONG_RETRY_DELAY.toSeconds());
        }
    }

    /**
     * Gets a summary of all available MCP tools.
     * 
     * @return formatted string containing all available tools
     */
    public static String get_all_available_mcp_tools() {
        Map<String, Boolean> unique_tools = new HashMap<>();
        StringBuilder sb = new StringBuilder("MCP TOOLS AVAILABLE:\n");

        lock.readLock().lock();
        try {
            for (var entry : mcp_tools.entrySet()) {
                MCPTool tool = entry.getValue();
                
                if (is_matching_mcp_tool(tool.getToolname(), settings.getFiltered_mcp_toolnames_csv())) {
                    continue; // Filtered out
                }
                
                if (!unique_tools.containsKey(tool.getToolname())) {
                    sb.append(tool.getTool_str()).append("\n");
                    unique_tools.put(tool.getToolname(), true);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return sb.toString().replaceAll("\n{3,}", "\n\n");
    }

    /**
     * Gets all MCP tools, excluding filtered ones.
     * 
     * @return map of tool ID to MCPTool
     */
    public static Map<String, MCPTool> get_mcp_tools() {
        Map<String, MCPTool> filtered_tools = new TreeMap<>();

        lock.readLock().lock();
        try {
            for (var entry : mcp_tools.entrySet()) {
                MCPTool tool = entry.getValue();
                
                if (!is_matching_mcp_tool(tool.getToolname(), settings.getFiltered_mcp_toolnames_csv())) {
                    filtered_tools.put(entry.getKey(), tool);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return filtered_tools;
    }

    /**
     * Gets the MCP tool for a specific tool name.
     * 
     * @param tool_name the name of the tool
     * @return the MCPTool, or null if not found
     */
    public static MCPTool get_mcp_url_for_tool(String tool_name) {
        lock.readLock().lock();
        try {
            for (var entry : mcp_tools.entrySet()) {
                MCPTool tool = entry.getValue();
                if (tool_name.equals(tool.getToolname())) {
                    return tool;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return null;
    }

    /**
     * Checks if a tool name matches any in the filtered list.
     * 
     * @param tool_name the tool name to check
     * @param mcp_tool_names comma-separated list of tool names to filter
     * @return true if the tool should be filtered
     */
    public static boolean is_matching_mcp_tool(String tool_name, String mcp_tool_names) {
        if (tool_name == null || mcp_tool_names == null) {
            return false;
        }

        for (String filtered_tool : mcp_tool_names.split(",")) {
            if (tool_name.equals(filtered_tool)) {
                return true;
            }
        }
        
        return false;
    }

    // Getters
    public static OllamaDramaSettings get_settings() {
        return settings;
    }

    public static List<String> get_service_cnets() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(service_cnets);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static Map<String, OllamaEndpoint> get_ollama_hosts() {
        lock.readLock().lock();
        try {
            return new TreeMap<>(ollama_endpoints);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static String get_models() {
        return settings.getOllama_models();
    }

    // Package-private for testing
    static void set_mcp_tools(Map<String, MCPTool> tools) {
        lock.writeLock().lock();
        try {
            mcp_tools.clear();
            mcp_tools.putAll(tools);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ========== BACKWARD COMPATIBILITY WRAPPERS (DEPRECATED) ==========

    public static boolean wireMCPs(boolean block_until_ready) {
        return wire_mcps(block_until_ready);
    }

    public static boolean wireOllama(boolean block_until_ready) {
        return wire_ollama(block_until_ready);
    }

    public static OllamaDramaSettings getSettings() {
        return get_settings();
    }

    public static List<String> getService_cnets() {
        return get_service_cnets();
    }

    public static Map<String, OllamaEndpoint> getollama_hosts() {
        return get_ollama_hosts();
    }

    public static OllamaEndpoint getRandomActiveOllamaURL() {
        return get_random_active_ollama_url();
    }

    public static OllamaSession getStrictProtocolSession(String model_name) {
        return get_strict_protocol_session(model_name);
    }

    public static OllamaSession getStrictProtocolSession(String model_name, boolean make_tools_available) {
        return get_strict_protocol_session(model_name, make_tools_available);
    }

    public static OllamaSession getStrictProtocolSession(String model_name, String initial_prompt, 
                                                          boolean make_tools_available) {
        return get_strict_protocol_session(model_name, initial_prompt, make_tools_available);
    }

    public static OllamaSession getStrictProtocolSession(String model_name, 
                                                          boolean hide_llm_reply_if_uncertain,
                                                          boolean use_random_seed,
                                                          boolean make_tools_available) {
        return get_strict_protocol_session(model_name, hide_llm_reply_if_uncertain, 
                                          use_random_seed, make_tools_available);
    }

    public static OllamaSession getStrictProtocolSession(String model_name,
                                                          boolean hide_llm_reply_if_uncertain,
                                                          boolean use_random_seed) {
        return get_strict_protocol_session(model_name, hide_llm_reply_if_uncertain, use_random_seed);
    }

    public static OllamaSession getStrictProtocolSession(String model_name,
                                                          boolean hide_llm_reply_if_uncertain,
                                                          boolean use_random_seed,
                                                          String initial_prompt,
                                                          boolean make_tools_available) {
        return get_strict_protocol_session(model_name, hide_llm_reply_if_uncertain, 
                                          use_random_seed, initial_prompt, make_tools_available);
    }

    public static OllamaSession getDefaultSession(String model_name) {
        return get_default_session(model_name);
    }

    public static String getAllAvailableMCPTools() {
        return get_all_available_mcp_tools();
    }

    public static Map<String, MCPTool> getMcp_tools() {
        return get_mcp_tools();
    }

    public static void setMcp_tools(Map<String, MCPTool> tools) {
        set_mcp_tools(tools);
    }

    public static MCPTool getMCPURLForTool(String tool_name) {
        return get_mcp_url_for_tool(tool_name);
    }

    public static boolean isMatchingMCPTool(String tool_name, String mcp_tool_names) {
        return is_matching_mcp_tool(tool_name, mcp_tool_names);
    }

    public static String getModels() {
        return get_models();
    }
}