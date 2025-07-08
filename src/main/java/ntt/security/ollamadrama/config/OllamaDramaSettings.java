package ntt.security.ollamadrama.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.objects.OllamaEndpoint;

public class OllamaDramaSettings {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaDramaSettings.class);
	
    private String release = "Strawberrry";
	
	private String ollama_username = "";
	private String ollama_password = "";
	private String ollama_models = Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_MINIDIVERSE_M;
	private Integer ollama_port = 11434;
	private Integer threadPoolCount = 20;
	private long ollama_timeout = 1200L; // 20 min
	private boolean ollama_scan = true;
	
	private String openaikey = "";
	private boolean use_openai = false;
	
	private String elevenlabs_apikey = "";
	private String elevenlabs_voice1 = "";
	private String elevenlabs_voice2 = "";
	
	private ArrayList<OllamaEndpoint> satellites;
	
	private String autopull_max_llm_size = "L"; // S, M, XL, XXL
	
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

}