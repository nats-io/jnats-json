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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Utility methods for reading typed values from {@link IndexedJsonValue} maps.
 * This is the indexed counterpart of {@link JsonValueUtils} — the API mirrors
 * it closely so that callers can switch between eager and lazy parsing with
 * minimal code changes.
 * <p>
 * Values are only materialized (string copied, number parsed) at the point
 * where the read method is called.
 */
public abstract class IndexedJsonValueUtils {

    private IndexedJsonValueUtils() {} /* ensures cannot be constructed */

    // ---- generic read helpers ----

    /**
     * Read a key's value without assuming its type.
     */
    @Nullable
    public static IndexedJsonValue readValue(@Nullable IndexedJsonValue jv, @NonNull String key) {
        if (jv == null || jv.getMap() == null) {
            return null;
        }
        return jv.getMap().get(key);
    }

    /**
     * Read a value generically with a supplier.
     */
    @Nullable
    public static <T> T read(@Nullable IndexedJsonValue jv, @NonNull String key, @Nullable T dflt,
                             @NonNull Function<IndexedJsonValue, T> valueSupplier) {
        if (jv == null || jv.getMap() == null) {
            return dflt;
        }
        IndexedJsonValue v = jv.getMap().get(key);
        return v == null ? dflt : valueSupplier.apply(v);
    }

    /**
     * Read a value generically with a required type and a supplier.
     */
    @Nullable
    public static <T> T read(@Nullable IndexedJsonValue jv, @NonNull String key, @Nullable T dflt,
                             @NonNull JsonValueType requiredType,
                             @NonNull Function<IndexedJsonValue, T> valueSupplier) {
        if (jv == null || jv.getMap() == null) {
            return dflt;
        }
        IndexedJsonValue v = jv.getMap().get(key);
        return v == null || v.type != requiredType ? dflt : valueSupplier.apply(v);
    }

    // ---- String ----

    /**
     * Read a key's string value. Returns null if not found or not a STRING.
     */
    @Nullable
    public static String readString(@Nullable IndexedJsonValue jv, @NonNull String key) {
        return read(jv, key, null, IndexedJsonValue::getString);
    }

    /**
     * Read a key's string value with a default.
     */
    @Nullable
    public static String readString(@Nullable IndexedJsonValue jv, @NonNull String key, @Nullable String dflt) {
        return read(jv, key, dflt, JsonValueType.STRING, IndexedJsonValue::getString);
    }

    // ---- Integer ----

    /**
     * Read a key's int value. Accepts INTEGER and LONG (if in int range).
     */
    @Nullable
    public static Integer readInteger(@Nullable IndexedJsonValue jv, @NonNull String key) {
        return read(jv, key, null, IndexedJsonValue::getInteger);
    }

    /**
     * Read a key's int value with a default.
     */
    public static int readInteger(@Nullable IndexedJsonValue jv, @NonNull String key, int dflt) {
        Integer i = readInteger(jv, key);
        return i == null ? dflt : i;
    }

    // ---- Long ----

    /**
     * Read a key's long value. Accepts INTEGER and LONG types.
     */
    @Nullable
    public static Long readLong(@Nullable IndexedJsonValue jv, @NonNull String key) {
        return read(jv, key, null, IndexedJsonValue::getLong);
    }

    /**
     * Read a key's long value with a default.
     */
    public static long readLong(@Nullable IndexedJsonValue jv, @NonNull String key, long dflt) {
        Long l = readLong(jv, key);
        return l == null ? dflt : l;
    }

    // ---- Boolean ----

    /**
     * Read a key's boolean value. Returns null if not found or not BOOL.
     */
    @Nullable
    public static Boolean readBoolean(@Nullable IndexedJsonValue jv, @NonNull String key) {
        return read(jv, key, null, IndexedJsonValue::getBoolean);
    }

    /**
     * Read a key's boolean value with a default.
     */
    public static boolean readBoolean(@Nullable IndexedJsonValue jv, @NonNull String key, boolean dflt) {
        Boolean b = readBoolean(jv, key);
        return b == null ? dflt : b;
    }

    // ---- Date / Duration ----

    /**
     * Read a key's string value and parse it as a ZonedDateTime.
     */
    @Nullable
    public static ZonedDateTime readDate(@Nullable IndexedJsonValue jv, @NonNull String key) {
        String s = readString(jv, key);
        return s == null ? null : DateTimeUtils.parseDateTimeThrowParseError(s);
    }

    /**
     * Read a key's integer/long value and convert to a Duration (nanoseconds).
     */
    @Nullable
    public static Duration readNanosAsDuration(@Nullable IndexedJsonValue jv, @NonNull String key) {
        Long l = readLong(jv, key);
        return l == null ? null : Duration.ofNanos(l);
    }

