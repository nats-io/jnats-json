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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class to build an {@link IndexedJsonValue} for an array directly from
 * values, without serializing to a string and re-parsing. This is the indexed
 * counterpart of {@link ArrayBuilder}.
 */
public class IndexedArrayBuilder implements JsonSerializable {
    /**
     * Whether this builder adds nulls to the array
     */
    public final boolean addNulls;

    /**
     * The IndexedJsonValue backing this builder
     */
    @NonNull
    public final IndexedJsonValue jv;

    private final List<IndexedJsonValue> theArray;

    /**
     * Get a new instance of IndexedArrayBuilder. Does not add nulls
     */
    public IndexedArrayBuilder() {
        this(false);
    }

    /**
     * Get a new instance of IndexedArrayBuilder, optionally adding nulls
     * @param addNulls whether to add nulls
     */
    public IndexedArrayBuilder(boolean addNulls) {
        this.addNulls = addNulls;
        theArray = new ArrayList<>();
        jv = new IndexedJsonValue(theArray);
    }

    /**
     * Get an instance of IndexedArrayBuilder that does not add nulls
     * @return an IndexedArrayBuilder instance
     */
    @NonNull
    public static IndexedArrayBuilder instance() {
        return new IndexedArrayBuilder(false);
    }

    /**
     * Get an instance of IndexedArrayBuilder that optionally adds nulls
     * @param addNulls whether to add nulls
     * @return an IndexedArrayBuilder instance
     */
    @NonNull
    public static IndexedArrayBuilder instance(boolean addNulls) {
        return new IndexedArrayBuilder(addNulls);
    }

    /**
     * Add an object to the array. The value is converted to an IndexedJsonValue if it isn't one already.
     * @param value the object
     * @return the builder
     */
    @NonNull
    public IndexedArrayBuilder add(@Nullable Object value) {
        if (value == null || value == JsonValue.NULL || value == IndexedJsonValue.NULL) {
            if (addNulls) {
                theArray.add(IndexedJsonValue.NULL);
            }
        }
        else if (value instanceof IndexedJsonValue) {
            theArray.add((IndexedJsonValue) value);
        }
        else {
            theArray.add(IndexedJsonValue.from(JsonValue.instance(value)));
        }
        return this;
    }

    /**
     * Add all items in the collection to the array.
     * Each value is converted to an IndexedJsonValue if it isn't one already.
     * @param values the collection
     * @return the builder
     */
    @NonNull
    public IndexedArrayBuilder addItems(@Nullable Collection<?> values) {
        if (values != null) {
            for (Object value : values) {
                add(value);
            }
        }
        return this;
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
