package ntt.security.ollamadrama.objects;

public class MCPTool {

	private String toolname = "";
	private String tool_str = "";
	private MCPEndpoint endpoint;
	
	public MCPTool() {
		super();
	}

	public MCPTool(String _toolname, String _tool_str, MCPEndpoint _endpoint) {
		super();
		this.toolname = _toolname;
		this.tool_str = _tool_str;
		this.endpoint = _endpoint;
	}

	public String getToolname() {
		return toolname;
	}

	public void setToolname(String toolname) {
		this.toolname = toolname;
	}

	public String getTool_str() {
		return tool_str;
	}

	public void setTool_str(String tool_str) {
		this.tool_str = tool_str;
	}

	public MCPEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(MCPEndpoint endpoint) {
		this.endpoint = endpoint;
	}

}
