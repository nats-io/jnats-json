// Copyright 2023-2026 The NATS Authors
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;

import static io.nats.json.Encoding.jsonEncode;

/**
 * An object representing a JSON value
 */
public class JsonValue implements JsonSerializable {

    /**
     * A JsonValue for JsonValueType.NULL
     */
    @NonNull
    public static final JsonValue NULL = new JsonValue(JsonValueType.NULL);

    /**
     * A JsonValue for boolean true
     */
    @NonNull
    public static final JsonValue TRUE = new JsonValue(true);

    /**
     * A JsonValue for boolean false
     */
    @NonNull
    public static final JsonValue FALSE = new JsonValue(false);

    /**
     * The backing map for an {@code EMPTY_MAP}
     */
    @NonNull
    public static final Map<String, JsonValue> EMPTY_MAP_MAP = Collections.unmodifiableMap(new HashMap<>());

    /**
     * A JsonValue representing an object that is an empty map
     */
    @NonNull
    public static final JsonValue EMPTY_MAP = new JsonValue(JsonValueType.MAP);

    /**
     * The backing array for an {@code EMPTY_ARRAY}
     */
    @NonNull
    public static final List<JsonValue> EMPTY_ARRAY_LIST = Collections.unmodifiableList(new ArrayList<>());

    /**
     * A JsonValue representing an object that is an empty array
     */
    @NonNull
    public static final JsonValue EMPTY_ARRAY = new JsonValue(JsonValueType.ARRAY);

