package io.nats.json.examples;

import io.nats.json.JsonParseException;
import io.nats.json.JsonParser;
import io.nats.json.JsonSerializable;
import io.nats.json.JsonValue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.nats.json.JsonValueUtils.*;
import static io.nats.json.JsonWriteUtils.*;

/**
 * ServerConfig converted to use the eager parser (JsonParser / JsonValue).
 * All fields are read eagerly during construction.
 */
public class ServerConfigEager implements JsonSerializable {
    private final String name;
    private final String host;
    private final int port;
    private final long maxConnections;
    private final boolean tlsEnabled;
    private final Duration timeout;
    private final List<String> tags;
    private final Map<String, String> metadata;
    private final PlacementEager placement;
    private final List<EndpointEager> endpoints;

    public ServerConfigEager(String json) throws JsonParseException {
        this(JsonParser.parse(json));
    }

    public ServerConfigEager(JsonValue v) {
        this.name = readString(v, "name");
        this.host = readString(v, "host");
        this.port = readInteger(v, "port", 0);
        this.maxConnections = readLong(v, "max_connections", -1);
        this.tlsEnabled = readBoolean(v, "tls_enabled", false);
        this.timeout = readNanosAsDuration(v, "timeout");
        this.tags = readStringListOrEmpty(v, "tags");
        this.metadata = readStringMapOrNull(v, "metadata");

        // Nested object: read as JsonValue, construct if present
        JsonValue pv = readValue(v, "placement");
        this.placement = pv == null ? null : new PlacementEager(pv);

        // List of nested objects: read array, construct each
        List<JsonValue> evs = readArrayOrNull(v, "endpoints");
        if (evs == null) {
            this.endpoints = Collections.emptyList();
        }
        else {
            List<EndpointEager> list = new ArrayList<>(evs.size());
            for (JsonValue ev : evs) {
                list.add(new EndpointEager(ev));
            }
            this.endpoints = Collections.unmodifiableList(list);
        }
    }

    @Override
    public String toJson() {
        StringBuilder sb = beginJson();
        addField(sb, "name", name);
        addField(sb, "host", host);
        addField(sb, "port", port);
        addField(sb, "max_connections", maxConnections);
        addField(sb, "tls_enabled", tlsEnabled);
        addFieldAsNanos(sb, "timeout", timeout);
        addStrings(sb, "tags", tags);
        addField(sb, "metadata", metadata);
        addField(sb, "placement", placement);
        addJsons(sb, "endpoints", endpoints);
        return endJson(sb).toString();
    }

    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public long getMaxConnections() { return maxConnections; }
    public boolean isTlsEnabled() { return tlsEnabled; }
    public Duration getTimeout() { return timeout; }
    public List<String> getTags() { return tags; }
    public Map<String, String> getMetadata() { return metadata; }
    public PlacementEager getPlacement() { return placement; }
    public List<EndpointEager> getEndpoints() { return endpoints; }

    // ---- Nested: Placement ----
    public static class PlacementEager implements JsonSerializable {
        private final String cluster;
        private final List<String> tags;

        public PlacementEager(JsonValue v) {
            this.cluster = readString(v, "cluster");
            this.tags = readStringListOrEmpty(v, "tags");
        }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            addField(sb, "cluster", cluster);
            addStrings(sb, "tags", tags);
            return endJson(sb).toString();
        }

        public String getCluster() { return cluster; }
        public List<String> getTags() { return tags; }
    }

    // ---- Nested: Endpoint ----
    public static class EndpointEager implements JsonSerializable {
        private final String name;
        private final String url;
        private final int weight;

        public EndpointEager(JsonValue v) {
            this.name = readString(v, "name");
            this.url = readString(v, "url");
            this.weight = readInteger(v, "weight", 0);
        }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            addField(sb, "name", name);
            addField(sb, "url", url);
            addField(sb, "weight", weight);
            return endJson(sb).toString();
        }

        public String getName() { return name; }
        public String getUrl() { return url; }
        public int getWeight() { return weight; }
    }
}
