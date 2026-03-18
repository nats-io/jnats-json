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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class to build a JsonValue for an array
 */
public class ArrayBuilder implements JsonSerializable {
    /**
     * Whether this builder puts nulls in the array
     */
    public final boolean addNulls;

    /**
     * The JsonValue backing this ArrayBuilder
     */
    @NonNull
    public final JsonValue jv;

    private final List<JsonValue> theArray;

    /**
     * Get a new instance of ArrayBuilder. Does not add nulls
     */
    public ArrayBuilder() {
        this(false);
    }

    /**
     * Get a new instance of ArrayBuilder, optionally adding nulls
     * @param addNulls whether to add nulls
     */
    public ArrayBuilder(boolean addNulls) {
        this.addNulls = addNulls;
        theArray = new ArrayList<>();
        jv = new JsonValue(theArray);
    }

    /**
     * Get an instance of ArrayBuilder that does not add nulls
     * @return an ArrayBuilder instance
     */
    @NonNull
    public static ArrayBuilder instance() {
        return new ArrayBuilder(false);
    }

    /**
     * Get an instance of ArrayBuilder that optionally adds nulls
     * @return an ArrayBuilder instance
     */
    @NonNull
    public static ArrayBuilder instance(boolean addNulls) {
        return new ArrayBuilder(addNulls);
    }

    /**
     * Add an object to the array. The object is converted to a JsonValue if it isn't one already.
     * @param value the object
     * @return the builder
     */
    @NonNull
    public ArrayBuilder add(@Nullable Object value) {
        if (value == null || value == JsonValue.NULL) {
            if (addNulls) {
                theArray.add(JsonValue.NULL);
            }
        }
        else {
            theArray.add(JsonValue.instance(value));
        }
        return this;
    }

    /**
     * Add all items in the collection to the array, unless an item is null;
     * @param values the collection
     * @return the builder
     */
    @NonNull
    public ArrayBuilder addItems(@Nullable Collection<?> values) {
        if (values != null) {
            for (Object value : values) {
                add(value);
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
