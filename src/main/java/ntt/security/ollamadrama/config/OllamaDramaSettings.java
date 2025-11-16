package ntt.security.ollamadrama.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final long DEFAULT_OLLAMA_TIMEOUT = 1200L; // 20 minutes in seconds
	private static final int DEFAULT_THREAD_POOL_COUNT = 20;
	private static final int MAX_PORT_NUMBER = 65535;
	private static final int MIN_PORT_NUMBER = 1;
	
	// Release info
	private String release = "Strawberrry";
	
	// Ollama configuration
	private String ollama_username = "";
	private String ollama_password = "";
	private String ollama_models = Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_MAXCONTEXT_L;
	private Integer ollama_port = DEFAULT_OLLAMA_PORT;
	private long ollama_timeout = DEFAULT_OLLAMA_TIMEOUT;
	private boolean ollama_scan = true;
	private boolean ollama_skip_paris_validation = false;
	private int n_ctx_override = -1;
	
	// MCP configuration
	private List<Integer> mcp_ports = new ArrayList<>(Arrays.asList(8000, 8080, 9000));
	private List<String> mcp_sse_paths = new ArrayList<>(Collections.singletonList("/sse"));
	private boolean mcp_scan = false;
	private boolean mcp_blind_trust = false;
	private String trusted_mcp_toolnames_csv = "";
	private String filtered_mcp_toolnames_csv = "";
	
	// Thread pool configuration
	private Integer threadPoolCount = DEFAULT_THREAD_POOL_COUNT;
	
	// API keys
	private String openaikey = "";
	private boolean use_openai = false;
	
	private String elevenlabs_apikey = "";
	private String elevenlabs_voice1 = "";
	private String elevenlabs_voice2 = "";
	
	// Endpoints
	private List<OllamaEndpoint> satellites = new ArrayList<>();
	private List<MCPEndpoint> mcp_satellites = new ArrayList<>();
	
	// Autopull configuration
	private String autopull_max_llm_size = "XL"; // S, M, XL, XXL
	
	public OllamaDramaSettings() {
		// Default constructor
	}

	// Getters and Setters
	
	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

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

	public String getElevenlabs_apikey() {
		return elevenlabs_apikey;
	}

	public void setElevenlabs_apikey(String elevenlabs_apikey) {
		this.elevenlabs_apikey = elevenlabs_apikey;
	}

	public String getElevenlabs_voice1() {
		return elevenlabs_voice1;
	}

	public void setElevenlabs_voice1(String elevenlabs_voice1) {
		this.elevenlabs_voice1 = elevenlabs_voice1;
	}

	public String getElevenlabs_voice2() {
		return elevenlabs_voice2;
	}

	public void setElevenlabs_voice2(String elevenlabs_voice2) {
		this.elevenlabs_voice2 = elevenlabs_voice2;
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
}