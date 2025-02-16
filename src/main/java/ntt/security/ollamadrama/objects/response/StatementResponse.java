package ntt.security.ollamadrama.objects.response;

public class StatementResponse {

	private String response;
	private String explanation;
	private String assumptions_made;

	public StatementResponse() {
		super();
	}

	public StatementResponse(String _response, String _explanation, String _assumptions_made) {
		super();
		this.response = _response;
		this.explanation = _explanation;
		this.assumptions_made = _assumptions_made;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getAssumptions_made() {
		return assumptions_made;
	}

	public void setAssumptions_made(String assumptions_made) {
		this.assumptions_made = assumptions_made;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}
	
}
