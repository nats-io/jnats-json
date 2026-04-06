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
 * A deeply lazy JSON value that defers BOTH leaf materialization (strings,
 * numbers) AND structural parsing (nested objects and arrays).
 * <p>
 * When the parser encounters a nested {@code {...}} or {@code [...]}, it
 * does not recurse into it. Instead it records the byte-range offsets and
 * skips to the end via brace/bracket counting. The children are only parsed
 * one level deep when {@link #getMap()} or {@link #getArray()} is first called.
 * <p>
 * Produced by {@link LazyJsonParser}.
 */
public class LazyJsonValue {

    // ---- singletons ----

    @NonNull public static final LazyJsonValue NULL = new LazyJsonValue(JsonValueType.NULL);
    @NonNull public static final LazyJsonValue TRUE = new LazyJsonValue(JsonValueType.BOOL);
    @NonNull public static final LazyJsonValue FALSE = new LazyJsonValue(JsonValueType.BOOL);
    @NonNull public static final LazyJsonValue EMPTY_MAP = new LazyJsonValue(JsonValueType.MAP);
    @NonNull public static final LazyJsonValue EMPTY_ARRAY = new LazyJsonValue(JsonValueType.ARRAY);

    // ---- fields ----

    @NonNull public final JsonValueType type;

    // Source array and offsets.
    // For STRING / number: content between delimiters (same as IndexedJsonValue).
    // For unresolved MAP / ARRAY: content INSIDE the braces/brackets.
    private final char[] json;
    private final int start;
    private final int end;
    private final boolean hasEscapes;    // STRING only
    private final boolean integersOnly;  // number fast-path + propagated to child parsing
    private final boolean keepNulls;     // propagated to child parsing

    // Lazy-resolved containers (null until getMap()/getArray() is called)
    private Map<String, LazyJsonValue> map;
    private List<LazyJsonValue> array;
    private boolean resolved; // true for singletons, leaves, and after lazy resolution

    // Cached leaf values
    private String cachedString;
    private Number cachedNumber;
    private JsonValueType cachedNumberType;

    // ---- constructors ----

    /**
     * Leaf value (STRING or number) backed by an offset range.
     */
    LazyJsonValue(@NonNull JsonValueType type, char[] json, int start, int end,
                         boolean hasEscapes, boolean integersOnly) {
        this.type = type;
        this.json = json;
        this.start = start;
        this.end = end;
        this.hasEscapes = hasEscapes;
        this.integersOnly = integersOnly;
        this.keepNulls = false;
        this.resolved = true; // leaves are always resolved
    }

    /**
     * Unresolved container (MAP or ARRAY). The region json[start..end) is the
     * content inside the braces/brackets and will be shallow-parsed on first access.
     */
    LazyJsonValue(@NonNull JsonValueType type, char[] json, int start, int end,
                         boolean integersOnly, boolean keepNulls, @SuppressWarnings("unused") boolean containerMarker) {
        this.type = type;
        this.json = json;
        this.start = start;
        this.end = end;
        this.hasEscapes = false;
        this.integersOnly = integersOnly;
        this.keepNulls = keepNulls;
        this.resolved = false; // will be resolved on first getMap()/getArray()
    }

    /**
     * Already-resolved MAP (used by the parser for the top-level object).
     */
    LazyJsonValue(@NonNull Map<String, LazyJsonValue> map) {
        this.type = JsonValueType.MAP;
        this.json = null;
        this.start = 0;
        this.end = 0;
        this.hasEscapes = false;
        this.integersOnly = false;
        this.keepNulls = false;
        this.map = map;
        this.resolved = true;
    }

    /**
     * Already-resolved ARRAY (used by the parser for the top-level array).
     */
    LazyJsonValue(@NonNull List<LazyJsonValue> array) {
        this.type = JsonValueType.ARRAY;
        this.json = null;
        this.start = 0;
        this.end = 0;
        this.hasEscapes = false;
        this.integersOnly = false;
        this.keepNulls = false;
        this.array = array;
        this.resolved = true;
    }

    /** Singleton constructor for NULL, BOOL, EMPTY_MAP, EMPTY_ARRAY. */
    private LazyJsonValue(@NonNull JsonValueType type) {
        this.type = type;
        this.json = null;
        this.start = 0;
        this.end = 0;
        this.hasEscapes = false;
        this.integersOnly = false;
        this.keepNulls = false;
        this.resolved = true;
        if (type == JsonValueType.MAP) {
            this.map = Collections.emptyMap();
        }
        else if (type == JsonValueType.ARRAY) {
            this.array = Collections.emptyList();
        }
    }

    // ---- container accessors (trigger lazy resolution) ----

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

    private void resolve() {
        if (type == JsonValueType.MAP) {
            map = LazyJsonParser.shallowParseObject(json, start, end, keepNulls, integersOnly);
        }
        else if (type == JsonValueType.ARRAY) {
            array = LazyJsonParser.shallowParseArray(json, start, end, keepNulls, integersOnly);
        }
        resolved = true;
    }

    // ---- leaf accessors (identical to IndexedJsonValue) ----

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

    @Nullable
    public Boolean getBoolean() {
        if (this == TRUE) {
            return Boolean.TRUE;
        }
        if (this == FALSE) {
            return Boolean.FALSE;
        }
        return null;
    }

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

    public int getInt(int dflt) {
        Integer val = getInteger();
        return val == null ? dflt : val;
    }

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

    public long getLong(long dflt) {
        Long val = getLong();
        return val == null ? dflt : val;
    }

    @Nullable
    public Double getDouble() {
        ensureNumber();
        return cachedNumberType == JsonValueType.DOUBLE ? (Double) cachedNumber : null;
    }

    @Nullable
    public BigDecimal getBigDecimal() {
        ensureNumber();
        return cachedNumberType == JsonValueType.BIG_DECIMAL ? (BigDecimal) cachedNumber : null;
    }

    @Nullable
    public BigInteger getBigInteger() {
        ensureNumber();
        return cachedNumberType == JsonValueType.BIG_INTEGER ? (BigInteger) cachedNumber : null;
    }

    @Nullable
    public Number getNumber() {
        ensureNumber();
        return cachedNumber;
    }

    // ---- conversion ----

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
                Map<String, LazyJsonValue> m = getMap();
                if (m == null || m.isEmpty()) {
                    return JsonValue.EMPTY_MAP;
                }
                java.util.HashMap<String, JsonValue> out = new java.util.HashMap<>();
                for (Map.Entry<String, LazyJsonValue> e : m.entrySet()) {
                    out.put(e.getKey(), e.getValue().toJsonValue());
                }
                return new JsonValue(out);
            }
            case ARRAY: {
                List<LazyJsonValue> a = getArray();
                if (a == null || a.isEmpty()) {
                    return JsonValue.EMPTY_ARRAY;
                }
                java.util.ArrayList<JsonValue> out = new java.util.ArrayList<>(a.size());
                for (LazyJsonValue child : a) {
                    out.add(child.toJsonValue());
                }
                return new JsonValue(out);
            }
            default:
                return JsonValue.NULL;
        }
    }

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
                            else  {
                                digit = 0;
                            }
                            code = (code << 4) | digit;
                        }
                        sb.append(Character.toChars(code));
                        break;
                    case '"': case '\'': case '\\': case '/':
                        sb.append(esc); break;
                    default:
                        sb.append(esc); break;
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
            } catch (NumberFormatException e) { /* leave null */ }
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
                    } else {
                        cachedNumber = bd;
                        cachedNumberType = JsonValueType.BIG_DECIMAL;
                    }
                } catch (NumberFormatException e) {
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
                    } else {
                        cachedNumber = longVal;
                        cachedNumberType = JsonValueType.LONG;
                    }
                } catch (NumberFormatException e) {
                    cachedNumber = new BigInteger(val);
                    cachedNumberType = JsonValueType.BIG_INTEGER;
                }
            }
        } catch (NumberFormatException e) { /* leave null */ }
    }

    private static boolean isDecimalNotation(String val) {
        return val.indexOf('.') > -1 || val.indexOf('e') > -1
            || val.indexOf('E') > -1 || "-0".equals(val);
    }
}
