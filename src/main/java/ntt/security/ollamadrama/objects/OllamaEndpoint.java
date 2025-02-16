package ntt.security.ollamadrama.objects;

public class OllamaEndpoint {

	private String ollama_url = "";
	private String ollama_username = "";
	private String ollama_password = "";
	
	public OllamaEndpoint() {
		super();
	}
	
	public OllamaEndpoint(String _ollama_url, String _ollama_username, String _ollama_password) {
		super();
		this.ollama_url = _ollama_url;
		this.ollama_username = _ollama_username;
		this.ollama_password = _ollama_password;
	}

	public String getOllama_url() {
		return ollama_url;
	}
	
	public void setOllama_url(String ollama_url) {
		this.ollama_url = ollama_url;
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
	
}
