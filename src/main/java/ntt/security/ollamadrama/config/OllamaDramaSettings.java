package ntt.security.ollamadrama.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.objects.MCPEndpoint;
import ntt.security.ollamadrama.objects.OllamaEndpoint;

@SuppressWarnings("serial")
public class OllamaDramaSettings {

    private String release = "Strawberrry";
	
	private String ollama_username = "";
	private String ollama_password = "";
	private String ollama_models = Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_MAXCONTEXT_L;
	private Integer ollama_port = 11434;
	private long ollama_timeout = 1200L; // 20 min
	private boolean ollama_scan = true;
	private int n_ctx_override = -1;
	
	private ArrayList<Integer> mcp_ports = new ArrayList<Integer>() {{
		this.add(8080);
		this.add(9000);
	}};
	private ArrayList<String> mcp_sse_paths = new ArrayList<String>() {{
		this.add("/sse");
	}};
	private boolean mcp_scan = true;
	private boolean mcp_blind_trust = false;
	
	private Integer threadPoolCount = 20;
	
	private String openaikey = "";
	private boolean use_openai = false;
	
	private String elevenlabs_apikey = "";
	private String elevenlabs_voice1 = "";
	private String elevenlabs_voice2 = "";
	
	private ArrayList<OllamaEndpoint> satellites;
	private ArrayList<MCPEndpoint> mcp_satellites;
	
	private String autopull_max_llm_size = "XL"; // S, M, XL, XXL
	
	public OllamaDramaSettings() {
		super();
	}

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
		this.ollama_port = ollama_port;
	}

	public void sanityCheck() {
	}

	public Integer getThreadPoolCount() {
		return threadPoolCount;
	}

	public void setThreadPoolCount(Integer threadPoolCount) {
		this.threadPoolCount = threadPoolCount;
	}

	public String getOllama_models() {
		return ollama_models;
	}

	public void setOllama_models(String ollama_models) {
		if (ollama_models != null && !ollama_models.isEmpty()) {
			String[] parts = ollama_models.split(",");
			Set<String> uniqueModels = new LinkedHashSet<>();
			for (String part : parts) {
				String trimmed = part.trim();
				if (!trimmed.isEmpty()) {
					uniqueModels.add(trimmed);
				}
			}
			this.ollama_models = String.join(",", uniqueModels);
		} else {
			this.ollama_models = "";
		}
	}


	public ArrayList<OllamaEndpoint> getSatellites() {
		return satellites;
	}

	public void setSatellites(ArrayList<OllamaEndpoint> satellites) {
		this.satellites = satellites;
	}

	public void addOllamaCustomEndpoint(OllamaEndpoint ep) {
		if (null == this.satellites) this.satellites = new ArrayList<OllamaEndpoint>();
		this.satellites.add(ep);
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
		this.ollama_timeout = ollama_timeout;
	}

	public ArrayList<Integer> getMcp_ports() {
		return mcp_ports;
	}

	public void setMcp_ports(ArrayList<Integer> mcp_ports) {
		this.mcp_ports = mcp_ports;
	}

	public ArrayList<MCPEndpoint> getMcp_satellites() {
		return mcp_satellites;
	}

	public void setMcp_satellites(ArrayList<MCPEndpoint> mcp_satellites) {
		this.mcp_satellites = mcp_satellites;
	}

	public ArrayList<String> getMcp_sse_paths() {
		return mcp_sse_paths;
	}

	public void setMcp_sse_paths(ArrayList<String> mcp_sse_paths) {
		this.mcp_sse_paths = mcp_sse_paths;
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

}