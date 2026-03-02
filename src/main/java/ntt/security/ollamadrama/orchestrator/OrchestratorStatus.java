package ntt.security.ollamadrama.orchestrator;

import java.util.List;

public class OrchestratorStatus {

    private String status;
    private int total_servers;
    private int healthy_servers;
    private List<Server> servers;
   
    public OrchestratorStatus() {
		super();
	}
	
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotal_servers() { return total_servers; }
    public void setTotal_servers(int total_servers) { this.total_servers = total_servers; }

    public int getHealthy_servers() { return healthy_servers; }
    public void setHealthy_servers(int healthy_servers) { this.healthy_servers = healthy_servers; }

    public List<Server> getServers() { return servers; }
    public void setServers(List<Server> servers) { this.servers = servers; }

}
