package ntt.security.ollamadrama.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import ntt.security.ollamadrama.enums.InteractMethod;

public class AppSettings {

	// --- Agent / session ---
	private String selected_model = "";
	private int max_retries = 1;
	private boolean unload_model_after_query = true;
	private boolean return_toolcall = false;
	private boolean halt_on_tool_error = false;
	private int toolcall_pausetime_in_seconds = 10;
	private boolean hide_llm_reply_if_uncertain = false;
	private int max_recursive_toolcall_depth = 10;
	private int num_ctx = 0;                          // 0 = no override
	private int session_tokens_maxlen = 14000;
	private boolean make_mcp_tools_available = true;
	private boolean use_random_seed = true;
	private boolean debug = false;
	private String forced_endpoint;
	private String initial_prompt = "";
	private String recursive_question = "";
	private String taskfolder = "tasks";
	private String taskstatefile = "task_state.json";
	private String persona = "";
	private float temperature_override = -1f;         // -1 = no override
	private boolean prompt_logging = false;
	private int rounds_per_pass = 2;

	// --- Ollama ---
	private String ollama_username = "";
	private String ollama_password = "";
	private String ollama_models = "";
	private int ollama_port = 11434;
	private long ollama_timeout = 240;
	private boolean ollama_scan = true;
	private boolean ollama_skip_paris_validation = false;
	private String orchestrator_url = null;
	private int thread_pool_count = 20;
	private String autopull_max_llm_size = "XL";

	// --- MCP ---
	private String mcp_ports = "";
	private List<String> mcp_sse_paths = new ArrayList<>(Collections.singletonList("/sse"));
	private boolean mcp_scan = false;
	private boolean mcp_blind_trust = false;
	private boolean mcp_enable_promptinject_protection = true;
	private String filtered_mcp_toolnames_csv = "";
	private String trusted_mcp_toolnames_csv = "";
	private Map<String, Function<String, String>> mcp_preprocess = new HashMap<>();

	// --- External API keys ---
	private String openaikey = "";
	private boolean use_openai = false;
	private String claudekey = "";
	private boolean use_claude = false;
	private String xaikey = "";
	private boolean use_xai = false;

	// --- Interact / control ---
	private InteractMethod interact_method = InteractMethod.STDIN;
	private String interact_filepath = "/interact";

	// --- Voice ---
	private boolean qwen3tts_enable = false;
	private String qwen3tts_url = "";
	private String qwen3tts_voice = "";

	public AppSettings() {
		super();
	}

	// --- Getters / setters ---

	public String getSelected_model() { return selected_model; }
	public void setSelected_model(String selected_model) { this.selected_model = selected_model; }

	public int getMax_retries() { return max_retries; }
	public void setMax_retries(int max_retries) { this.max_retries = max_retries; }

	public boolean isUnload_model_after_query() { return unload_model_after_query; }
	public void setUnload_model_after_query(boolean unload_model_after_query) { this.unload_model_after_query = unload_model_after_query; }

	public boolean isReturn_toolcall() { return return_toolcall; }
	public void setReturn_toolcall(boolean return_toolcall) { this.return_toolcall = return_toolcall; }

	public boolean isHalt_on_tool_error() { return halt_on_tool_error; }
	public void setHalt_on_tool_error(boolean halt_on_tool_error) { this.halt_on_tool_error = halt_on_tool_error; }

	public int getToolcall_pausetime_in_seconds() { return toolcall_pausetime_in_seconds; }
	public void setToolcall_pausetime_in_seconds(int toolcall_pausetime_in_seconds) { this.toolcall_pausetime_in_seconds = toolcall_pausetime_in_seconds; }

	public boolean isHide_llm_reply_if_uncertain() { return hide_llm_reply_if_uncertain; }
	public void setHide_llm_reply_if_uncertain(boolean hide_llm_reply_if_uncertain) { this.hide_llm_reply_if_uncertain = hide_llm_reply_if_uncertain; }

	public int getMax_recursive_toolcall_depth() { return max_recursive_toolcall_depth; }
	public void setMax_recursive_toolcall_depth(int max_recursive_toolcall_depth) { this.max_recursive_toolcall_depth = max_recursive_toolcall_depth; }

	public int getNum_ctx() { return num_ctx; }
	public void setNum_ctx(int num_ctx) { this.num_ctx = num_ctx; }

	public int getSession_tokens_maxlen() { return session_tokens_maxlen; }
	public void setSession_tokens_maxlen(int session_tokens_maxlen) { this.session_tokens_maxlen = session_tokens_maxlen; }

	public boolean isMake_mcp_tools_available() { return make_mcp_tools_available; }
	public void setMake_mcp_tools_available(boolean make_mcp_tools_available) { this.make_mcp_tools_available = make_mcp_tools_available; }

	public boolean isUse_random_seed() { return use_random_seed; }
	public void setUse_random_seed(boolean use_random_seed) { this.use_random_seed = use_random_seed; }

	public boolean isDebug() { return debug; }
	public void setDebug(boolean debug) { this.debug = debug; }

	public String getForced_endpoint() { return forced_endpoint; }
	public void setForced_endpoint(String forced_endpoint) { this.forced_endpoint = forced_endpoint; }

	public String getInitial_prompt() { return initial_prompt; }
	public void setInitial_prompt(String initial_prompt) { this.initial_prompt = initial_prompt; }

	public String getRecursive_question() { return recursive_question; }
	public void setRecursive_question(String recursive_question) { this.recursive_question = recursive_question; }

	public String getTaskfolder() { return taskfolder; }
	public void setTaskfolder(String taskfolder) { this.taskfolder = taskfolder; }

	public String getTaskstatefile() { return taskstatefile; }
	public void setTaskstatefile(String taskstatefile) { this.taskstatefile = taskstatefile; }

