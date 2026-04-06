package io.nats.json.examples;

import io.nats.json.JsonParseException;
import io.nats.json.JsonSerializable;
import io.nats.json.LazyJsonParser;
import io.nats.json.LazyJsonValue;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
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

    public String getName() { return readString(source, "name"); }
    public String getHost() { return readString(source, "host"); }
    public int getPort() { return readInteger(source, "port", 0); }
    public long getMaxConnections() { return readLong(source, "max_connections", -1); }
    public boolean isTlsEnabled() { return readBoolean(source, "tls_enabled", false); }
    public Duration getTimeout() { return readNanosAsDuration(source, "timeout"); }
    public List<String> getTags() { return readStringListOrEmpty(source, "tags"); }
    public Map<String, String> getMetadata() { return readStringMapOrNull(source, "metadata"); }
    public PlacementLazy getPlacement() { return PlacementLazy.optionalInstance(readValue(source, "placement")); }
    public List<EndpointLazy> getEndpoints() { return EndpointLazy.optionalListOf(readValue(source, "endpoints")); }

    @Override
    public @NonNull String toJson() {
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
