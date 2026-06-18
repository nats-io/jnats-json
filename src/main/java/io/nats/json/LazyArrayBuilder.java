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
 * Utility class to build a {@link LazyJsonValue} for an array directly from
 * values, without serializing to a string and re-parsing. This is the lazy
 * counterpart of {@link ArrayBuilder}.
 */
public class LazyArrayBuilder implements JsonSerializable {
    /**
     * Whether this builder adds nulls to the array
     */
    public final boolean addNulls;

    /**
     * The LazyJsonValue backing this builder
     */
    @NonNull
    public final LazyJsonValue jv;

    private final List<LazyJsonValue> theArray;

    /**
     * Get a new instance of LazyArrayBuilder. Does not add nulls
     */
    public LazyArrayBuilder() {
        this(false);
    }

    /**
     * Get a new instance of LazyArrayBuilder, optionally adding nulls
     * @param addNulls whether to add nulls
     */
    public LazyArrayBuilder(boolean addNulls) {
        this.addNulls = addNulls;
        theArray = new ArrayList<>();
        jv = new LazyJsonValue(theArray);
    }

    /**
     * Get an instance of LazyArrayBuilder that does not add nulls
     * @return a LazyArrayBuilder instance
     */
    @NonNull
    public static LazyArrayBuilder instance() {
        return new LazyArrayBuilder(false);
    }

    /**
     * Get an instance of LazyArrayBuilder that optionally adds nulls
     * @param addNulls whether to add nulls
     * @return a LazyArrayBuilder instance
     */
    @NonNull
    public static LazyArrayBuilder instance(boolean addNulls) {
        return new LazyArrayBuilder(addNulls);
    }

    /**
     * Add an object to the array. The value is converted to a LazyJsonValue if it isn't one already.
     * @param value the object
     * @return the builder
     */
    @NonNull
    public LazyArrayBuilder add(@Nullable Object value) {
        if (value == null || value == JsonValue.NULL || value == LazyJsonValue.NULL) {
            if (addNulls) {
                theArray.add(LazyJsonValue.NULL);
            }
        }
        else if (value instanceof LazyJsonValue) {
            theArray.add((LazyJsonValue) value);
        }
        else {
            theArray.add(LazyJsonValue.from(JsonValue.instance(value)));
        }
        return this;
    }

    /**
     * Add all items in the collection to the array.
     * Each value is converted to a LazyJsonValue if it isn't one already.
     * @param values the collection
     * @return the builder
     */
    @NonNull
    public LazyArrayBuilder addItems(@Nullable Collection<?> values) {
        if (values != null) {
            for (Object value : values) {
                add(value);
            }
        }
        return this;
    }

    /**
     * Whether this builder adds null values to the array (vs. dropping them).
     * @return true if nulls are added
     */
    public boolean allowAddNulls() {
        return addNulls;
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
