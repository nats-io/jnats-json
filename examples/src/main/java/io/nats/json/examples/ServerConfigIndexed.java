package io.nats.json.examples;

import io.nats.json.IndexedJsonParser;
import io.nats.json.IndexedJsonValue;
import io.nats.json.JsonParseException;
import io.nats.json.JsonSerializable;

import java.time.Duration;
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

    public String getName() { return readString(source, "name"); }
    public String getHost() { return readString(source, "host"); }
    public int getPort() { return readInteger(source, "port", 0); }
    public long getMaxConnections() { return readLong(source, "max_connections", -1); }
    public boolean isTlsEnabled() { return readBoolean(source, "tls_enabled", false); }
    public Duration getTimeout() { return readNanosAsDuration(source, "timeout"); }
    public List<String> getTags() { return readStringListOrEmpty(source, "tags"); }
    public Map<String, String> getMetadata() { return readStringMapOrNull(source, "metadata"); }
    public PlacementIndexed getPlacement() { return PlacementIndexed.optionalInstance(readValue(source, "placement")); }
    public List<EndpointIndexed> getEndpoints() { return EndpointIndexed.optionalListOf(readValue(source, "endpoints")); }

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
}
