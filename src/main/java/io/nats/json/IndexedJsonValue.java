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
 * A JSON value that references the original source char array and only
 * copies/parses data when accessor methods are called. This is the "indexed"
 * counterpart of {@link JsonValue} produced by {@link IndexedJsonParser}.
 * <p>
 * For STRING and number types the raw characters are retained as offsets into
 * the source array. Extraction (and any escape-sequence processing for strings)
 * happens lazily on first access and is then cached.
 * <p>
 * MAP and ARRAY children are themselves {@code IndexedJsonValue} instances,
 * so the entire tree is lazy at the leaf level. The full tree structure
 * (HashMaps, ArrayLists) is built eagerly during parsing.
 */
public class IndexedJsonValue extends AbstractIndexedJsonValue<IndexedJsonValue> {

    // ---- singletons ----

    @NonNull public static final IndexedJsonValue NULL = new IndexedJsonValue(JsonValueType.NULL);
    @NonNull public static final IndexedJsonValue TRUE = new IndexedJsonValue(JsonValueType.BOOL);
    @NonNull public static final IndexedJsonValue FALSE = new IndexedJsonValue(JsonValueType.BOOL);
    @NonNull public static final IndexedJsonValue EMPTY_MAP = new IndexedJsonValue(JsonValueType.MAP);
    @NonNull public static final IndexedJsonValue EMPTY_ARRAY = new IndexedJsonValue(JsonValueType.ARRAY);

    // ---- fields (container children only) ----

    private final Map<String, IndexedJsonValue> map;
    private final List<IndexedJsonValue> array;

    // ---- constructors ----

    IndexedJsonValue(@NonNull JsonValueType type, char[] json, int start, int end, boolean hasEscapes) {
        this(type, json, start, end, hasEscapes, false);
    }

    IndexedJsonValue(@NonNull JsonValueType type, char[] json, int start, int end,
                     boolean hasEscapes, boolean integersOnly) {
        super(type, json, start, end, hasEscapes, integersOnly);
        this.map = null;
        this.array = null;
    }

    IndexedJsonValue(@NonNull Map<String, IndexedJsonValue> map) {
        super(JsonValueType.MAP, null, 0, 0, false, false);
        this.map = map;
        this.array = null;
    }

    IndexedJsonValue(@NonNull List<IndexedJsonValue> array) {
        super(JsonValueType.ARRAY, null, 0, 0, false, false);
        this.map = null;
        this.array = array;
    }

    private IndexedJsonValue(@NonNull JsonValueType type) {
        super(type, null, 0, 0, false, false);
        if (type == JsonValueType.MAP) {
            this.map = Collections.emptyMap();
            this.array = null;
        }
        else if (type == JsonValueType.ARRAY) {
            this.map = null;
            this.array = Collections.emptyList();
        }
        else {
            this.map = null;
            this.array = null;
        }
    }

    // ---- abstract method implementations ----

    @Override
    protected IndexedJsonValue trueInstance() {
        return TRUE;
    }

    @Override
    protected IndexedJsonValue falseInstance() {
        return FALSE;
    }

    @Override
    @Nullable
    public Map<String, IndexedJsonValue> getMap() {
        return map;
    }

    @Override
    @Nullable
    public List<IndexedJsonValue> getArray() {
        return array;
    }
}
