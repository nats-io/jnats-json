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
 * Utility class to build a {@link LazyJsonValue} for a map directly from
 * key/values, without serializing to a string and re-parsing. This is the
 * lazy counterpart of {@link MapBuilder}.
 */
public class LazyMapBuilder implements JsonSerializable {
    /**
     * Whether this builder puts nulls in the map
     */
    public final boolean putNulls;

    /**
     * The LazyJsonValue backing this builder
     */
    @NonNull
    public final LazyJsonValue jv;

    private final Map<String, LazyJsonValue> theMap;

    /**
     * Get a new instance of LazyMapBuilder. Does not put nulls
     */
    public LazyMapBuilder() {
        this(false);
    }

    /**
     * Get a new instance of LazyMapBuilder, optionally putting nulls
     * @param putNulls whether to put nulls
     */
    public LazyMapBuilder(boolean putNulls) {
        this.putNulls = putNulls;
        theMap = new LinkedHashMap<>();
        jv = new LazyJsonValue(theMap);
    }

    /**
     * Get an instance of LazyMapBuilder that does not put nulls
     * @return a LazyMapBuilder instance
     */
    @NonNull
    public static LazyMapBuilder instance() {
        return new LazyMapBuilder(false);
    }

    /**
     * Get an instance of LazyMapBuilder that optionally puts nulls
     * @param putNulls whether to put nulls
     * @return a LazyMapBuilder instance
     */
    @NonNull
    public static LazyMapBuilder instance(boolean putNulls) {
        return new LazyMapBuilder(putNulls);
    }

    /**
     * Put an object in the map. The value is converted to a LazyJsonValue if it isn't one already.
     * @param key the key
     * @param value the value
     * @return the builder
     */
    @NonNull
    public LazyMapBuilder put(@NonNull String key, @Nullable Object value) {
        if (value == null || value == JsonValue.NULL || value == LazyJsonValue.NULL) {
            if (putNulls) {
                theMap.put(key, LazyJsonValue.NULL);
            }
        }
        else if (value instanceof LazyJsonValue) {
            theMap.put(key, (LazyJsonValue) value);
        }
        else {
            theMap.put(key, LazyJsonValue.from(JsonValue.instance(value)));
        }
        return this;
    }

    /**
     * Put all entries from the source map into the builder.
     * All values are converted to a LazyJsonValue if they aren't one already.
     * @param map the map
     * @return the builder
     */
    @NonNull
    public LazyMapBuilder putEntries(@Nullable Map<String, ?> map) {
        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    /**
     * Get the built {@link LazyJsonValue}.
     * @return the LazyJsonValue (the same instance as the public {@link #jv} field)
     */
    @NonNull
    public LazyJsonValue build() {
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
     * To obtain the {@link LazyJsonValue} itself, use {@link #build()} or the
     * public {@link #jv} field.
     */
    @Override
    @NonNull
    public JsonValue toJsonValue() {
        return jv.toJsonValue();
    }
}