    /**
     * Read a key's integer/long value and convert to a Duration (nanoseconds) with a default.
     */
    @Nullable
    public static Duration readNanosAsDuration(@Nullable IndexedJsonValue jv, @NonNull String key, Duration dflt) {
        Long l = readLong(jv, key);
        return l == null ? dflt : Duration.ofNanos(l);
    }

    // ---- Bytes ----

    /**
     * Read a key's string value as UTF-8 bytes.
     */
    public static byte @Nullable [] readBytes(@Nullable IndexedJsonValue jv, @NonNull String key) {
        String s = readString(jv, key);
        return s == null ? null : s.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Read a key's string value as bytes in the given charset.
     */
    public static byte @Nullable [] readBytes(@Nullable IndexedJsonValue jv, @NonNull String key, Charset charset) {
        String s = readString(jv, key);
        return s == null ? null : s.getBytes(charset);
    }

    /**
     * Read a key's string value and decode from basic base64.
     */
    public static byte @Nullable [] readBase64Basic(@Nullable IndexedJsonValue jv, @NonNull String key) {
        String b64 = readString(jv, key);
        return b64 == null ? null : Encoding.base64BasicDecode(b64);
    }

    /**
     * Read a key's string value and decode from URL-safe base64.
     */
    public static byte @Nullable [] readBase64Url(@Nullable IndexedJsonValue jv, @NonNull String key) {
        String b64 = readString(jv, key);
        return b64 == null ? null : Encoding.base64UrlDecode(b64);
    }

    // ---- Map ----

    /**
     * Read a nested map value as an IndexedJsonValue, or null.
     */
    @Nullable
    public static IndexedJsonValue readMapObjectOrNull(@Nullable IndexedJsonValue jv, @NonNull String key) {
        return read(jv, key, null, JsonValueType.MAP, v -> v);
    }

    /**
     * Read a nested map value as an IndexedJsonValue, or EMPTY_MAP.
     */
    @NonNull
    public static IndexedJsonValue readMapObjectOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key) {
        IndexedJsonValue jvv = readMapObjectOrNull(jv, key);
        return jvv == null ? IndexedJsonValue.EMPTY_MAP : jvv;
    }

    /**
     * Read a nested map as a Map of String to IndexedJsonValue, or null.
     */
    @Nullable
    public static Map<String, IndexedJsonValue> readMapMapOrNull(@Nullable IndexedJsonValue jv, @NonNull String key) {
        IndexedJsonValue jvv = readMapObjectOrNull(jv, key);
        return jvv == null ? null : jvv.getMap();
    }