    private static final char QUOTE = '"';
    private static final char COMMA = ',';
    private static final String NULL_STR = "null";

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.STRING}
     */
    @Nullable
    public final String string;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.BOOL}
     */
    @Nullable
    public final Boolean bool;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.INTEGER}
     */
    @Nullable
    public final Integer i;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.LONG}
     */
    @Nullable
    public final Long l;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.DOUBLE}
     */
    @Nullable
    public final Double d;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.STRING}
     */
    @Nullable
    public final Float f;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.BIG_DECIMAL}
     */
    @Nullable
    public final BigDecimal bd;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.BIG_INTEGER}
     */
    @Nullable
    public final BigInteger bi;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.MAP}
     */
    @Nullable
    public final Map<String, JsonValue> map;

    /**
     * The typed backing object when the JsonValue is of {@code JsonValueType.ARRAY}
     */
    @Nullable
    public final List<JsonValue> array;

    /**
     * The JsonValueType of the object
     */
    @NonNull
    public final JsonValueType type;

    /**
     * The backing object for any type. Will be null for JsonValueType.NULL
     */
    @Nullable
    public final Object object;

    /**
     * The backing object when the type is any Number type
     */
    @Nullable
    public final Number number;

    /**
     * A list of field names used for ordering the fields when resolving toJson,
     * when the object is a map
     */
    @NonNull
    public final List<String> mapOrder;

    /**
     * Convert an object to a JsonValue
     * @param o the object
     * @return a JsonValue
     */
    public static JsonValue instance(Object o) {
        if (o == null) {
            return JsonValue.NULL;
        }
        if (o instanceof JsonValue) {
            return (JsonValue)o;
        }
        if (o instanceof JsonSerializable) {
            return ((JsonSerializable)o).toJsonValue();
        }
        if (o instanceof Map<?, ?>) {
            return _instance((Map<?, ?>)o);
        }
        if (o instanceof Collection<?>) {
            return _instance((Collection<?>)o);
        }
        if (o instanceof String) {
            String s = ((String)o).trim();
            return new JsonValue(s);
        }
        if (o instanceof Boolean) {
            return new JsonValue((Boolean)o);
        }
        if (o instanceof Integer) {
            return new JsonValue((Integer)o);
        }
        if (o instanceof Long) {
            return new JsonValue((Long)o);
        }
        if (o instanceof Double) {
            return new JsonValue((Double)o);
        }
        if (o instanceof Float) {
            return new JsonValue((Float)o);
        }
        if (o instanceof BigDecimal) {
            return new JsonValue((BigDecimal)o);
        }
        if (o instanceof BigInteger) {
            return new JsonValue((BigInteger)o);
        }
        if (o instanceof Duration) {
            return new JsonValue(((Duration)o).toNanos());
        }
        return new JsonValue(o.toString());
    }

    private static JsonValue _instance(Collection<?> list) {
        List<JsonValue> jv = new ArrayList<>();
        for (Object o : list) {
            jv.add(JsonValue.instance(o));
        }
        return new JsonValue(jv);
    }

    private static JsonValue _instance(Map<?, ?> map) {
        Map<String, JsonValue> jv = new HashMap<>();
        for(Map.Entry<?, ?> entry : map.entrySet()) {
            jv.put(entry.getKey().toString(), JsonValue.instance(entry.getValue()));
        }
        return new JsonValue(jv);
    }

    /**
     * Create a JsonValue from a string
     * @param string the string
     */
    public JsonValue(String string) {
        this(string, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Create a JsonValue from a character. It becomes {@code JsonValueType.STRING}
     * @param c the character
     */
    public JsonValue(char c) {
        this("" + c, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Create a JsonValue from a boolean
     * @param bool the boolean
     */
    public JsonValue(Boolean bool) {
        this(null, bool, null, null, null, null, null, null, null, null);
    }

    /**
     * Create a JsonValue from an int
     * @param i the int
     */
    public JsonValue(int i) {
        this(null, null, i, null, null, null, null, null, null, null);
    }

    /**
     * Create a JsonValue from a long
     * @param l the long
     */
    public JsonValue(long l) {
        this(null, null, null, l, null, null, null, null, null, null);
    }

    /**
     * Create a JsonValue from a double
     * @param d the double
     */
    public JsonValue(double d) {
        this(null, null, null, null, d, null, null, null, null, null);
    }

    /**
     * Create a JsonValue from a float
     * @param f the float
     */
    public JsonValue(float f) {
        this(null, null, null, null, null, f, null, null, null, null);
    }

    /**
     * Create a JsonValue from a string
     * @param bd the BigDecimal
     */
    public JsonValue(BigDecimal bd) {
        this(null, null, null, null, null, null, bd, null, null, null);
    }

    /**
     * Create a JsonValue from a string
     * @param bi the Bignteger
     */
    public JsonValue(BigInteger bi) {
        this(null, null, null, null, null, null, null, bi, null, null);
    }

    /**
     * Create a JsonValue from a map This becomes a {@link JsonValueType#MAP}
     * @param map the map
     */
    public JsonValue(Map<String, JsonValue> map) {
        this(null, null, null, null, null, null, null, null, map, null);
    }

    /**
     * Create a JsonValue from a List. This becomes a {@link JsonValueType#ARRAY}
     * @param collection the collection
     */
    public JsonValue(List<JsonValue> collection) {
        this(null, null, null, null, null, null, null, null, null, collection);
    }

    /**
     * Create a JsonValue from an array
     * @param array the array
     */
    public JsonValue(JsonValue[] array) {
        this(null, null, null, null, null, null, null, null, null, array == null ? null : Arrays.asList(array));
    }

    private JsonValue(@Nullable String string,
                      @Nullable Boolean bool,
                      @Nullable Integer i,
                      @Nullable Long l,
                      @Nullable Double d,
                      @Nullable Float f,
                      @Nullable BigDecimal bd,
                      @Nullable BigInteger bi,
                      @Nullable Map<String, JsonValue> map,
                      @Nullable List<JsonValue> array)
    {
        this.map = map;
        this.mapOrder = new ArrayList<>();
        this.array = array;
        this.string = string;
        this.bool = bool;
        this.i = i;
        this.l = l;
        this.d = d;
        this.f = f;
        this.bd = bd;
        this.bi = bi;
        if (i != null) {
            this.type = JsonValueType.INTEGER;
            number = i;
            object = number;
        }
        else if (l != null) {
            this.type = JsonValueType.LONG;
            number = l;
            object = number;
        }
        else if (d != null) {
            this.type = JsonValueType.DOUBLE;
            number = this.d;
            object = number;
        }
        else if (f != null) {
            this.type = JsonValueType.FLOAT;
            number = this.f;
            object = number;
        }
        else if (bd != null) {
            this.type = JsonValueType.BIG_DECIMAL;
            number = this.bd;
            object = number;
        }
        else if (bi != null) {
            this.type = JsonValueType.BIG_INTEGER;
            number = this.bi;
            object = number;
        }
        else {
            number = null;
            if (map != null) {
                this.type = JsonValueType.MAP;
                object = map;
            }
            else if (string != null) {
                this.type = JsonValueType.STRING;
                object = string;
            }
            else if (bool != null) {
                this.type = JsonValueType.BOOL;
                object = bool;
            }
            else if (array != null) {
                this.type = JsonValueType.ARRAY;
                object = array;
            }
            else {
                this.type = JsonValueType.NULL;
                object = null;
            }
        }
    }

    /**
     * Special internal constructor for empty and null
     * @param type the type;
     */
    private JsonValue(@NonNull JsonValueType type) {
        this.type = type;

        string = null;
        bool = null;
        i = null;
        l = null;
        d = null;
        f = null;
        bd = null;
        bi = null;
        number = null;
        mapOrder = new ArrayList<>();

        if (type == JsonValueType.MAP) {
            map = EMPTY_MAP_MAP;
            array = null;
            object = map;
        }
        else if (type == JsonValueType.ARRAY) {
            map = null;
            array = EMPTY_ARRAY_LIST;
            object = array;
        }
        else { // JsonValueType.NULL
            map = null;
            array = null;
            object = null;
        }
    }

    public void setMapOrder(String... keys) {
        if (type != JsonValueType.MAP) {
            throw new IllegalStateException("JsonValue does not represent a map.");
        }
        mapOrder.clear();
        if (keys != null) {
            Collections.addAll(mapOrder, keys);
        }
    }

    public void setMapOrder(List<String> keys) {
        if (type != JsonValueType.MAP) {
            throw new IllegalStateException("JsonValue does not represent a map.");
        }
        mapOrder.clear();
        if (keys != null) {
            mapOrder.addAll(keys);
        }
    }

    public void addMapOrder(String key) {
        if (type != JsonValueType.MAP) {
            throw new IllegalStateException("JsonValue does not represent a map.");
        }
        mapOrder.add(key);
    }

    /**
     * Create a JSON string using the class simple name as the key for the entire object
     * @param c the class
     * @return the JSON string
     */
    public String toString(Class<?> c) {
        return JsonWriteUtils.toKey(c) + toJson();
    }

    /**
     * Create a JSON string using the class simple name as the key for the entire object
     * @param key the field name
     * @return the JSON string
     */
    public String toString(String key) {
        return JsonWriteUtils.toKey(key) + toJson();
    }

    @Override
    public String toString() {
        return toJson();
    }

    @Override
    @NonNull
    public JsonValue toJsonValue() {
        return this;
    }

    @SuppressWarnings("DataFlowIssue") // we check the type and know the backing item is not null
    @Override
    @NonNull
    public String toJson() {
        switch (type) {
            case STRING:      return QUOTE + jsonEncode(string) + QUOTE;
            case BOOL:        return Boolean.toString(bool).toLowerCase();
            case MAP:         return mapString();
            case ARRAY:       return listString();
            case INTEGER:     return i.toString();
            case LONG:        return l.toString();
            case DOUBLE:      return d.toString();
            case FLOAT:       return f.toString();
            case BIG_DECIMAL: return bd.toString();
            case BIG_INTEGER: return bi.toString();
            default:          return NULL_STR;
        }
    }

    @SuppressWarnings("DataFlowIssue") // by checking the type we know that the map is not null
    private String mapString() {
        StringBuilder sbo = JsonWriteUtils.beginJson();
        if (!mapOrder.isEmpty()) {
            for (String key : mapOrder) {
                JsonWriteUtils.addField(sbo, key, map.get(key));
            }
        }
        else {
            for (String key : map.keySet()) {
                JsonWriteUtils.addField(sbo, key, map.get(key));
            }
        }
        return JsonWriteUtils.endJson(sbo).toString();
    }

    @SuppressWarnings("DataFlowIssue") // by checking the type we know that the list is not null
    private String listString() {
        StringBuilder sba = JsonWriteUtils.beginArray();
        for (JsonValue v : array) {
            sba.append(v.toJson());
            sba.append(COMMA);
        }
        return JsonWriteUtils.endArray(sba).toString();
    }

    @SuppressWarnings("DataFlowIssue") // by checking the type we know that the value is not null
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonValue jsonValue = (JsonValue) o;

        if (type != jsonValue.type) return false;

        switch (type) {
            case STRING: return string.equals(jsonValue.string);
            case BOOL: return bool.equals(jsonValue.bool);
            case INTEGER: return i.equals(jsonValue.i);
            case LONG: return l.equals(jsonValue.l);
            case DOUBLE: return d.equals(jsonValue.d);
            case FLOAT: return f.equals(jsonValue.f);
            case BIG_DECIMAL: return bd.equals(jsonValue.bd);
            case BIG_INTEGER: return bi.equals(jsonValue.bi);
            case MAP: return map.equals(jsonValue.map);
            case ARRAY: return array.equals(jsonValue.array);
            case NULL: return true; // null has null value, type was already checked.
        }
        return false;
    }

    @SuppressWarnings("DataFlowIssue") // by checking the type we know that the value is not null
    @Override
    public int hashCode() {

        int hc = 0;
        switch (type) {
            case STRING: hc = string.hashCode(); break;
            case BOOL: hc = bool.hashCode(); break;
            case INTEGER: hc = i.hashCode(); break;
            case LONG: hc = l.hashCode(); break;
            case DOUBLE: hc = d.hashCode(); break;
            case FLOAT: hc = f.hashCode(); break;
            case BIG_DECIMAL: hc = bd.hashCode(); break;
            case BIG_INTEGER: hc = bi.hashCode(); break;
            case MAP: hc = map.hashCode(); break;
            case ARRAY: hc = array.hashCode(); break;
        }
        return 31 * hc + type.hashCode();
    }
}
