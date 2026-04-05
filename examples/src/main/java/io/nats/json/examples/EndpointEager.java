package io.nats.json.examples;

import io.nats.json.JsonSerializable;
import io.nats.json.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nats.json.JsonValueUtils.readInteger;
import static io.nats.json.JsonValueUtils.readString;
import static io.nats.json.JsonWriteUtils.*;

public class EndpointEager implements JsonSerializable {
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

    public static EndpointEager optionalInstance(JsonValue v) {
        return v == null ? null : new EndpointEager(v);
    }

    public static List<EndpointEager> listOf(JsonValue v) {
        if (v == null || v.array == null) return Collections.emptyList();
        List<EndpointEager> list = new ArrayList<>(v.array.size());
        for (JsonValue item : v.array) {
            list.add(new EndpointEager(item));
        }
        return Collections.unmodifiableList(list);
    }

    public static List<EndpointEager> optionalListOf(JsonValue v) {
        List<EndpointEager> list = listOf(v);
        return list.isEmpty() ? null : list;
    }
}
