package ntt.security.ollamadrama.objects;

import java.util.HashMap;

public class ToolCallRequest {

	private String toolname = "";
	private String calltype = "";
	private HashMap<String, Object> arguments;
	
	public ToolCallRequest() {
		super();
	}

	public ToolCallRequest(String toolname, String calltype, HashMap<String, Object> arguments) {
		super();
		this.toolname = toolname;
		this.calltype = calltype;
		this.arguments = arguments;
	}

	public String getToolname() {
		return toolname;
	}

	public void setToolname(String toolname) {
		this.toolname = toolname;
	}

	public String getCalltype() {
		return calltype;
	}

	public void setCalltype(String calltype) {
		this.calltype = calltype;
	}

	public HashMap<String, Object> getArguments() {
		return arguments;
	}

	public void setArguments(HashMap<String, Object> arguments) {
		this.arguments = arguments;
	}
	

}
