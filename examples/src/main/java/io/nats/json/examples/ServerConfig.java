package io.nats.json.examples;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * A plain POJO representing a server configuration.
 * This is the BEFORE version -- no JSON support yet.
 */
public class ServerConfig {
    private String name;
    private String host;
    private int port;
    private long maxConnections;
    private boolean tlsEnabled;
    private Duration timeout;
    private List<String> tags;
    private Map<String, String> metadata;
    private Placement placement;
    private List<Endpoint> endpoints;

    public ServerConfig() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public long getMaxConnections() { return maxConnections; }
    public void setMaxConnections(long maxConnections) { this.maxConnections = maxConnections; }

    public boolean isTlsEnabled() { return tlsEnabled; }
    public void setTlsEnabled(boolean tlsEnabled) { this.tlsEnabled = tlsEnabled; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public Placement getPlacement() { return placement; }
    public void setPlacement(Placement placement) { this.placement = placement; }

    public List<Endpoint> getEndpoints() { return endpoints; }
    public void setEndpoints(List<Endpoint> endpoints) { this.endpoints = endpoints; }
}
