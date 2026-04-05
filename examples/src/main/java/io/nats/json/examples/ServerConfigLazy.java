package io.nats.json.examples;

import io.nats.json.JsonParseException;
import io.nats.json.JsonSerializable;
import io.nats.json.LazyJsonParser;
import io.nats.json.LazyJsonValue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.nats.json.JsonWriteUtils.*;
import static io.nats.json.LazyJsonValueUtils.*;

/**
 * ServerConfig converted to use the lazy parser (LazyJsonParser / LazyJsonValue).
 * Nested objects and arrays are not parsed until accessed.
 * Strings and numbers are also deferred.
 * Best for large documents where you only read a few fields.
 */
public class ServerConfigLazy implements JsonSerializable {
    private final LazyJsonValue source;

    public ServerConfigLazy(String json) throws JsonParseException {
        this.source = LazyJsonParser.parse(json);
    }

    public ServerConfigLazy(LazyJsonValue v) {
        this.source = v;
    }

    // -- Lazy getters: nothing is parsed until you call these --
    public String getName() { return readString(source, "name"); }
    public String getHost() { return readString(source, "host"); }
    public int getPort() { return readInteger(source, "port", 0); }
    public long getMaxConnections() { return readLong(source, "max_connections", -1); }
    public boolean isTlsEnabled() { return readBoolean(source, "tls_enabled", false); }
    public Duration getTimeout() { return readNanosAsDuration(source, "timeout"); }
    public List<String> getTags() { return readStringListOrEmpty(source, "tags"); }
    public Map<String, String> getMetadata() { return readStringMapOrNull(source, "metadata"); }

    // -- Nested object: wrap on access. The placement {...} was skip-scanned during
    //    the initial parse. It is only parsed when getPlacement() is called. --
    public PlacementLazy getPlacement() {
        LazyJsonValue pv = readValue(source, "placement");
        return pv == null ? null : new PlacementLazy(pv);
    }

    // -- List of nested objects: the endpoints [...] was skip-scanned during parse.
    //    Calling getEndpoints() triggers parsing the array one level. Each element
    //    that is an object is itself an unresolved offset range until accessed. --
    public List<EndpointLazy> getEndpoints() {
        List<LazyJsonValue> evs = readArrayOrNull(source, "endpoints");
        if (evs == null) return Collections.emptyList();
        List<EndpointLazy> list = new ArrayList<>(evs.size());
        for (LazyJsonValue ev : evs) {
            list.add(new EndpointLazy(ev));
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
    public static class PlacementLazy implements JsonSerializable {
        private final LazyJsonValue source;

        public PlacementLazy(LazyJsonValue v) {
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
    public static class EndpointLazy implements JsonSerializable {
        private final LazyJsonValue source;

        public EndpointLazy(LazyJsonValue v) {
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
