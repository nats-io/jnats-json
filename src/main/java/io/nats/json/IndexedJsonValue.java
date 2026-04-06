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

import java.math.BigDecimal;
import java.math.BigInteger;
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
 * so the entire tree is lazy.
 */
public class IndexedJsonValue {

    // ---- singletons for null / true / false / empty containers ----

    /**
     * Singleton for JSON null
     */
    @NonNull
    public static final IndexedJsonValue NULL = new IndexedJsonValue(JsonValueType.NULL);

    /**
     * Singleton for JSON true
     */
    @NonNull
    public static final IndexedJsonValue TRUE = new IndexedJsonValue(JsonValueType.BOOL);

    /**
     * Singleton for JSON false
     */
    @NonNull
    public static final IndexedJsonValue FALSE = new IndexedJsonValue(JsonValueType.BOOL);

    /**
     * Singleton for an empty map
     */
    @NonNull
    public static final IndexedJsonValue EMPTY_MAP = new IndexedJsonValue(JsonValueType.MAP);

    /**
     * Singleton for an empty array
     */
    @NonNull
    public static final IndexedJsonValue EMPTY_ARRAY = new IndexedJsonValue(JsonValueType.ARRAY);

    // ---- fields ----

    /**
     * The type of this value
     */
    @NonNull
    public final JsonValueType type;

    // Source char array and offsets for STRING / number types.
    // For STRING: json[start..end) is the raw content BETWEEN the quotes
    //   (escapes are NOT yet resolved).
    // For numbers: json[start..end) is the raw number text.
    private final char[] json;
    private final int start;
    private final int end;
    private final boolean hasEscapes; // only meaningful for STRING

    // When true, ensureNumber() skips decimal paths and parses longs
    // directly from the char[] without String allocation.
    private final boolean integersOnly;

    // Cached results (computed lazily, at most once)
    private String cachedString;
    private Number cachedNumber;
    private JsonValueType cachedNumberType;

    // Container children (non-null only for MAP / ARRAY)
    private final Map<String, IndexedJsonValue> map;
    private final List<IndexedJsonValue> array;

    // ---- constructors ----

    /**
     * Construct a STRING or number value backed by a region of the source array.
     */
    IndexedJsonValue(@NonNull JsonValueType type, char[] json, int start, int end, boolean hasEscapes) {
        this(type, json, start, end, hasEscapes, false);
    }

    /**
     * Construct a STRING or number value backed by a region of the source array,
     * with an optional integers-only hint for faster number parsing.
     */
    IndexedJsonValue(@NonNull JsonValueType type, char[] json, int start, int end, boolean hasEscapes, boolean integersOnly) {
        this.type = type;
        this.json = json;
        this.start = start;
        this.end = end;
        this.hasEscapes = hasEscapes;
        this.integersOnly = integersOnly;
        this.map = null;
        this.array = null;
    }

    /**
     * Construct a MAP value.
     */
    IndexedJsonValue(@NonNull Map<String, IndexedJsonValue> map) {
        this.type = JsonValueType.MAP;
        this.json = null;
        this.start = 0;
        this.end = 0;
        this.hasEscapes = false;
        this.integersOnly = false;
        this.map = map;
        this.array = null;
    }

    /**
     * Construct an ARRAY value.
     */
    IndexedJsonValue(@NonNull List<IndexedJsonValue> array) {
        this.type = JsonValueType.ARRAY;
        this.json = null;
        this.start = 0;
        this.end = 0;
        this.hasEscapes = false;
        this.integersOnly = false;
        this.map = null;
        this.array = array;
    }

