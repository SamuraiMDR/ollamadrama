package ntt.security.ollamadrama.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.agent.AppSettings;
import ntt.security.ollamadrama.enums.InteractMethod;
import ntt.security.ollamadrama.objects.MCPEndpoint;
import ntt.security.ollamadrama.objects.OllamaEndpoint;

/**
 * Configuration settings for OllamaDrama application.
 * Manages Ollama endpoints, MCP (Model Context Protocol) endpoints, and various API configurations.
 */
public class OllamaDramaSettings {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaDramaSettings.class);
	
	// Constants
	private static final int DEFAULT_OLLAMA_PORT = 11434;
	private static final long DEFAULT_OLLAMA_TIMEOUT = 240; // 2 min
	private static final int DEFAULT_THREAD_POOL_COUNT = 20;
	private static final int MAX_PORT_NUMBER = 65535;
	private static final int MIN_PORT_NUMBER = 1;

	// Ollama configuration
	private String ollama_username = "";
	private String ollama_password = "";
	private String ollama_models = Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_MAXCONTEXT_L;
	private Integer ollama_port = DEFAULT_OLLAMA_PORT;
	private long ollama_timeout = DEFAULT_OLLAMA_TIMEOUT;
	private boolean ollama_scan = true;
	private boolean ollama_skip_paris_validation = false;
	private int n_ctx_override = -1;
	private float temperature_override = -1f; // leave for no override
	private String orchestrator_url = null; // http://127.0.0.1:1111/api/status
	
	// MCP configuration
	private List<Integer> mcp_ports = new ArrayList<>(Arrays.asList(8000, 8080, 9000));
	private List<String> mcp_sse_paths = new ArrayList<>(Collections.singletonList("/sse"));
	private boolean mcp_scan = false;
	private boolean mcp_blind_trust = false;
	private boolean mcp_enable_promptinject_protection = true;
	private String trusted_mcp_toolnames_csv = "";
	private String filtered_mcp_toolnames_csv = "";
	private Map<String, Function<String, String>> mcp_preprocess = new HashMap<>();
	
	// Thread pool configuration
	private Integer threadPoolCount = DEFAULT_THREAD_POOL_COUNT;
	
	// API keys
	private String openaikey = "";
	private boolean use_openai = false;
	private String claudekey = "";
	private boolean use_claude = false;
	private String xaikey = "";
	private boolean use_xai = false;
	
	// Endpoints
	private List<OllamaEndpoint> satellites = new ArrayList<>();
	private List<MCPEndpoint> mcp_satellites = new ArrayList<>();
	
	// Autopull configuration
	private String autopull_max_llm_size = "XL"; // S, M, XL, XXL
	
	// Interact/Control
	private InteractMethod interact_method = InteractMethod.STDIN; // FILE
	private String interact_filepath = "/interact";
	
	// Voice
	private boolean qwen3tts_enable = false;
	private String qwen3tts_url = "";
	private String qwen3tts_voice = "";
	
	// Agent
	private int rounds_per_pass = 2;
	
	public OllamaDramaSettings() {
		// Default constructor
	}

	// Getters and Setters

	public String getOllama_username() {
		return ollama_username;
	}

	public void setOllama_username(String ollama_username) {
		this.ollama_username = ollama_username;
	}

	public String getOllama_password() {
		return ollama_password;
	}

	public void setOllama_password(String ollama_password) {
		this.ollama_password = ollama_password;
	}

	public Integer getOllama_port() {
		return ollama_port;
	}

	public void setOllama_port(Integer ollama_port) {
		if (ollama_port != null && (ollama_port < MIN_PORT_NUMBER || ollama_port > MAX_PORT_NUMBER)) {
			LOGGER.warn("Invalid Ollama port {}. Using default port {}", ollama_port, DEFAULT_OLLAMA_PORT);
			this.ollama_port = DEFAULT_OLLAMA_PORT;
		} else {
			this.ollama_port = ollama_port;
		}
	}

	/**
	 * Performs sanity checks on the configuration.
	 * Ensures model names are unique and properly formatted.
	 */
	public void sanityCheck() {
		// Deduplicate and clean model names
		Set<String> uniqueModels = Arrays.stream(this.getOllama_models().split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toCollection(LinkedHashSet::new));
		
		this.setOllama_models(String.join(",", uniqueModels));
		LOGGER.info("Deduced models string: {}", this.getOllama_models());
	}

	public Integer getThreadPoolCount() {
		return threadPoolCount;
	}

	public void setThreadPoolCount(Integer threadPoolCount) {
		if (threadPoolCount != null && threadPoolCount <= 0) {
			LOGGER.warn("Invalid thread pool count {}. Using default {}", threadPoolCount, DEFAULT_THREAD_POOL_COUNT);
			this.threadPoolCount = DEFAULT_THREAD_POOL_COUNT;
		} else {
			this.threadPoolCount = threadPoolCount;
		}
	}

	public String getOllama_models() {
		return ollama_models;
	}

	public void setOllama_models(String _ollama_models) {
		if (_ollama_models != null && !_ollama_models.isEmpty()) {
			Set<String> uniqueModels = Arrays.stream(_ollama_models.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(LinkedHashSet::new));
			
			this.ollama_models = String.join(",", uniqueModels);
		} else {
			this.ollama_models = "";
		}
	}

	public List<OllamaEndpoint> getSatellites() {
		return Collections.unmodifiableList(satellites);
	}

	public void setSatellites(List<OllamaEndpoint> satellites) {
		this.satellites = satellites != null ? new ArrayList<>(satellites) : new ArrayList<>();
	}

	public void addOllamaCustomEndpoint(OllamaEndpoint ep) {
		if (ep != null) {
			this.satellites.add(ep);
		} else {
			LOGGER.warn("Attempted to add null OllamaEndpoint");
		}
	}

	public String getOpenaikey() {
		return openaikey;
	}

	public void setOpenaikey(String openaikey) {
		this.openaikey = openaikey;
	}

	public boolean isOllama_scan() {
		return ollama_scan;
	}

	public void setOllama_scan(boolean ollama_scan) {
		this.ollama_scan = ollama_scan;
	}

	public boolean isUse_openai() {
		return use_openai;
	}

	public void setUse_openai(boolean use_openai) {
		this.use_openai = use_openai;
	}

	public String getClaudekey() {
		return claudekey;
	}

	public void setClaudekey(String claudekey) {
		this.claudekey = claudekey;
	}

	public boolean isUse_claude() {
		return use_claude;
	}

	public void setUse_claude(boolean use_claude) {
		this.use_claude = use_claude;
	}

	public String getXaikey() {
		return xaikey;
	}

	public void setXaikey(String xaikey) {
		this.xaikey = xaikey;
	}

	public boolean isUse_xai() {
		return use_xai;
	}

	public void setUse_xai(boolean use_xai) {
		this.use_xai = use_xai;
	}

	public String getAutopull_max_llm_size() {
		return autopull_max_llm_size;
	}

	public void setAutopull_max_llm_size(String autopull_max_llm_size) {
		this.autopull_max_llm_size = autopull_max_llm_size;
	}

	public long getOllama_timeout() {
		return ollama_timeout;
	}

	public void setOllama_timeout(long ollama_timeout) {
		if (ollama_timeout <= 0) {
			LOGGER.warn("Invalid timeout {}. Using default {}", ollama_timeout, DEFAULT_OLLAMA_TIMEOUT);
			this.ollama_timeout = DEFAULT_OLLAMA_TIMEOUT;
		} else {
			this.ollama_timeout = ollama_timeout;
		}
	}

	public List<Integer> getMcp_ports() {
		return Collections.unmodifiableList(mcp_ports);
	}

	/**
	 * Sets MCP ports from a comma-separated string.
	 * Validates that ports are within valid range (1-65535).
	 * 
	 * @param csv comma-separated port numbers
	 */
	public void setMcp_ports_csv(String csv) {
		List<Integer> validPorts = Arrays.stream(csv.split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(s -> {
				try {
					return Integer.parseInt(s);
				} catch (NumberFormatException e) {
					LOGGER.warn("Invalid MCP port specified: {}", s);
					return null;
				}
			})
			.filter(port -> port != null && port >= MIN_PORT_NUMBER && port <= MAX_PORT_NUMBER)
			.collect(Collectors.toList());
		
		this.mcp_ports = validPorts;
	}
	
	public void setMcp_ports(List<Integer> mcp_ports) {
		this.mcp_ports = mcp_ports != null ? new ArrayList<>(mcp_ports) : new ArrayList<>();
	}

	public List<MCPEndpoint> getMcp_satellites() {
		return Collections.unmodifiableList(mcp_satellites);
	}

	public void setMcp_satellites(List<MCPEndpoint> mcp_satellites) {
		this.mcp_satellites = mcp_satellites != null ? new ArrayList<>(mcp_satellites) : new ArrayList<>();
	}

	public List<String> getMcp_sse_paths() {
		return Collections.unmodifiableList(mcp_sse_paths);
	}

	public void setMcp_sse_paths(List<String> mcp_sse_paths) {
		this.mcp_sse_paths = mcp_sse_paths != null ? new ArrayList<>(mcp_sse_paths) : new ArrayList<>();
	}

	public boolean isMcp_scan() {
		return mcp_scan;
	}

	public void setMcp_scan(boolean mcp_scan) {
		this.mcp_scan = mcp_scan;
	}

	public boolean isMcp_blind_trust() {
		return mcp_blind_trust;
	}

	public void setMcp_blind_trust(boolean mcp_blind_trust) {
		this.mcp_blind_trust = mcp_blind_trust;
	}

	public int getN_ctx_override() {
		return n_ctx_override;
	}

	public void setN_ctx_override(int n_ctx_override) {
		this.n_ctx_override = n_ctx_override;
	}

	public String getTrusted_mcp_toolnames_csv() {
		return trusted_mcp_toolnames_csv;
	}

	public void setTrusted_mcp_toolnames_csv(String trusted_mcp_toolnames_csv) {
		this.trusted_mcp_toolnames_csv = trusted_mcp_toolnames_csv;
	}

	public String getFiltered_mcp_toolnames_csv() {
		return filtered_mcp_toolnames_csv;
	}

	public void setFiltered_mcp_toolnames_csv(String filtered_mcp_toolnames_csv) {
		this.filtered_mcp_toolnames_csv = filtered_mcp_toolnames_csv;
	}

	public boolean isOllama_skip_paris_validation() {
		return ollama_skip_paris_validation;
	}

	public void setOllama_skip_paris_validation(boolean ollama_skip_paris_validation) {
		this.ollama_skip_paris_validation = ollama_skip_paris_validation;
	}

	public Float getTemperature_override() {
		return temperature_override;
	}

	public void setTemperature_override(Float temperature_override) {
		this.temperature_override = temperature_override;
	}

	public String getOrchestrator_url() {
		return orchestrator_url;
	}

	public void setOrchestrator_url(String orchestrator_url) {
		this.orchestrator_url = orchestrator_url;
	}

	public void setTemperature_override(float temperature_override) {
		this.temperature_override = temperature_override;
	}

	public boolean isMcp_enable_promptinject_protection() {
		return mcp_enable_promptinject_protection;
	}

	public void setMcp_enable_promptinject_protection(boolean mcp_enable_promptinject_protection) {
		this.mcp_enable_promptinject_protection = mcp_enable_promptinject_protection;
	}

	public Map<String, Function<String, String>> getMcp_preprocess() {
		return mcp_preprocess;
	}

	public void setMcp_preprocess(Map<String, Function<String, String>> mcp_preprocess) {
		this.mcp_preprocess = mcp_preprocess;
	}

	public InteractMethod getInteract_method() {
		return interact_method;
	}

	public void setInteract_method(InteractMethod interact_method) {
		this.interact_method = interact_method;
	}

	public String getInteract_filepath() {
		return interact_filepath;
	}

	public void setInteract_filepath(String interact_filepath) {
		this.interact_filepath = interact_filepath;
	}

	public boolean isQwen3tts_enable() {
		return qwen3tts_enable;
	}

	public void setQwen3tts_enable(boolean qwen3tts_enable) {
		this.qwen3tts_enable = qwen3tts_enable;
	}

	public String getQwen3tts_url() {
		return qwen3tts_url;
	}

	public void setQwen3tts_url(String qwen3tts_url) {
		this.qwen3tts_url = qwen3tts_url;
	}

	public String getQwen3tts_voice() {
		return qwen3tts_voice;
	}

	public void setQwen3tts_voice(String qwen3tts_voice) {
		this.qwen3tts_voice = qwen3tts_voice;
	}

	public int getRounds_per_pass() {
		return rounds_per_pass;
	}

	public void setRounds_per_pass(int rounds_per_pass) {
		this.rounds_per_pass = rounds_per_pass;
	}
	
	
	/**
	 * Copies all fields from an AppSettings instance into this settings object.
	 * Sentinel-guarded fields (num_ctx == 0, temperature_override == -1f) are skipped
	 * so values already pulled in by parseOllamaDramaConfigENV() are preserved unless
	 * the caller explicitly overrode them on AppSettings.
	 */
	public void updateWithAppSettings(AppSettings appsettings) {
		if (appsettings == null) {
			LOGGER.warn("__updateWithAppSettings__ called with null AppSettings; skipping");
			return;
		}

		// Ollama credentials
		this.setOllama_username(appsettings.getOllama_username());
		this.setOllama_password(appsettings.getOllama_password());

		// Ollama endpoint (forced_endpoint becomes the sole satellite if set)
		if (appsettings.getForced_endpoint() != null && !appsettings.getForced_endpoint().isEmpty()) {
			this.setSatellites(new ArrayList<>(Arrays.asList(
				new OllamaEndpoint(
					appsettings.getForced_endpoint(),
					appsettings.getOllama_username(),
					appsettings.getOllama_password()
				)
			)));
		}

		// Model selection: prefer explicit CSV list, fall back to single selected_model
		if (appsettings.getOllama_models() != null && !appsettings.getOllama_models().isEmpty()) {
			this.setOllama_models(appsettings.getOllama_models());
		} else if (appsettings.getSelected_model() != null && !appsettings.getSelected_model().isEmpty()) {
			this.setOllama_models(appsettings.getSelected_model());
		}

		this.setOllama_port(appsettings.getOllama_port());
		this.setOllama_timeout(appsettings.getOllama_timeout());
		this.setOllama_scan(appsettings.isOllama_scan());
		this.setOllama_skip_paris_validation(appsettings.isOllama_skip_paris_validation());
		this.setOrchestrator_url(appsettings.getOrchestrator_url());
		this.setThreadPoolCount(appsettings.getThread_pool_count());
		this.setAutopull_max_llm_size(appsettings.getAutopull_max_llm_size());

		// Context override (sentinel: 0 means "don't override")
		if (0 != appsettings.getNum_ctx()) {
			this.setN_ctx_override(appsettings.getNum_ctx());
		}

		// Temperature override (sentinel: -1f means "don't override")
		if (-1f != appsettings.getTemperature_override()) {
			this.setTemperature_override(appsettings.getTemperature_override());
		}

		// MCP
		this.setMcp_ports_csv(appsettings.getMcp_ports());
		this.setMcp_sse_paths(appsettings.getMcp_sse_paths());
		this.setMcp_scan(appsettings.isMcp_scan());
		this.setMcp_blind_trust(appsettings.isMcp_blind_trust());
		this.setMcp_enable_promptinject_protection(appsettings.isMcp_enable_promptinject_protection());
		this.setFiltered_mcp_toolnames_csv(appsettings.getFiltered_mcp_toolnames_csv());
		this.setTrusted_mcp_toolnames_csv(appsettings.getTrusted_mcp_toolnames_csv());
		this.setMcp_preprocess(appsettings.getMcp_preprocess());

		// External API keys
		this.setOpenaikey(appsettings.getOpenaikey());
		this.setUse_openai(appsettings.isUse_openai());
		this.setClaudekey(appsettings.getClaudekey());
		this.setUse_claude(appsettings.isUse_claude());
		this.setXaikey(appsettings.getXaikey());
		this.setUse_xai(appsettings.isUse_xai());

		// Interact
		this.setInteract_method(appsettings.getInteract_method());
		this.setInteract_filepath(appsettings.getInteract_filepath());

		// Qwen3 TTS
		this.setQwen3tts_url(appsettings.getQwen3tts_url());
		this.setQwen3tts_voice(appsettings.getQwen3tts_voice());
		this.setQwen3tts_enable(appsettings.isQwen3tts_enable());

		// Agent
		this.setRounds_per_pass(appsettings.getRounds_per_pass());
	}
}