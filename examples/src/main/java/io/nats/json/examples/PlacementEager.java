package io.nats.json.examples;

import io.nats.json.JsonSerializable;
import io.nats.json.JsonValue;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nats.json.JsonValueUtils.readString;
import static io.nats.json.JsonValueUtils.readStringListOrEmpty;
import static io.nats.json.JsonWriteUtils.*;

public class PlacementEager implements JsonSerializable {
    private final String cluster;
    private final List<String> tags;

    public PlacementEager(JsonValue v) {
        this.cluster = readString(v, "cluster");
        this.tags = readStringListOrEmpty(v, "tags");
    }

    @Override
    public @NonNull String toJson() {
        StringBuilder sb = beginJson();
        addField(sb, "cluster", cluster);
        addStrings(sb, "tags", tags);
        return endJson(sb).toString();
    }

    public String getCluster() { return cluster; }
    public List<String> getTags() { return tags; }

    /**
     * Construct from a JsonValue if non-null, otherwise return null.
     */
    public static PlacementEager optionalInstance(JsonValue v) {
        return v == null ? null : new PlacementEager(v);
    }

    /**
     * Convert a JsonValue array into a list of PlacementEager.
     * Returns an empty list if the input is null.
     */
    public static List<PlacementEager> listOf(JsonValue v) {
        if (v == null || v.array == null) return Collections.emptyList();
        List<PlacementEager> list = new ArrayList<>(v.array.size());
        for (JsonValue item : v.array) {
            list.add(new PlacementEager(item));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Convert a JsonValue array into a list of PlacementEager.
     * Returns null if the input is null or the resulting list is empty.
     */
    public static List<PlacementEager> optionalListOf(JsonValue v) {
        List<PlacementEager> list = listOf(v);
        return list.isEmpty() ? null : list;
    }
}
