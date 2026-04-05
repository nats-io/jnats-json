package io.nats.json.examples;

import java.util.List;

/**
 * A nested POJO representing placement directives.
 * This is the BEFORE version -- no JSON support yet.
 */
public class Placement {
    private String cluster;
    private List<String> tags;

    public Placement() {}

    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
