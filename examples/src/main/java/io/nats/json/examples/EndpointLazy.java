package io.nats.json.examples;

import io.nats.json.JsonSerializable;
import io.nats.json.LazyJsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nats.json.JsonWriteUtils.*;
import static io.nats.json.LazyJsonValueUtils.readInteger;
import static io.nats.json.LazyJsonValueUtils.readString;

public class EndpointLazy implements JsonSerializable {
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

    public static EndpointLazy optionalInstance(LazyJsonValue v) {
        return v == null ? null : new EndpointLazy(v);
    }

    public static List<EndpointLazy> listOf(LazyJsonValue v) {
        if (v == null || v.getArray() == null) return Collections.emptyList();
        List<EndpointLazy> list = new ArrayList<>(v.getArray().size());
        for (LazyJsonValue item : v.getArray()) {
            list.add(new EndpointLazy(item));
        }
        return Collections.unmodifiableList(list);
    }

    public static List<EndpointLazy> optionalListOf(LazyJsonValue v) {
        List<EndpointLazy> list = listOf(v);
        return list.isEmpty() ? null : list;
    }
}