    /**
     * Read a nested map as a Map of String to IndexedJsonValue, or empty map.
     */
    @NonNull
    public static Map<String, IndexedJsonValue> readMapMapOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key) {
        Map<String, IndexedJsonValue> map = readMapMapOrNull(jv, key);
        return map == null ? Collections.emptyMap() : map;
    }

    /**
     * Read a nested map as a Map of String to String, or null.
     */
    @Nullable
    public static Map<String, String> readStringMapOrNull(@Nullable IndexedJsonValue jv, @NonNull String key) {
        Map<String, IndexedJsonValue> map = readMapMapOrNull(jv, key);
        return map == null ? null : convertToStringMap(map);
    }

    /**
     * Read a nested map as a Map of String to String, or empty map.
     */
    @NonNull
    public static Map<String, String> readStringMapOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key) {
        Map<String, IndexedJsonValue> map = readMapMapOrEmpty(jv, key);
        return convertToStringMap(map);
    }

    /**
     * Convert a map of IndexedJsonValue to a map of String, filtering non-STRING values.
     */
    @NonNull
    public static Map<String, String> convertToStringMap(@NonNull Map<String, IndexedJsonValue> map) {
        Map<String, String> temp = new HashMap<>();
        for (Map.Entry<String, IndexedJsonValue> entry : map.entrySet()) {
            if (entry.getValue().type == JsonValueType.STRING) {
                temp.put(entry.getKey(), entry.getValue().getString());
            }
        }
        return temp;
    }

    // ---- Array ----

    /**
     * Read a key's array value, or null.
     */
    @Nullable
    public static List<IndexedJsonValue> readArrayOrNull(@Nullable IndexedJsonValue jv, @NonNull String key) {
        return read(jv, key, null, JsonValueType.ARRAY, IndexedJsonValue::getArray);
    }

    /**
     * Read a key's array value, or empty list.
     */
    @NonNull
    public static List<IndexedJsonValue> readArrayOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key) {
        List<IndexedJsonValue> list = readArrayOrNull(jv, key);
        return list == null ? Collections.emptyList() : list;
    }

    // ---- String list ----

    @Nullable
    public static List<String> readStringListOrNull(@Nullable IndexedJsonValue jv, @NonNull String key) {
        return readStringListOrNull(jv, key, false);
    }

    @Nullable
    public static List<String> readStringListOrNull(@Nullable IndexedJsonValue jv, @NonNull String key, boolean ignoreBlank) {
        List<IndexedJsonValue> list = readArrayOrNull(jv, key);
        if (list == null) {
            return null;
        }
        List<String> strings = convertToStringList(list, ignoreBlank);
        return strings.isEmpty() ? null : strings;
    }

    @NonNull
    public static List<String> readStringListOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key) {
        List<IndexedJsonValue> source = readArrayOrNull(jv, key);
        return source == null ? Collections.emptyList() : convertToStringList(source, false);
    }

    @NonNull
    public static List<String> readStringListOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key, boolean ignoreBlank) {
        List<IndexedJsonValue> source = readArrayOrNull(jv, key);
        return source == null ? Collections.emptyList() : convertToStringList(source, ignoreBlank);
    }

    @NonNull
    public static List<String> convertToStringList(@NonNull List<IndexedJsonValue> source, boolean ignoreBlank) {
        List<String> result = new ArrayList<>();
        for (IndexedJsonValue v : source) {
            String s = v.getString();
            if (s != null) {
                if (ignoreBlank) {
                    if (!s.trim().isEmpty()) {
                        result.add(s);
                    }
                }
                else {
                    result.add(s);
                }
            }
        }
        return result;
    }

    // ---- Integer list ----

    @Nullable
    public static List<Integer> readIntegerListOrNull(@Nullable IndexedJsonValue jv, @NonNull String key) {
        List<IndexedJsonValue> source = readArrayOrNull(jv, key);
        if (source == null) {
            return null;
        }
        List<Integer> list = convertToIntegerList(source);
        return list.isEmpty() ? null : list;
    }

    @NonNull
    public static List<Integer> readIntegerListOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key) {
        List<IndexedJsonValue> source = readArrayOrNull(jv, key);
        return source == null ? Collections.emptyList() : convertToIntegerList(source);
    }

    @NonNull
    public static List<Integer> convertToIntegerList(@NonNull List<IndexedJsonValue> source) {
        List<Integer> result = new ArrayList<>();
        for (IndexedJsonValue v : source) {
            Integer i = v.getInteger();
            if (i != null) {
                result.add(i);
            }
        }
        return result;
    }

    // ---- Long list ----

    @Nullable
    public static List<Long> readLongListOrNull(@Nullable IndexedJsonValue jv, @NonNull String key) {
        List<IndexedJsonValue> source = readArrayOrNull(jv, key);
        if (source == null) {
            return null;
        }
        List<Long> list = convertToLongList(source);
        return list.isEmpty() ? null : list;
    }

    @NonNull
    public static List<Long> readLongListOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key) {
        List<IndexedJsonValue> source = readArrayOrNull(jv, key);
        return source == null ? Collections.emptyList() : convertToLongList(source);
    }

    @NonNull
    public static List<Long> convertToLongList(@NonNull List<IndexedJsonValue> source) {
        List<Long> result = new ArrayList<>();
        for (IndexedJsonValue v : source) {
            Long l = v.getLong();
            if (l != null) {
                result.add(l);
            }
        }
        return result;
    }

    // ---- Duration list ----

    @Nullable
    public static List<Duration> readNanosAsDurationListOrNull(@Nullable IndexedJsonValue jv, @NonNull String key) {
        List<Long> source = readLongListOrNull(jv, key);
        return source == null ? null : convertNanosToDuration(source);
    }

    @NonNull
    public static List<Duration> readNanosAsDurationListOrEmpty(@Nullable IndexedJsonValue jv, @NonNull String key) {
        List<Long> source = readLongListOrNull(jv, key);
        return source == null ? Collections.emptyList() : convertNanosToDuration(source);
    }

    @NonNull
    private static List<Duration> convertNanosToDuration(@NonNull List<Long> listOfNanos) {
        List<Duration> durations = new ArrayList<>();
        for (Long l : listOfNanos) {
            durations.add(Duration.ofNanos(l));
        }
        return durations;
    }

    // ---- Generic list conversion ----

    @Nullable
    public static <T> List<T> listOfOrNull(@Nullable IndexedJsonValue jv, @NonNull Function<IndexedJsonValue, T> converter) {
        if (jv == null || jv.getArray() == null) {
            return null;
        }
        List<T> list = convertToList(jv.getArray(), converter);
        return list.isEmpty() ? null : list;
    }

    @NonNull
    public static <T> List<T> listOfOrEmpty(@Nullable IndexedJsonValue jv, @NonNull Function<IndexedJsonValue, T> converter) {
        if (jv == null || jv.getArray() == null) {
            return Collections.emptyList();
        }
        return convertToList(jv.getArray(), converter);
    }

    @NonNull
    public static <T> List<T> convertToList(@NonNull List<IndexedJsonValue> list, @NonNull Function<IndexedJsonValue, T> converter) {
        List<T> result = new ArrayList<>();
        for (IndexedJsonValue jvv : list) {
            T t = converter.apply(jvv);
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }
}
