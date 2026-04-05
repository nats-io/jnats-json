package io.nats.json.examples;

import io.nats.json.IndexedJsonParser;
import io.nats.json.IndexedJsonValue;
import io.nats.json.JsonParseException;
import io.nats.json.JsonSerializable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.nats.json.IndexedJsonValueUtils.*;
import static io.nats.json.JsonWriteUtils.*;

/**
 * ServerConfig converted to use the indexed parser (IndexedJsonParser / IndexedJsonValue).
 * Strings and numbers are copied/parsed lazily on first access.
 * The full structure (HashMap, ArrayList) is built during parse.
 */
public class ServerConfigIndexed implements JsonSerializable {
    private final IndexedJsonValue source;

    public ServerConfigIndexed(String json) throws JsonParseException {
        this.source = IndexedJsonParser.parse(json);
    }

    public ServerConfigIndexed(IndexedJsonValue v) {
        this.source = v;
    }

    // -- Lazy getters: data is only copied/parsed when accessed --
    public String getName() { return readString(source, "name"); }
    public String getHost() { return readString(source, "host"); }
    public int getPort() { return readInteger(source, "port", 0); }
    public long getMaxConnections() { return readLong(source, "max_connections", -1); }
    public boolean isTlsEnabled() { return readBoolean(source, "tls_enabled", false); }
    public Duration getTimeout() { return readNanosAsDuration(source, "timeout"); }
    public List<String> getTags() { return readStringListOrEmpty(source, "tags"); }
    public Map<String, String> getMetadata() { return readStringMapOrNull(source, "metadata"); }

    // -- Nested object: wrap in its own indexed type on access --
    public PlacementIndexed getPlacement() {
        IndexedJsonValue pv = readValue(source, "placement");
        return pv == null ? null : new PlacementIndexed(pv);
    }

    // -- List of nested objects: wrap each element on access --
    public List<EndpointIndexed> getEndpoints() {
        List<IndexedJsonValue> evs = readArrayOrNull(source, "endpoints");
        if (evs == null) return Collections.emptyList();
        List<EndpointIndexed> list = new ArrayList<>(evs.size());
        for (IndexedJsonValue ev : evs) {
            list.add(new EndpointIndexed(ev));
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public String toJson() {
        StringBuilder sb = beginJson();
        addField(sb, "name", getName());
        addField(sb, "host", getHost());
        addField(sb, "port", getPort());
        addField(sb, "max_connections", getMaxConnections());
        addField(sb, "tls_enabled", isTlsEnabled());
        addFieldAsNanos(sb, "timeout", getTimeout());
        addStrings(sb, "tags", getTags());
        addField(sb, "metadata", getMetadata());
        addField(sb, "placement", getPlacement());
        addJsons(sb, "endpoints", getEndpoints());
        return endJson(sb).toString();
    }

    // ---- Nested: Placement ----
    public static class PlacementIndexed implements JsonSerializable {
        private final IndexedJsonValue source;

        public PlacementIndexed(IndexedJsonValue v) {
            this.source = v;
        }

        public String getCluster() { return readString(source, "cluster"); }
        public List<String> getTags() { return readStringListOrEmpty(source, "tags"); }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            addField(sb, "cluster", getCluster());
            addStrings(sb, "tags", getTags());
            return endJson(sb).toString();
        }
    }

    // ---- Nested: Endpoint ----
    public static class EndpointIndexed implements JsonSerializable {
        private final IndexedJsonValue source;

        public EndpointIndexed(IndexedJsonValue v) {
            this.source = v;
        }

        public String getName() { return readString(source, "name"); }
        public String getUrl() { return readString(source, "url"); }
        public int getWeight() { return readInteger(source, "weight", 0); }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            addField(sb, "name", getName());
            addField(sb, "url", getUrl());
            addField(sb, "weight", getWeight());
            return endJson(sb).toString();
        }
    }
}