	public String getPersona() { return persona; }
	public void setPersona(String persona) { this.persona = persona; }

	public float getTemperature_override() { return temperature_override; }
	public void setTemperature_override(float temperature_override) { this.temperature_override = temperature_override; }

	public boolean isPrompt_logging() { return prompt_logging; }
	public void setPrompt_logging(boolean prompt_logging) { this.prompt_logging = prompt_logging; }

	public int getRounds_per_pass() { return rounds_per_pass; }
	public void setRounds_per_pass(int rounds_per_pass) { this.rounds_per_pass = rounds_per_pass; }

	public String getOllama_username() { return ollama_username; }
	public void setOllama_username(String ollama_username) { this.ollama_username = ollama_username; }

	public String getOllama_password() { return ollama_password; }
	public void setOllama_password(String ollama_password) { this.ollama_password = ollama_password; }

	public String getOllama_models() { return ollama_models; }
	public void setOllama_models(String ollama_models) { this.ollama_models = ollama_models; }

	public int getOllama_port() { return ollama_port; }
	public void setOllama_port(int ollama_port) { this.ollama_port = ollama_port; }

	public long getOllama_timeout() { return ollama_timeout; }
	public void setOllama_timeout(long ollama_timeout) { this.ollama_timeout = ollama_timeout; }

	public boolean isOllama_scan() { return ollama_scan; }
	public void setOllama_scan(boolean ollama_scan) { this.ollama_scan = ollama_scan; }

	public boolean isOllama_skip_paris_validation() { return ollama_skip_paris_validation; }
	public void setOllama_skip_paris_validation(boolean ollama_skip_paris_validation) { this.ollama_skip_paris_validation = ollama_skip_paris_validation; }

	public String getOrchestrator_url() { return orchestrator_url; }
	public void setOrchestrator_url(String orchestrator_url) { this.orchestrator_url = orchestrator_url; }

	public int getThread_pool_count() { return thread_pool_count; }
	public void setThread_pool_count(int thread_pool_count) { this.thread_pool_count = thread_pool_count; }

	public String getAutopull_max_llm_size() { return autopull_max_llm_size; }
	public void setAutopull_max_llm_size(String autopull_max_llm_size) { this.autopull_max_llm_size = autopull_max_llm_size; }

	public String getMcp_ports() { return mcp_ports; }
	public void setMcp_ports(String mcp_ports) { this.mcp_ports = mcp_ports; }

	public List<String> getMcp_sse_paths() { return mcp_sse_paths; }
	public void setMcp_sse_paths(List<String> mcp_sse_paths) { this.mcp_sse_paths = mcp_sse_paths; }

	public boolean isMcp_scan() { return mcp_scan; }
	public void setMcp_scan(boolean mcp_scan) { this.mcp_scan = mcp_scan; }

	public boolean isMcp_blind_trust() { return mcp_blind_trust; }
	public void setMcp_blind_trust(boolean mcp_blind_trust) { this.mcp_blind_trust = mcp_blind_trust; }

	public boolean isMcp_enable_promptinject_protection() { return mcp_enable_promptinject_protection; }
	public void setMcp_enable_promptinject_protection(boolean mcp_enable_promptinject_protection) { this.mcp_enable_promptinject_protection = mcp_enable_promptinject_protection; }

	public String getFiltered_mcp_toolnames_csv() { return filtered_mcp_toolnames_csv; }
	public void setFiltered_mcp_toolnames_csv(String filtered_mcp_toolnames_csv) { this.filtered_mcp_toolnames_csv = filtered_mcp_toolnames_csv; }

	public String getTrusted_mcp_toolnames_csv() { return trusted_mcp_toolnames_csv; }
	public void setTrusted_mcp_toolnames_csv(String trusted_mcp_toolnames_csv) { this.trusted_mcp_toolnames_csv = trusted_mcp_toolnames_csv; }

	public Map<String, Function<String, String>> getMcp_preprocess() { return mcp_preprocess; }
	public void setMcp_preprocess(Map<String, Function<String, String>> mcp_preprocess) { this.mcp_preprocess = mcp_preprocess; }

	public String getOpenaikey() { return openaikey; }
	public void setOpenaikey(String openaikey) { this.openaikey = openaikey; }

	public boolean isUse_openai() { return use_openai; }
	public void setUse_openai(boolean use_openai) { this.use_openai = use_openai; }

	public String getClaudekey() { return claudekey; }
	public void setClaudekey(String claudekey) { this.claudekey = claudekey; }

	public boolean isUse_claude() { return use_claude; }
	public void setUse_claude(boolean use_claude) { this.use_claude = use_claude; }

	public String getXaikey() { return xaikey; }
	public void setXaikey(String xaikey) { this.xaikey = xaikey; }

	public boolean isUse_xai() { return use_xai; }
	public void setUse_xai(boolean use_xai) { this.use_xai = use_xai; }

	public InteractMethod getInteract_method() { return interact_method; }
	public void setInteract_method(InteractMethod interact_method) { this.interact_method = interact_method; }

	public String getInteract_filepath() { return interact_filepath; }
	public void setInteract_filepath(String interact_filepath) { this.interact_filepath = interact_filepath; }

	public boolean isQwen3tts_enable() { return qwen3tts_enable; }
	public void setQwen3tts_enable(boolean qwen3tts_enable) { this.qwen3tts_enable = qwen3tts_enable; }

	public String getQwen3tts_url() { return qwen3tts_url; }
	public void setQwen3tts_url(String qwen3tts_url) { this.qwen3tts_url = qwen3tts_url; }

	public String getQwen3tts_voice() { return qwen3tts_voice; }
	public void setQwen3tts_voice(String qwen3tts_voice) { this.qwen3tts_voice = qwen3tts_voice; }

}