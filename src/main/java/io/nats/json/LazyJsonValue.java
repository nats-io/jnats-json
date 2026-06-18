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

import java.util.*;

/**
 * A deeply lazy JSON value that defers BOTH leaf materialization (strings,
 * numbers) AND structural parsing (nested objects and arrays).
 * <p>
 * When the parser encounters a nested {@code {...}} or {@code [...]}, it
 * does not recurse into it. Instead, it records the byte-range offsets and
 * skips to the end via brace/bracket counting. The children are only parsed
 * one level deep when {@link #getMap()} or {@link #getArray()} is first called.
 * <p>
 * Produced by {@link LazyJsonParser}.
 */
public class LazyJsonValue extends AbstractIndexedJsonValue<LazyJsonValue> {

    // ---- singletons ----

    @NonNull public static final LazyJsonValue NULL = new LazyJsonValue(JsonValueType.NULL);
    @NonNull public static final LazyJsonValue TRUE = new LazyJsonValue(JsonValueType.BOOL);
    @NonNull public static final LazyJsonValue FALSE = new LazyJsonValue(JsonValueType.BOOL);
    @NonNull public static final LazyJsonValue EMPTY_MAP = new LazyJsonValue(JsonValueType.MAP);
    @NonNull public static final LazyJsonValue EMPTY_ARRAY = new LazyJsonValue(JsonValueType.ARRAY);

    // ---- fields (lazy-specific) ----

    private final boolean keepNulls;
    private Map<String, LazyJsonValue> map;
    private List<LazyJsonValue> array;
    private boolean resolved;

    // ---- constructors ----

    /**
     * Value backed by an offset range in the source char array.
     * For leaves (STRING, numbers): resolved=true, hasEscapes may be true, keepNulls ignored.
     * For unresolved containers (MAP, ARRAY): resolved=false, hasEscapes=false, keepNulls propagated.
     */
    LazyJsonValue(@NonNull JsonValueType type, char[] json, int start, int end,
                  boolean hasEscapes, boolean integersOnly, boolean keepNulls, boolean resolved) {
        super(type, json, start, end, hasEscapes, integersOnly);
        this.keepNulls = keepNulls;
        this.resolved = resolved;
    }

    LazyJsonValue(@NonNull String cachedString) {
        super(JsonValueType.STRING, cachedString, null, null);
        this.keepNulls = false;
        this.resolved = true;
    }

    LazyJsonValue(@NonNull JsonValueType numberType, @NonNull Number cachedNumber) {
        super(numberType, null, cachedNumber, numberType);
        this.keepNulls = false;
        this.resolved = true;
    }

    LazyJsonValue(@NonNull Map<String, LazyJsonValue> map) {
        super(JsonValueType.MAP, null, 0, 0, false, false);
        this.keepNulls = false;
        this.map = map;
        this.resolved = true;
    }

    LazyJsonValue(@NonNull List<LazyJsonValue> array) {
        super(JsonValueType.ARRAY, null, 0, 0, false, false);
        this.keepNulls = false;
        this.array = array;
        this.resolved = true;
    }

    private LazyJsonValue(@NonNull JsonValueType type) {
        super(type, null, 0, 0, false, false);
        this.keepNulls = false;
        this.resolved = true;
        if (type == JsonValueType.MAP) {
            this.map = Collections.emptyMap();
        }
        else if (type == JsonValueType.ARRAY) {
            this.array = Collections.emptyList();
        }
    }

    // ---- construction from a materialized JsonValue ----

    /**
     * Build a {@code LazyJsonValue} from a materialized {@link JsonValue}.
     * This is the structural inverse of {@link #toJsonValue()} and copies no
     * source characters. Map key order is preserved (following the source
     * {@code mapOrder} when present).
     * @param jv the source value
     * @return an equivalent LazyJsonValue
     */
    @NonNull
    public static LazyJsonValue from(@NonNull JsonValue jv) {
        switch (jv.type) {
            case STRING:
                //noinspection DataFlowIssue -- type is STRING, so jv.string is not null
                return new LazyJsonValue(jv.string);
            case BOOL:
                return Boolean.TRUE.equals(jv.bool) ? TRUE : FALSE;
            case INTEGER:
            case LONG:
            case DOUBLE:
            case FLOAT:
            case BIG_DECIMAL:
            case BIG_INTEGER:
                //noinspection DataFlowIssue -- type is numeric, so jv.number is not null
                return new LazyJsonValue(jv.type, jv.number);
            case MAP:
                return fromMap(jv);
            case ARRAY:
                return fromArray(jv);
            default:
                return NULL;
        }
    }

    private static LazyJsonValue fromMap(@NonNull JsonValue jv) {
        Map<String, JsonValue> src = jv.map;
        if (src == null || src.isEmpty()) {
            return EMPTY_MAP;
        }
        Map<String, LazyJsonValue> out = new LinkedHashMap<>();
        for (String key : jv.mapOrder) {
            JsonValue child = src.get(key);
            if (child != null) {
                out.put(key, from(child));
            }
        }
        for (Map.Entry<String, JsonValue> e : src.entrySet()) {
            if (!out.containsKey(e.getKey())) {
                out.put(e.getKey(), from(e.getValue()));
            }
        }
        return new LazyJsonValue(out);
    }

    private static LazyJsonValue fromArray(@NonNull JsonValue jv) {
        List<JsonValue> src = jv.array;
        if (src == null || src.isEmpty()) {
            return EMPTY_ARRAY;
        }
        List<LazyJsonValue> out = new ArrayList<>(src.size());
        for (JsonValue child : src) {
            out.add(from(child));
        }
        return new LazyJsonValue(out);
    }

    // ---- abstract method implementations ----

    @Override
    protected LazyJsonValue trueInstance() {
        return TRUE;
    }

    @Override
    protected LazyJsonValue falseInstance() {
        return FALSE;
    }

    @Override
    @Nullable
    public Map<String, LazyJsonValue> getMap() {
        if (type != JsonValueType.MAP) {
            return null;
        }
        if (!resolved) {
            resolve();
        }
        return map;
    }

    @Override
    @Nullable
    public List<LazyJsonValue> getArray() {
        if (type != JsonValueType.ARRAY) {
            return null;
        }
        if (!resolved) {
            resolve();
        }
        return array;
    }

    // ---- lazy resolution ----

    private void resolve() {
        if (type == JsonValueType.MAP) {
            map = LazyJsonParser.shallowParseObject(json, start, end, keepNulls, integersOnly);
        }
        else if (type == JsonValueType.ARRAY) {
            array = LazyJsonParser.shallowParseArray(json, start, end, keepNulls, integersOnly);
        }
        resolved = true;
    }
}
