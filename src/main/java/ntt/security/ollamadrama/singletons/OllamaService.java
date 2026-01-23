package ntt.security.ollamadrama.singletons;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.TreeMap;

import java.util.TreeSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
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
 * 
 * Ollama servers are fingerprinted using their list of available models to prevent
 * duplicate registrations when the same server is accessible via multiple IPs.

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
    
    // Tracks fingerprints of registered Ollama servers to prevent duplicates
    private static final Set<String> registered_ollama_fingerprints = new TreeSet<>();

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
     * Uses model list fingerprinting to prevent duplicate registrations
     * when the same server is accessible via multiple IPs.

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
        Set<String> seen_fingerprints = new TreeSet<>();

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

                validate_ollama_endpoints(candidates, abandoned_ollamas, verified_ollamas, seen_fingerprints);

                if (!verified_ollamas.isEmpty()) {

                    lock.writeLock().lock();

                    try {

                        ollama_endpoints.clear();

                        ollama_endpoints.putAll(verified_ollamas);
                        registered_ollama_fingerprints.clear();
                        registered_ollama_fingerprints.addAll(seen_fingerprints);

                    } finally {

                        lock.writeLock().unlock();

                    }

                    

                    found_ollamas = true;

                    if (block_until_ready) {

                        LOGGER.info("Verified Ollama endpoints: {}", verified_ollamas.keySet());
                        LOGGER.info("Unique Ollama servers (by fingerprint): {}", seen_fingerprints.size());

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

                                                 Map<String, OllamaEndpoint> verified_ollamas,
                                                 Set<String> seen_fingerprints) {

        for (var entry : candidates.entrySet()) {

            String url = entry.getKey();

            OllamaEndpoint endpoint = entry.getValue();

            if (abandoned_ollamas.containsKey(url)) {

                LOGGER.debug("Skipping abandoned endpoint: {}", url);

                continue;

            }

            if (validate_single_ollama_endpoint(endpoint, abandoned_ollamas, seen_fingerprints)) {

                verified_ollamas.put(url, endpoint);

            }

        }

    }

    /**
     * Generates a fingerprint hash for an Ollama server based on its available models.
     * The fingerprint is created by sorting model names alphabetically, joining them,
     * and computing a SHA-256 hash. This allows identification of the same server 
     * even when accessed via different IPs.
     * 
     * @param api the Ollama API connection
     * @return a fingerprint hash string, or null if models cannot be retrieved
     */
    private static String generate_ollama_fingerprint(Ollama api) {
        try {
            List<String> models = OllamaUtils.getModelsAvailable(api);
            if (models == null || models.isEmpty()) {
                return null;
            }
            // Sort models to ensure consistent fingerprint regardless of list order
            List<String> sorted_models = new ArrayList<>(models);
            sorted_models.sort(String::compareTo);
            String model_list = String.join("|", sorted_models);
            
            // Generate SHA-256 hash of the model list
            return compute_sha256_hash(model_list);
        } catch (Exception e) {
            LOGGER.debug("Failed to generate fingerprint: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Computes SHA-256 hash of the input string and returns it as a hex string.
     * 
     * @param input the string to hash
     * @return hex-encoded SHA-256 hash
     */
    private static String compute_sha256_hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash_bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex_string = new StringBuilder();
            for (byte b : hash_bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hex_string.append('0');
                }
                hex_string.append(hex);
            }
            return hex_string.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available, but fallback to simple hash just in case
            LOGGER.warn("SHA-256 not available, using simple hash");
            return Integer.toHexString(input.hashCode());
        }
    }

    private static boolean validate_single_ollama_endpoint(OllamaEndpoint endpoint,
                                                          Map<String, OllamaEndpoint> abandoned_ollamas,
                                                          Set<String> seen_fingerprints) {

        String url = endpoint.getOllama_url();

        Ollama api = OllamaUtils.createConnection(endpoint, settings.getOllama_timeout());

        try {

            if (!api.ping()) {

                LOGGER.debug("Ping failed for: {}", url);

                return false;

            }

            LOGGER.info("Ollama endpoint {} responded to ping", url);
            
            // Generate fingerprint based on available models - do this BEFORE any sanity checks
            List<String> available_models = OllamaUtils.getModelsAvailable(api);
            LOGGER.info("Available models on {}: {}", url, available_models);
            
            String fingerprint = generate_ollama_fingerprint(api);
            if (fingerprint == null) {
                LOGGER.warn("Could not generate fingerprint for {}, skipping", url);
                return false;
            }
            
            LOGGER.info("Ollama server fingerprint for {}: {}", url, fingerprint);
            
            // Check if we've already seen this fingerprint (duplicate server on different IP)
            if (seen_fingerprints.contains(fingerprint)) {
                LOGGER.info("Skipping duplicate Ollama server at {} (fingerprint {} already registered)", url, fingerprint);
                return false;
            }
            
            // Register fingerprint BEFORE running sanity checks
            // This prevents duplicate validation attempts on the same physical server
            seen_fingerprints.add(fingerprint);
            LOGGER.debug("Registered fingerprint {} for endpoint {}", fingerprint, url);

            if (settings.isOllama_skip_paris_validation()) {

                LOGGER.info("Quick boot mode: skipping model validation for {}", url);

                return true;

            }

            boolean valid = validate_required_models(api, endpoint, abandoned_ollamas);
            if (!valid) {
                // Remove fingerprint if validation fails so we can retry on a different IP
                seen_fingerprints.remove(fingerprint);
                LOGGER.debug("Removed fingerprint {} due to validation failure", fingerprint);
            }
            return valid;

            

        } catch (Exception e) {

            LOGGER.warn("Exception while validating {}: {}", url, e.getMessage());

            return false;

        }

    }

    private static boolean validate_required_models(Ollama api,

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

    private static boolean validate_model(Ollama api,
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
        
        // This was just for validation, lets suggest to unload the model
        try {
        	LOGGER.info("Suggested unload of model " + model_name);
			api.unloadModel(model_name);
		} catch (OllamaException e) {
			// silent
		}

        return true;

    }

    private static boolean handle_missing_model(Ollama api,

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

        int seed = NumUtils.randomNumWithinRangeAsInt(1, 1000000);
        LOGGER.info("Session seed generated as " + seed);
        
        return new OllamaSession(

                model_name,

                get_random_active_ollama_url(),

                Globals.createCreativeOptionsBuilder(model_name, settings.getN_ctx_override(), seed),

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

                	System.out.println("comparing " + tool_name + " against " + tool.getToolname());

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
    
    /**
     * Gets the number of unique Ollama servers registered (by fingerprint).
     * 
     * @return count of unique Ollama server fingerprints
     */
    public static int get_unique_ollama_server_count() {
        lock.readLock().lock();
        try {
            return registered_ollama_fingerprints.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the registered Ollama fingerprints (for debugging/monitoring).
     * 
     * @return set of fingerprint strings
     */
    public static Set<String> get_registered_ollama_fingerprints() {
        lock.readLock().lock();
        try {
            return new TreeSet<>(registered_ollama_fingerprints);
        } finally {
            lock.readLock().unlock();
        }
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

    
    /**
     * Destroys the singleton instance and clears all state.
     * This allows the singleton to be re-initialized with fresh settings.
     * 
     * WARNING: This method is not thread-safe with concurrent getInstance() calls.
     * Ensure no other threads are using the service during reset.
     */
    public static void destroyInstance() {
        lock.writeLock().lock();
        try {
            // Clear all static state
            ollama_endpoints.clear();
            mcp_tools.clear();
            registered_ollama_fingerprints.clear();
            service_cnets.clear();
            
            // Reset settings to default
            settings = new OllamaDramaSettings();
            
            // Destroy the singleton instance
            single_instance = null;
            
            LOGGER.info("OllamaService singleton destroyed and state cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Resets the singleton with new settings.
     * Combines destroy and re-initialization in a single synchronized operation.
     * 
     * @param new_settings the new settings to use, or null for environment-based settings
     * @return the newly initialized singleton instance
     */
    public static OllamaService resetInstance(OllamaDramaSettings new_settings) {
        synchronized (OllamaService.class) {
            destroyInstance();
            return getInstance(new_settings);
        }
    }

    // Backward compatibility wrapper
    public static void destroy() {
        destroyInstance();
    }

    public static OllamaService reset(OllamaDramaSettings new_settings) {
        return resetInstance(new_settings);
    }
}