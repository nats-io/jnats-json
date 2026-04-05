package io.nats.json.examples;

import io.nats.json.IndexedJsonValue;
import io.nats.json.JsonSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nats.json.IndexedJsonValueUtils.readString;
import static io.nats.json.IndexedJsonValueUtils.readStringListOrEmpty;
import static io.nats.json.JsonWriteUtils.*;

public class PlacementIndexed implements JsonSerializable {
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

    public static PlacementIndexed optionalInstance(IndexedJsonValue v) {
        return v == null ? null : new PlacementIndexed(v);
    }

    public static List<PlacementIndexed> listOf(IndexedJsonValue v) {
        if (v == null || v.getArray() == null) return Collections.emptyList();
        List<PlacementIndexed> list = new ArrayList<>(v.getArray().size());
        for (IndexedJsonValue item : v.getArray()) {
            list.add(new PlacementIndexed(item));
        }
        return Collections.unmodifiableList(list);
    }

    public static List<PlacementIndexed> optionalListOf(IndexedJsonValue v) {
        List<PlacementIndexed> list = listOf(v);
        return list.isEmpty() ? null : list;
    }
}
