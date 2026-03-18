// Copyright 2025-2026 The NATS Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.json;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to build a JsonValue for a map
 */
public class MapBuilder implements JsonSerializable {
    /**
     * Whether this builder puts nulls in the map
     */
    public final boolean putNulls;

    /**
     * The JsonValue backing this MapBuilder
     */
    @NonNull
    public final JsonValue jv;

    private final Map<String, JsonValue> theMap;

    /**
     * Get a new instance of MapBuilder. Does not put nulls
     */
    public MapBuilder() {
        this(false);
    }

    /**
     * Get a new instance of MapBuilder, optionally putting nulls
     * @param putNulls whether to put nulls
     */
    public MapBuilder(boolean putNulls) {
        this.putNulls = putNulls;
        theMap = new HashMap<>();
        jv = new JsonValue(theMap);
    }

    /**
     * Get an instance of MapBuilder that does not put nulls
     * @return a MapBuilder instance
     */
    @NonNull
    public static MapBuilder instance() {
        return new MapBuilder(false);
    }

    /**
     * Get an instance of MapBuilder that optionally puts nulls
     * @param putNulls whether to put nulls
     * @return a MapBuilder instance
     */
    @NonNull
    public static MapBuilder instance(boolean putNulls) {
        return new MapBuilder(putNulls);
    }

    /**
     * Put an object in the map. The value is converted to a JsonValue if it isn't one already.
     * @param key the key
     * @param value the value
     * @return the builder
     */
    @NonNull
    public MapBuilder put(@NonNull String key, @Nullable Object value) {
        if (value == null || value == JsonValue.NULL) {
            if (putNulls) {
                theMap.put(key, JsonValue.NULL);
                jv.mapOrder.add(key);
            }
        }
        else {
            theMap.put(key, JsonValue.instance(value));
            jv.mapOrder.add(key);
        }
        return this;
    }

    /**
     * Put all entries from the source map into the MapBuilder.
     * All values are converted to a JsonValue if they aren't one already.
     * @param map the map
     * @return the builder
     */
    @NonNull
    public MapBuilder putEntries(@Nullable Map<String, ?> map) {
        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String toJson() {
        return jv.toJson();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public JsonValue toJsonValue() {
        return jv;
    }
}
