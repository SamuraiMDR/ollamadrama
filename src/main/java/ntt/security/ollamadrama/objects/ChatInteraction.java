package ntt.security.ollamadrama.objects;

import io.github.ollama4j.models.chat.OllamaChatResult;

public class ChatInteraction {

	private OllamaChatResult chatResult;
	private String response;
	private boolean success;
	
	public ChatInteraction() {
		super();
	}
	
	public ChatInteraction(OllamaChatResult _chatResult, String _response, boolean _success) {
		super();
		this.chatResult = _chatResult;
		this.response = _response;
		this.success = _success;
	}

	public OllamaChatResult getChatResult() {
		return chatResult;
	}

	public void setChatResult(OllamaChatResult chatResult) {
		this.chatResult = chatResult;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}
	
}
