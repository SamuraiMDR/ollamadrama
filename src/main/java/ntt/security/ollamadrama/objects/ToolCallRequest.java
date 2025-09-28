package ntt.security.ollamadrama.objects;

import java.util.HashMap;

public class ToolCallRequest {

	private String toolname = "";
	private String calltype = "";
	private HashMap<String, Object> arguments;
	private String rawrequest;
	
	public ToolCallRequest() {
		super();
	}

	public ToolCallRequest(String toolname, String calltype, HashMap<String, Object> arguments, String rawrequest) {
		super();
		this.toolname = toolname;
		this.calltype = calltype;
		this.arguments = arguments;
		this.rawrequest = rawrequest;
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

	public String getRawrequest() {
		return rawrequest;
	}

	public void setRawrequest(String rawrequest) {
		this.rawrequest = rawrequest;
	}

	public boolean sanitycheck_pass() {
		if ((null == toolname)  || (toolname.length() == 0)) {
			return false;
		}
		if ((null == calltype)  || (calltype.length() == 0)) {
			return false;
		}
		if ((null == rawrequest)  || (rawrequest.length() == 0)) {
			return false;
		}
		return true;
	}
	

}
