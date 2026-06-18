// Copyright 2026 The NATS Authors
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to build an {@link IndexedJsonValue} for a map directly from
 * key/values, without serializing to a string and re-parsing. This is the
 * indexed counterpart of {@link MapBuilder}.
 */
public class IndexedMapBuilder implements JsonSerializable {
    /**
     * Whether this builder puts nulls in the map
     */
    public final boolean putNulls;

    /**
     * The IndexedJsonValue backing this builder
     */
    @NonNull
    public final IndexedJsonValue jv;

    private final Map<String, IndexedJsonValue> theMap;

    /**
     * Get a new instance of IndexedMapBuilder. Does not put nulls
     */
    public IndexedMapBuilder() {
        this(false);
    }

    /**
     * Get a new instance of IndexedMapBuilder, optionally putting nulls
     * @param putNulls whether to put nulls
     */
    public IndexedMapBuilder(boolean putNulls) {
        this.putNulls = putNulls;
        theMap = new LinkedHashMap<>();
        jv = new IndexedJsonValue(theMap);
    }

    /**
     * Get an instance of IndexedMapBuilder that does not put nulls
     * @return an IndexedMapBuilder instance
     */
    @NonNull
    public static IndexedMapBuilder instance() {
        return new IndexedMapBuilder(false);
    }

    /**
     * Get an instance of IndexedMapBuilder that optionally puts nulls
     * @param putNulls whether to put nulls
     * @return an IndexedMapBuilder instance
     */
    @NonNull
    public static IndexedMapBuilder instance(boolean putNulls) {
        return new IndexedMapBuilder(putNulls);
    }

    /**
     * Put an object in the map. The value is converted to an IndexedJsonValue if it isn't one already.
     * @param key the key
     * @param value the value
     * @return the builder
     */
    @NonNull
    public IndexedMapBuilder put(@NonNull String key, @Nullable Object value) {
        if (value == null || value == JsonValue.NULL || value == IndexedJsonValue.NULL || value == LazyJsonValue.NULL) {
            if (putNulls) {
                theMap.put(key, IndexedJsonValue.NULL);
            }
        }
        else if (value instanceof IndexedJsonValue) {
            theMap.put(key, (IndexedJsonValue) value);
        }
        else {
            theMap.put(key, IndexedJsonValue.from(JsonValue.instance(value)));
        }
        return this;
    }

    /**
     * Put all entries from the source map into the builder.
     * All values are converted to an IndexedJsonValue if they aren't one already.
     * @param map the map
     * @return the builder
     */
    @NonNull
    public IndexedMapBuilder putEntries(@Nullable Map<String, ?> map) {
        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    /**
     * Whether this builder puts null values in the map (vs. dropping them).
     * @return true if nulls are put
     */
    public boolean allowPutNulls() {
        return putNulls;
    }

    /**
     * Get the built {@link IndexedJsonValue}.
     * @return the IndexedJsonValue (the same instance as the public {@link #jv} field)
     */
    @NonNull
    public IndexedJsonValue build() {
        return jv;
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
     * <p>
     * Returns a fully materialized {@link JsonValue} view of the built value.
     * To obtain the {@link IndexedJsonValue} itself, use {@link #build()} or the
     * public {@link #jv} field.
     */
    @Override
    @NonNull
    public JsonValue toJsonValue() {
        return jv.toJsonValue();
    }
}
