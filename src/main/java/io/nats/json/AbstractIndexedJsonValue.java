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
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for JSON values that reference the original source
 * {@code char[]} and defer string copying and number parsing until accessor
 * methods are called.
 * <p>
 * Subclasses ({@link IndexedJsonValue}, {@link LazyJsonValue}) differ in
 * how they handle container types (MAP, ARRAY) but share all leaf-value
 * logic: string extraction, number parsing, and type accessors.
 *
 * @param <SELF> the concrete subclass type, enabling type-safe container accessors
 */
public abstract class AbstractIndexedJsonValue<SELF extends AbstractIndexedJsonValue<SELF>>
    implements JsonSerializable {

    /**
     * The type of this value.
     */
    @NonNull
    public final JsonValueType type;

    // Source char array and offsets for STRING / number types.
    // Package-private so subclasses (in the same package) can access for lazy resolution.
    final char[] json;
    final int start;
    final int end;
    final boolean hasEscapes;
    final boolean integersOnly;

    // Cached results (computed lazily, at most once)
    private String cachedString;
    private Number cachedNumber;
    private JsonValueType cachedNumberType;

    // ---- constructor ----

    protected AbstractIndexedJsonValue(@NonNull JsonValueType type, char[] json,
                                       int start, int end,
                                       boolean hasEscapes, boolean integersOnly) {
        this.type = type;
        this.json = json;
        this.start = start;
        this.end = end;
        this.hasEscapes = hasEscapes;
        this.integersOnly = integersOnly;
    }

    // ---- abstract methods for subclass-specific behavior ----

    /**
     * Return the singleton TRUE instance for the concrete subclass.
     */
    protected abstract SELF trueInstance();

    /**
     * Return the singleton FALSE instance for the concrete subclass.
     */
    protected abstract SELF falseInstance();

    /**
     * Get the map of children. Only valid when type is MAP.
     * @return the map, or null if this is not a MAP value
     */
    @Nullable
    public abstract Map<String, SELF> getMap();

    /**
     * Get the array of children. Only valid when type is ARRAY.
     * @return the list, or null if this is not an ARRAY value
     */
    @Nullable
    public abstract List<SELF> getArray();

    // ---- leaf accessors ----

    /**
     * Get the string value. Only valid when type is STRING.
     * Processes escape sequences on first call and caches the result.
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
     * @return the Boolean, or null if this is not a BOOL value
     */
    @Nullable
    public Boolean getBoolean() {
        if (this == trueInstance()) {
            return Boolean.TRUE;
        }
        if (this == falseInstance()) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * Get the value as an Integer. Works for INTEGER type, and for LONG type
     * if the value fits in an int.
     * @return the Integer, or null if this value is not an integer-compatible number
     */
    @Nullable
    public Integer getInteger() {
        ensureNumber();
        if (cachedNumberType == JsonValueType.INTEGER) {
            return cachedNumber.intValue();
        }
        if (cachedNumberType == JsonValueType.LONG) {
            long lv = cachedNumber.longValue();
            if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) {
                return (int) lv;
            }
        }
        return null;
    }

    /**
     * Get the value as an int, returning the default if not available.
     * @param dflt the default value
     * @return the int value or default
     */
    public int getInt(int dflt) {
        Integer val = getInteger();
        return val == null ? dflt : val;
    }

    /**
     * Get the value as a Long. Works for LONG and INTEGER types.
     * @return the Long, or null if this value is not a long-compatible number
     */
    @Nullable
    public Long getLong() {
        ensureNumber();
        if (cachedNumberType == JsonValueType.LONG || cachedNumberType == JsonValueType.INTEGER) {
            return cachedNumber.longValue();
        }
        return null;
    }

    /**
     * Get the value as a long, returning the default if not available.
     * @param dflt the default value
     * @return the long value or default
     */
    public long getLong(long dflt) {
        Long val = getLong();
        return val == null ? dflt : val;
    }

    /**
     * Get the value as a Double. Works for DOUBLE type.
     * @return the Double, or null
     */
    @Nullable
    public Double getDouble() {
        ensureNumber();
        return cachedNumberType == JsonValueType.DOUBLE ? (Double) cachedNumber : null;
    }

    /**
     * Get the value as a BigDecimal. Works for BIG_DECIMAL type.
     * @return the BigDecimal, or null
     */
    @Nullable
    public BigDecimal getBigDecimal() {
        ensureNumber();
        return cachedNumberType == JsonValueType.BIG_DECIMAL ? (BigDecimal) cachedNumber : null;
    }

    /**
     * Get the value as a BigInteger. Works for BIG_INTEGER type.
     * @return the BigInteger, or null
     */
    @Nullable
    public BigInteger getBigInteger() {
        ensureNumber();
        return cachedNumberType == JsonValueType.BIG_INTEGER ? (BigInteger) cachedNumber : null;
    }

    /**
     * Get the backing Number for any numeric type.
     * @return the Number, or null if this is not a numeric value
     */
    @Nullable
    public Number getNumber() {
        ensureNumber();
        return cachedNumber;
    }

    // ---- JsonSerializable ----

    @Override
    public @NonNull String toJson() {
        return toJsonValue().toJson();
    }

    /**
     * Convert this value to a fully materialized {@link JsonValue}.
     * Recursively materializes all children for MAP and ARRAY types.
     * @return the equivalent JsonValue
     */
    @Override
    @NonNull
    public JsonValue toJsonValue() {
        switch (type) {
            case STRING:
                return new JsonValue(getString());
            case BOOL:
                return this == trueInstance() ? JsonValue.TRUE : JsonValue.FALSE;
            case INTEGER:
            case LONG:
            case DOUBLE:
            case FLOAT:
            case BIG_DECIMAL:
            case BIG_INTEGER:
                return numberToJsonValue();
            case MAP: {
                Map<String, SELF> m = getMap();
                if (m == null || m.isEmpty()) {
                    return JsonValue.EMPTY_MAP;
                }
                java.util.HashMap<String, JsonValue> out = new java.util.HashMap<>();
                for (Map.Entry<String, SELF> e : m.entrySet()) {
                    out.put(e.getKey(), e.getValue().toJsonValue());
                }
                return new JsonValue(out);
            }
            case ARRAY: {
                List<SELF> a = getArray();
                if (a == null || a.isEmpty()) {
                    return JsonValue.EMPTY_ARRAY;
                }
                java.util.ArrayList<JsonValue> out = new java.util.ArrayList<>(a.size());
                for (SELF child : a) {
                    out.add(child.toJsonValue());
                }
                return new JsonValue(out);
            }
            default:
                return JsonValue.NULL;
        }
    }

    // ---- internal: number to JsonValue ----

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

    // ---- internal: string extraction ----

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
                            else {
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

    // ---- internal: number parsing ----

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

    private void ensureIntegerOnly() {
        int i = start;
        boolean negative = false;
        long limit = -Long.MAX_VALUE;
        if (i < end && json[i] == '-') {
            negative = true;
            limit = Long.MIN_VALUE;
            i++;
        }
        if (i == end) {
            return;
        }

        long result = 0;
        boolean overflow = false;
        long multmin = limit / 10;
        while (i < end) {
            int digit = json[i++] - '0';
            if (digit < 0 || digit > 9) {
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

    private void ensureNumberFull() {
        String val = new String(json, start, end - start);
        try {
            if (isDecimalNotation(val)) {
                try {
                    BigDecimal bd = new BigDecimal(val);
                    if (val.charAt(0) == '-' && BigDecimal.ZERO.compareTo(bd) == 0) {
                        cachedNumber = -0.0;
                        cachedNumberType = JsonValueType.DOUBLE;
                    }
                    else {
                        cachedNumber = bd;
                        cachedNumberType = JsonValueType.BIG_DECIMAL;
                    }
                }
                catch (NumberFormatException retryAsDouble) {
                    cachedNumber = Double.parseDouble(val);
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
                    cachedNumber = new BigInteger(val);
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
