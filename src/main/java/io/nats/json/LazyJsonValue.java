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

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
