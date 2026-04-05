package io.nats.json.examples;

import io.nats.json.IndexedJsonValue;
import io.nats.json.JsonSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nats.json.IndexedJsonValueUtils.readInteger;
import static io.nats.json.IndexedJsonValueUtils.readString;
import static io.nats.json.JsonWriteUtils.*;

public class EndpointIndexed implements JsonSerializable {
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

    public static EndpointIndexed optionalInstance(IndexedJsonValue v) {
        return v == null ? null : new EndpointIndexed(v);
    }

    public static List<EndpointIndexed> listOf(IndexedJsonValue v) {
        if (v == null || v.getArray() == null) return Collections.emptyList();
        List<EndpointIndexed> list = new ArrayList<>(v.getArray().size());
        for (IndexedJsonValue item : v.getArray()) {
            list.add(new EndpointIndexed(item));
        }
        return Collections.unmodifiableList(list);
    }

    public static List<EndpointIndexed> optionalListOf(IndexedJsonValue v) {
        List<EndpointIndexed> list = listOf(v);
        return list.isEmpty() ? null : list;
    }
}
