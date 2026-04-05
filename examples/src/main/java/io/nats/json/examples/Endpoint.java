package io.nats.json.examples;

/**
 * A nested POJO representing an endpoint.
 * This is the BEFORE version -- no JSON support yet.
 */
public class Endpoint {
    private String name;
    private String url;
    private int weight;

    public Endpoint() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
}