    /**
     * Singleton constructor for NULL, BOOL, EMPTY_MAP, EMPTY_ARRAY.
     */
    private IndexedJsonValue(@NonNull JsonValueType type) {
        this.type = type;
        this.json = null;
        this.start = 0;
        this.end = 0;
        this.hasEscapes = false;
        this.integersOnly = false;
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

    // ---- accessors (copy-on-read) ----

    /**
     * Get the string value. Only valid when type is STRING.
     * Processes escape sequences on first call and caches the result.
     *
     * @return the string, or null if this is not a STRING value
     */
    @Nullable
    public String getString() {
        if (type != JsonValueType.STRING) {
            return null;
        }
        if (cachedString == null) {
            cachedString = extractString();
        }
        return cachedString;
    }

    /**
     * Get the boolean value. Only valid when type is BOOL.
     *
     * @return the Boolean, or null if this is not a BOOL value
     */
    @Nullable
    public Boolean getBoolean() {
        if (this == IndexedJsonValue.TRUE) {
            return Boolean.TRUE;
        }
        if (this == IndexedJsonValue.FALSE) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * Get the value as an Integer. Works for INTEGER type, and for LONG type
     * if the value fits in an int.
     *
     * @return the Integer, or null if this value is not an integer-compatible number
     */
    @Nullable
    public Integer getInteger() {
        ensureNumber();
        if (cachedNumberType == JsonValueType.INTEGER) {
            return (Integer) cachedNumber;
        }
        if (cachedNumberType == JsonValueType.LONG) {
            long lv = (Long) cachedNumber;
            if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) {
                return (int) lv;
            }
        }
        return null;
    }

    /**
     * Get the value as an int, returning the default if not available.
     *
     * @param dflt the default value
     * @return the int value or default
     */
    public int getInt(int dflt) {
        Integer val = getInteger();
        return val == null ? dflt : val;
    }

    /**
     * Get the value as a Long. Works for LONG and INTEGER types.
     *
     * @return the Long, or null if this value is not a long-compatible number
     */
    @Nullable
    public Long getLong() {
        ensureNumber();
        if (cachedNumberType == JsonValueType.LONG) {
            return (Long) cachedNumber;
        }
        if (cachedNumberType == JsonValueType.INTEGER) {
            return ((Integer) cachedNumber).longValue();
        }
        return null;
    }

    /**
     * Get the value as a long, returning the default if not available.
     *
     * @param dflt the default value
     * @return the long value or default
     */
    public long getLong(long dflt) {
        Long val = getLong();
        return val == null ? dflt : val;
    }

    /**
     * Get the value as a Double. Works for DOUBLE type.
     *
     * @return the Double, or null
     */
    @Nullable
    public Double getDouble() {
        ensureNumber();
        return cachedNumberType == JsonValueType.DOUBLE ? (Double) cachedNumber : null;
    }

    /**
     * Get the value as a BigDecimal. Works for BIG_DECIMAL type.
     *
     * @return the BigDecimal, or null
     */
    @Nullable
    public BigDecimal getBigDecimal() {
        ensureNumber();
        return cachedNumberType == JsonValueType.BIG_DECIMAL ? (BigDecimal) cachedNumber : null;
    }

    /**
     * Get the value as a BigInteger. Works for BIG_INTEGER type.
     *
     * @return the BigInteger, or null
     */
    @Nullable
    public BigInteger getBigInteger() {
        ensureNumber();
        return cachedNumberType == JsonValueType.BIG_INTEGER ? (BigInteger) cachedNumber : null;
    }

    /**
     * Get the backing Number for any numeric type.
     *
     * @return the Number, or null if this is not a numeric value
     */
    @Nullable
    public Number getNumber() {
        ensureNumber();
        return cachedNumber;
    }

    /**
     * Get the map of children. Only valid when type is MAP.
     *
     * @return the map, or null if this is not a MAP value
     */
    @Nullable
    public Map<String, IndexedJsonValue> getMap() {
        return map;
    }

    /**
     * Get the array of children. Only valid when type is ARRAY.
     *
     * @return the list, or null if this is not an ARRAY value
     */
    @Nullable
    public List<IndexedJsonValue> getArray() {
        return array;
    }

    /**
     * Convert this indexed value to a fully materialized {@link JsonValue}.
     * This recursively materializes all children for MAP and ARRAY types.
     *
     * @return the equivalent JsonValue
     */
    @NonNull
    public JsonValue toJsonValue() {
        switch (type) {
            case STRING:
                return new JsonValue(getString());
            case BOOL:
                return this == TRUE ? JsonValue.TRUE : JsonValue.FALSE;
            case INTEGER:
            case LONG:
            case DOUBLE:
            case FLOAT:
            case BIG_DECIMAL:
            case BIG_INTEGER:
                return numberToJsonValue();
            case MAP: {
                if (map == null || map.isEmpty()) {
                    return JsonValue.EMPTY_MAP;
                }
                java.util.HashMap<String, JsonValue> m = new java.util.HashMap<>();
                for (Map.Entry<String, IndexedJsonValue> e : map.entrySet()) {
                    m.put(e.getKey(), e.getValue().toJsonValue());
                }
                return new JsonValue(m);
            }
            case ARRAY: {
                if (array == null || array.isEmpty()) {
                    return JsonValue.EMPTY_ARRAY;
                }
                java.util.ArrayList<JsonValue> a = new java.util.ArrayList<>(array.size());
                for (IndexedJsonValue child : array) {
                    a.add(child.toJsonValue());
                }
                return new JsonValue(a);
            }
            default:
                return JsonValue.NULL;
        }
    }

    /**
     * Convert the number value to the correct JsonValue based on the
     * actual parsed number type (which may differ from the initial type
     * marker assigned during indexing).
     */
    private JsonValue numberToJsonValue() {
        ensureNumber();
        if (cachedNumberType == null) {
            return JsonValue.NULL;
        }
        switch (cachedNumberType) {
            case INTEGER:     return new JsonValue((int) cachedNumber);
            case LONG:        return new JsonValue((long) cachedNumber);
            case DOUBLE:      return new JsonValue((double) cachedNumber);
            case FLOAT:       return new JsonValue((float) cachedNumber);
            case BIG_DECIMAL: return new JsonValue((BigDecimal) cachedNumber);
            case BIG_INTEGER: return new JsonValue((BigInteger) cachedNumber);
            default:          return JsonValue.NULL;
        }
    }

    // ---- internal helpers ----

    /**
     * Extract the string from the source array, processing escape sequences.
     */
    private String extractString() {
        if (json == null) {
            return "";
        }
        if (!hasEscapes) {
            return new String(json, start, end - start);
        }
        StringBuilder sb = new StringBuilder(end - start);
        int i = start;
        while (i < end) {
            char c = json[i++];
            if (c == '\\' && i < end) {
                char esc = json[i++];
                switch (esc) {
                    case 'b':  sb.append('\b'); break;
                    case 't':  sb.append('\t'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'r':  sb.append('\r'); break;
                    case 'u':
                        int code = 0;
                        for (int j = 0; j < 4 && i < end; j++) {
                            char h = json[i++];
                            int digit;
                            if (h >= '0' && h <= '9') {
                                digit = h - '0';
                            }
                            else if (h >= 'A' && h <= 'F') {
                                digit = h - 'A' + 10;
                            }
                            else if (h >= 'a' && h <= 'f') {
                                digit = h - 'a' + 10;
                            }
                            else  { // shouldn't happen; parser validated
                                digit = 0;
                            }
                            code = (code << 4) | digit;
                        }
                        sb.append(Character.toChars(code));
                        break;
                    case '"':
                    case '\'':
                    case '\\':
                    case '/':
                        sb.append(esc);
                        break;
                    default:
                        sb.append(esc);
                        break;
                }
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Parse the number from the source array on first access.
     */
    private void ensureNumber() {
        if (cachedNumberType != null) {
            return;
        }
        if (json == null || type == JsonValueType.STRING || type == JsonValueType.BOOL
            || type == JsonValueType.MAP || type == JsonValueType.ARRAY || type == JsonValueType.NULL) {
            return;
        }
        if (integersOnly) {
            ensureIntegerOnly();
        }
        else {
            ensureNumberFull();
        }
    }

    /**
     * Fast path: parse a long directly from the char[], no String allocation.
     * Falls back to BigInteger only if the value overflows long.
     * Uses the same negation-safe approach as Long.parseLong: accumulate
     * as a negative number to handle Long.MIN_VALUE correctly.
     */
    private void ensureIntegerOnly() {
        int i = start;
        boolean negative = false;
        long limit = -Long.MAX_VALUE;
        if (i < end && json[i] == '-') {
            negative = true;
            limit = Long.MIN_VALUE;
            i++;
        }
        if (i == end) { // shouldn't happen, parser validated
            return;
        }

        // Parse digits — accumulate as negative to avoid overflow on Long.MIN_VALUE
        long result = 0;
        boolean overflow = false;
        long multmin = limit / 10;
        while (i < end) {
            int digit = json[i++] - '0';
            if (digit < 0 || digit > 9) {
                // Not a pure integer — fall back to full parsing
                ensureNumberFull();
                return;
            }
            if (result < multmin) {
                overflow = true;
                break;
            }
            result *= 10;
            if (result < limit + digit) {
                overflow = true;
                break;
            }
            result -= digit;
        }

        if (overflow) {
            // Very large number — fall back to BigInteger via String
            String val = new String(json, start, end - start);
            try {
                cachedNumber = new BigInteger(val);
                cachedNumberType = JsonValueType.BIG_INTEGER;
            }
            catch (NumberFormatException e) {
                // leave cached as null
            }
            return;
        }

        long longVal = negative ? result : -result;
        if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
            cachedNumber = (int) longVal;
            cachedNumberType = JsonValueType.INTEGER;
        }
        else {
            cachedNumber = longVal;
            cachedNumberType = JsonValueType.LONG;
        }
    }

    /**
     * Full path: handles decimals, scientific notation, BigDecimal, etc.
     */
    private void ensureNumberFull() {
        String val = new String(json, start, end - start);
        try {
            if (isDecimalNotation(val)) {
                try {
                    BigDecimal bd = new BigDecimal(val);
                    char initial = val.charAt(0);
                    if (initial == '-' && BigDecimal.ZERO.compareTo(bd) == 0) {
                        cachedNumber = -0.0;
                        cachedNumberType = JsonValueType.DOUBLE;
                    }
                    else {
                        cachedNumber = bd;
                        cachedNumberType = JsonValueType.BIG_DECIMAL;
                    }
                }
                catch (NumberFormatException retryAsDouble) {
                    double d = Double.parseDouble(val);
                    cachedNumber = d;
                    cachedNumberType = JsonValueType.DOUBLE;
                }
            }
            else {
                try {
                    long longVal = Long.parseLong(val);
                    if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
                        cachedNumber = (int) longVal;
                        cachedNumberType = JsonValueType.INTEGER;
                    }
                    else {
                        cachedNumber = longVal;
                        cachedNumberType = JsonValueType.LONG;
                    }
                }
                catch (NumberFormatException e) {
                    BigInteger bi = new BigInteger(val);
                    cachedNumber = bi;
                    cachedNumberType = JsonValueType.BIG_INTEGER;
                }
            }
        }
        catch (NumberFormatException e) {
            // leave cached as null
        }
    }

    private static boolean isDecimalNotation(String val) {
        return val.indexOf('.') > -1 || val.indexOf('e') > -1
            || val.indexOf('E') > -1 || "-0".equals(val);
    }
}
