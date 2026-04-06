package io.nats.json.examples;

import io.nats.json.JsonSerializable;
import io.nats.json.LazyJsonValue;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nats.json.JsonWriteUtils.*;
import static io.nats.json.LazyJsonValueUtils.readString;
import static io.nats.json.LazyJsonValueUtils.readStringListOrEmpty;

public class PlacementLazy implements JsonSerializable {
    private final LazyJsonValue source;

    public PlacementLazy(LazyJsonValue v) {
        this.source = v;
    }

    public String getCluster() { return readString(source, "cluster"); }
    public List<String> getTags() { return readStringListOrEmpty(source, "tags"); }

    @Override
    public @NonNull String toJson() {
        StringBuilder sb = beginJson();
        addField(sb, "cluster", getCluster());
        addStrings(sb, "tags", getTags());
        return endJson(sb).toString();
    }

    public static PlacementLazy optionalInstance(LazyJsonValue v) {
        return v == null ? null : new PlacementLazy(v);
    }

    public static List<PlacementLazy> listOf(LazyJsonValue v) {
        if (v == null || v.getArray() == null) return Collections.emptyList();
        List<PlacementLazy> list = new ArrayList<>(v.getArray().size());
        for (LazyJsonValue item : v.getArray()) {
            list.add(new PlacementLazy(item));
        }
        return Collections.unmodifiableList(list);
    }

    public static List<PlacementLazy> optionalListOf(LazyJsonValue v) {
        List<PlacementLazy> list = listOf(v);
        return list.isEmpty() ? null : list;
    }
}
