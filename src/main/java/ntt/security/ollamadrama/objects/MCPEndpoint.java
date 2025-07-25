package ntt.security.ollamadrama.objects;

public class MCPEndpoint {

	private String schema = "";
	private String host = "";
	private Integer port = -1;
	private String path = "/sse"; // server sent events
	
	public MCPEndpoint() {
		super();
	}

	public MCPEndpoint(String schema, String host, Integer port, String path) {
		super();
		this.schema = schema;
		this.host = host;
		this.port = port;
		this.path = path;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}
	
}
