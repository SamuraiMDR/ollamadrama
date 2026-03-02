package ntt.security.ollamadrama.orchestrator;

import java.util.List;

public class Server {

    private String name;
    private String url;
    private boolean healthy;
    private List<String> models;
    private String capacity;
    private List<String> priority;
    private int active_requests;
    private String last_check;

    public Server() {
		super();
	}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean healthy) { this.healthy = healthy; }

    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }

    public String getCapacity() { return capacity; }
    public void setCapacity(String capacity) { this.capacity = capacity; }

    public List<String> getPriority() { return priority; }
    public void setPriority(List<String> priority) { this.priority = priority; }

    public int getActive_requests() { return active_requests; }
    public void setActive_requests(int active_requests) { this.active_requests = active_requests; }

    public String getLast_check() { return last_check; }
    public void setLast_check(String last_check) { this.last_check = last_check; }
}