package ntt.security.ollamadrama.objects.response;

public class SingleStringQuestionResponse {

	private String response = "";
	private Integer probability = 0;
	private boolean empty = true;
	private String motivation = "";
	private String assumptions_made = "";

	public SingleStringQuestionResponse() {
		super();
	}

	public SingleStringQuestionResponse(String _response, Integer _probability, String _motivation, String _assumptions_made) {
		super();
		this.response = _response;
		this.probability = _probability;
		this.empty = false;
		this.motivation = _motivation;
		this.assumptions_made = _assumptions_made;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public Integer getProbability() {
		return probability;
	}

	public void setProbability(Integer probability) {
		this.probability = probability;
	}

	public boolean isEmpty() {
		return empty;
	}

	public void setEmpty(boolean empty) {
		this.empty = empty;
	}

	public String getMotivation() {
		return motivation;
	}

	public void setMotivation(String motivation) {
		this.motivation = motivation;
	}

	public String getAssumptions_made() {
		return assumptions_made;
	}

	public void setAssumptions_made(String assumptions_made) {
		this.assumptions_made = assumptions_made;
	}
	
	public void print() {
		System.out.println("response         : " + this.response);
		System.out.println("probability      : " + this.probability + "%");
		System.out.println("motivation       : " + this.motivation);
		System.out.println("assumptions_made : " + this.assumptions_made);
	}
}
