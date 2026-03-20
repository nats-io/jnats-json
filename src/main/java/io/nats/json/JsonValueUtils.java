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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

import static io.nats.json.JsonValue.EMPTY_MAP;
import static io.nats.json.JsonValue.EMPTY_MAP_MAP;

/**
 * Utilities around JsonValue finding values in a {@link JsonValueType#MAP}.
 */
public abstract class JsonValueUtils {

    private JsonValueUtils() {} /* ensures cannot be constructed */

    /**
     * Read a key's value, without assuming it's type
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The JsonValue or null if the key is not found
     */
    @Nullable
    public static JsonValue readValue(@Nullable JsonValue jv, @NonNull String key) {
        return jv == null || jv.map == null ? null : jv.map.get(key);
    }

    /**
     * Read a value generically
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param dflt the default value to use if the input is null, is not a map, or the key is not found
     * @param valueSupplier the generic supplier that converts the object to the output type
     * @return the value represented by the key
     * @param <T> the output type
     */
    @Nullable
    public static <T> T read(@Nullable JsonValue jv, @NonNull String key, @Nullable T dflt, @NonNull JsonValueSupplier<T> valueSupplier) {
        if (jv == null || jv.map == null) {
            return dflt;
        }
        JsonValue v = jv.map.get(key);
        return v == null ? dflt : valueSupplier.get(v);
    }

    /**
     * Read a key's value and extract the value using the value supplied
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param dflt the default value for error conditions, like wrong type or not found
     * @param requiredType the required type of the value found with the key
     * @param valueSupplier the value supplier
     * @return the value
     * @param <T> the supplied type
     */
    @Nullable
    public static <T> T read(@Nullable JsonValue jv, @NonNull String key, @Nullable T dflt, @NonNull JsonValueType requiredType, @NonNull JsonValueSupplier<T> valueSupplier) {
        if (jv == null || jv.map == null) {
            return dflt;
        }
        JsonValue v = jv.map.get(key);
        return v == null || v.type != requiredType ? dflt : valueSupplier.get(v);
    }

    /**
     * Read a key's string value expecting the value to be of type {@link JsonValueType#STRING}.
     * If the key is not found or the type is not STRING, null is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the string or null
     */
    @Nullable
    public static String readString(@Nullable JsonValue jv, @NonNull String key) {
        return read(jv, key, null, v -> v.string);
    }

    /**
     * Read a key's string value expecting the value to be of type {@link JsonValueType#STRING}.
     * If the key is not found or the type is not STRING, the supplied default is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param dflt the default value. Null is allowed
     * @return the string or the default
     */
    @Nullable
    public static String readString(@Nullable JsonValue jv, @NonNull String key, @Nullable String dflt) {
        return read(jv, key, dflt, JsonValueType.STRING, v -> v.string);
    }

    /**
     * Read a key's int value expecting the value to be of type {@link JsonValueType#INTEGER}.
     * or of type {@link JsonValueType#LONG} with a value in the range of integer.
     * If the key is not found or the value is not an integer, null is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the integer or null
     */
    @Nullable
    public static Integer readInteger(@Nullable JsonValue jv, @NonNull String key) {
        return read(jv, key, null, JsonValueUtils::getInteger);
    }

    /**
     * Read a key's int value expecting the value to be of type {@link JsonValueType#INTEGER}.
     * or of type {@link JsonValueType#LONG} with a value in the range of integer.
     * If the key is not found or the value is not an integer, the supplied default is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param dflt the default value
     * @return the integer or the default
     */
    public static int readInteger(@Nullable JsonValue jv, @NonNull String key, int dflt) {
        Integer i = readInteger(jv, key);
        return i == null ? dflt : i;
    }

    /**
     * Read a key's long value expecting the value to be of type {@link JsonValueType#INTEGER} or {@link JsonValueType#LONG}.
     * If the key is not found or the value is not an integer or long, null is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the long or null
     */
    @Nullable
    public static Long readLong(@Nullable JsonValue jv, @NonNull String key) {
        return read(jv, key, null, JsonValueUtils::getLong);
    }

    /**
     * Read a key's long value expecting the value to be of type {@link JsonValueType#INTEGER} or {@link JsonValueType#LONG}.
     * If the key is not found or the value is not an integer or long, the default is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param dflt the default value
     * @return the long or the default
     */
    public static long readLong(@Nullable JsonValue jv, @NonNull String key, long dflt) {
        Long l = readLong(jv, key);
        return l == null ? dflt : l;
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#BOOL}.
     * <p>If the key is not found or the type is not BOOL, null is returned.</p>
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the value or false
     */
    @Nullable
    public static Boolean readBoolean(@Nullable JsonValue jv, @NonNull String key) {
        return read(jv, key, null, v -> v.bool);
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#BOOL}.
     * <p>If the key is not found or the type is not BOOL, the default returned.</p>
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param dflt the default value
     * @return the value or the default
     */
    public static boolean readBoolean(@Nullable JsonValue jv, @NonNull String key, boolean dflt) {
        Boolean b = readBoolean(jv, key);
        return b == null ? dflt : b;
    }

    /**
     * Read a key's string value expecting the value to be of type {@link JsonValueType#STRING}.
     * and then parses that string to a ZonedDateTime.
     * <p>If the key is not found or the type is not STRING, null is returned.</p>
     * <p>If the string is found but is not parseable by ZonedDateTime, a DateTimeParseException is thrown</p>
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the ZonedDateTime or null
     */
    @Nullable
    public static ZonedDateTime readDate(@Nullable JsonValue jv, @NonNull String key) {
        String s = readString(jv, key);
        return s == null ? null : DateTimeUtils.parseDateTimeThrowParseError(s);
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#INTEGER}
     * or {@link JsonValueType#LONG}, and then converts that to a Duration assuming the number represents nanoseconds
     * <p>If the key is not found or the type is not INTEGER or LONG, null is returned.</p>
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the Duration or null
     */
    @Nullable
    public static Duration readNanosAsDuration(@Nullable JsonValue jv, @NonNull String key) {
        Long l = readLong(jv, key);
        return l == null ? null : Duration.ofNanos(l);
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#INTEGER}
     * or {@link JsonValueType#LONG}, and then converts that to a Duration assuming the number
     * represents nanoseconds
     * <p>If the key is not found or the type is not INTEGER or LONG, null is returned.</p>
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param dflt the default value
     * @return the Duration or the default
     */
    @Nullable
    public static Duration readNanosAsDuration(@Nullable JsonValue jv, @NonNull String key, Duration dflt) {
        Long l = readLong(jv, key);
        return l == null ? dflt : Duration.ofNanos(l);
    }

    /**
     * Read a key's string value expecting the value to be of type {@link JsonValueType#STRING}.
     * If the key is not found or the type is not STRING, null is returned,
     * otherwise the string is converted to bytes using UTF8
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the byte[] or null
     */
    public static byte @Nullable [] readBytes(@Nullable JsonValue jv, @NonNull String key) {
        String s = readString(jv, key);
        return s == null ? null : s.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Read a key's string value expecting the value to be of type {@link JsonValueType#STRING}.
     * If the key is not found or the type is not STRING, null is returned,
     * otherwise the string is converted to bytes using the charset provided
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param charset the charset of the bytes in the read string
     * @return the string or default
     */
    public static byte @Nullable [] readBytes(@Nullable JsonValue jv, @NonNull String key, Charset charset) {
        String s = readString(jv, key);
        return s == null ? null : s.getBytes(charset);
    }

    /**
     * Read a key's string value expecting the value to be of type {@link JsonValueType#STRING}.
     * If the key is not found or the type is not STRING, null is returned,
     * otherwise the string is converted to bytes using Encoding.base64BasicDecode
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the byte[] or null
     */
    public static byte @Nullable [] readBase64Basic(@Nullable JsonValue jv, @NonNull String key) {
        String b64 = readString(jv, key);
        return b64 == null ? null : Encoding.base64BasicDecode(b64);
    }

    /**
     * Read a key's string value expecting the value to be of type {@link JsonValueType#STRING}.
     * If the key is not found or the type is not STRING, null is returned,
     * otherwise the string is converted to bytes using Encoding.base64UrlDecode
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return the byte[] or null
     */
    public static byte @Nullable [] readBase64Url(@Nullable JsonValue jv, @NonNull String key) {
        String b64 = readString(jv, key);
        return b64 == null ? null : Encoding.base64UrlDecode(b64);
    }

    /**
     * Read a key's map value as a generic JsonValue assuming the value is a JSON object (a map)
     * If the key is not found or the value type is not MAP, null is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The JsonValue for the MAP object or null
     */
    @Nullable
    public static JsonValue readMapObjectOrNull(@Nullable JsonValue jv, @NonNull String key) {
        return read(jv, key, null, JsonValueType.MAP, v -> v);
    }

    /**
     * Read a key's map value as a generic JsonValue assuming the value is a JSON object (a map)
     * If the key is not found or the value type is not MAP, EMPTY_MAP is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The JsonValue for the MAP object or EMPTY_MAP
     */
    @NonNull
    public static JsonValue readMapObjectOrEmpty(@Nullable JsonValue jv, @NonNull String key) {
        JsonValue jvv = readMapObjectOrNull(jv, key);
        return jvv == null ? EMPTY_MAP : jvv;
    }

    /**
     * Read a key's map value as a Map of String to JsonValue
     * If the key is not found or the value type is not MAP, null is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The JsonValue for the MAP object or null
     */
    @Nullable
    public static Map<String, JsonValue> readMapMapOrNull(@Nullable JsonValue jv, @NonNull String key) {
        JsonValue jvv = readMapObjectOrNull(jv, key);
        return jvv == null ? null : jvv.map;
    }

    /**
     * Read a key's map value as a Map of String to JsonValue
     * If the key is not found or the value type is not MAP, EMPTY_MAP is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The JsonValue for the MAP object or EMPTY_MAP
     */
    @NonNull
    public static Map<String, JsonValue> readMapMapOrEmpty(@Nullable JsonValue jv, @NonNull String key) {
        Map<String, JsonValue> map = readMapMapOrNull(jv, key);
        return map == null ? EMPTY_MAP_MAP : map;
    }

    /**
     * Read a key's map value as a Map of String to String. This will
     * assume that every value in the looked-up map is a string,
     * otherwise the key will not be included in the returned map.
     * If there is no map for the key, or the map is found but empty,
     * the function returns null.
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The resolved map or null
     */
    @Nullable
    public static Map<String, String> readStringMapOrNull(@Nullable JsonValue jv, @NonNull String key) {
        Map<String, JsonValue> map = readMapMapOrNull(jv, key);
        return map == null ? null : convertToStringMap(map);
    }

    /**
     * Read a key's map value as a Map of String to String. This will
     * assume that every value in the looked-up map is a string,
     * otherwise the key will not be included in the returned map.
     * If there is no map for the key, or the map is found but empty,
     * the function returns null.
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The resolved map or empty map
     */
    @NonNull
    public static Map<String, String> readStringMapOrEmpty(@Nullable JsonValue jv, @NonNull String key) {
        return convertToStringMap(readMapMapOrEmpty(jv, key));
    }

    /**
     * Converts a JsonValue's Map of String to JsonValue to a Map of String to String,
     * filtering out any key whose value is not a string.
     * @param map the input map
     * @return the converted map. May be empty but not null.
     */
    @NonNull
    public static Map<String, String> convertToStringMap(@NonNull Map<String, JsonValue> map) {
        Map<String, String> temp = new HashMap<>();
        for (String k : map.keySet()) {
            JsonValue value = map.get(k);
            if (value.type == JsonValueType.STRING) {
                temp.put(k, value.string);
            }
        }
        return temp;
    }

    /**
     * Read a value expecting it to be of type {@link JsonValueType#ARRAY}.
     * If the value is not found or the type is not ARRAY, null is returned
     * Returns null if the array is empty
     * @param jv the jsonValue that is an array (type is {@link JsonValueType#ARRAY})
     * @param converter a function that converts each value in the array to the desired type
     * @return The List of JsonValues in the array or null if the value was null or not an ARRAY
     * @param <T> the list type
     */
    @Nullable
    public static <T> List<T> listOfOrNull(@Nullable JsonValue jv, @NonNull Function<JsonValue, T> converter) {
        if (jv == null || jv.array == null) {
            return null;
        }
        List<T> list = convertToList(jv.array, converter);
        return list.size() == 0 ? null : list;
    }

    /**
     * Read a value expecting it to be of type {@link JsonValueType#ARRAY}.
     * If the value is not found or the type is not ARRAY, an empty list is returned
     * @param jv the jsonValue that is an array (type is {@link JsonValueType#ARRAY})
     * @param converter a function that converts each value in the array to the desired type
     * @return The List of JsonValues in the array or Collections.emptyList() if the value was null or not an ARRAY
     * @param <T> the list type
     */
    @NonNull
    public static <T> List<T> listOfOrEmpty(@Nullable JsonValue jv, @NonNull Function<JsonValue, T> converter) {
        if (jv == null || jv.array == null) {
            return Collections.emptyList();
        }
        return convertToList(jv.array, converter);
    }

    /**
     * Convert a list of JsonValue to another type of list
     * @param list the source list
     * @param converter the converter function
     * @return the resulting list
     * @param <T> the output type of the conversion list
     */
    @NonNull
    public static <T> List<T> convertToList(@NonNull List<JsonValue> list, @NonNull Function<JsonValue, T> converter) {
        List<T> result = new ArrayList<>();
        for (JsonValue jvv : list) {
            T t = converter.apply(jvv);
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, null is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of JsonValues or null
     */
    @Nullable
    public static List<JsonValue> readArrayOrNull(@Nullable JsonValue jv, @NonNull String key) {
        return read(jv, key, null, JsonValueType.ARRAY, v -> v.array);
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, an empty list is returned
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of JsonValues in the array or Collections.emptyList()
     */
    @NonNull
    public static List<JsonValue> readArrayOrEmpty(@Nullable JsonValue jv, @NonNull String key) {
        List<JsonValue> list = readArrayOrNull(jv, key);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, null is returned
     * If the value is not a string it is not included in the returned list.
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of strings in the array or null
     */
    @Nullable
    public static List<String> readStringListOrNull(@Nullable JsonValue jv, @NonNull String key) {
        return readStringListOrNull(jv, key, false);
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, null is returned
     * If the value is not a string it is not included in the returned list.
     * Optionally ignores blank strings.
     * Returns null if the array is empty
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param ignoreBlank ignore blanks
     * @return The list of strings in the array or null
     */
    @Nullable
    public static List<String> readStringListOrNull(@Nullable JsonValue jv, @NonNull String key, boolean ignoreBlank) {
        List<JsonValue> list = read(jv, key, null, JsonValueType.ARRAY, v -> v.array);
        if (list == null) {
            return null;
        }
        List<String> strings = convertToStringList(list, ignoreBlank);
        return strings.size() == 0 ? null : strings;
    }

    /**
     * Read a key's value expecting the value to be of type {@link }JsonValueType#ARRAY}.
     * If the key is not found or the type is not ARRAY, an empty list is returned
     * If the value is not a string it is not included in the returned list.
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of string in the array or Collections.emptyList()
     */
    @NonNull
    public static List<String> readStringListOrEmpty(@Nullable JsonValue jv, @NonNull String key) {
        List<JsonValue> source = read(jv, key, null, JsonValueType.ARRAY, v -> v.array);
        return source == null ? Collections.emptyList() : convertToStringList(source, false);
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, an empty list is returned
     * If the value is not a string it is not included in the returned list.
     * Optionally ignores blank strings.
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @param ignoreBlank ignore blanks
     * @return The list of string in the array or Collections.emptyList()
     */
    @NonNull
    public static List<String> readStringListOrEmpty(@Nullable JsonValue jv, @NonNull String key, boolean ignoreBlank) {
        List<JsonValue> source = read(jv, key, null, JsonValueType.ARRAY, v -> v.array);
        return source == null ? Collections.emptyList() : convertToStringList(source, ignoreBlank);
    }

    /**
     * Convert a list of JsonValue to a list of Strings.
     * Ignores any JsonValue that is not a {@link JsonValueType#STRING}
     * Optionally ignores blank strings.
     * It does not stringify numbers!
     * @param source the source list
     * @param ignoreBlank ignore blanks
     * @return the list of String
     */
    @NonNull
    public static List<String> convertToStringList(@NonNull List<JsonValue> source, boolean ignoreBlank) {
        List<String> result = new ArrayList<>();
        for (JsonValue v : source) {
            if (v.string != null) {
                if (ignoreBlank) {
                    if (!v.string.trim().isEmpty()) {
                        result.add(v.string);
                    }
                }
                else {
                    result.add(v.string);
                }
            }
        }
        return result;
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, null is returned
     * If the value is not an integer it is not included in the returned list.
     * Returns null if the array is empty
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of integers in the array or null
     */
    @Nullable
    public static List<Integer> readIntegerListOrNull(@Nullable JsonValue jv, @NonNull String key) {
        List<JsonValue> source = read(jv, key, null, JsonValueType.ARRAY, v -> v.array);
        if (source == null) {
            return null;
        }
        List<Integer> list = convertToIntegerList(source);
        return list.size() == 0 ? null : list;
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, an empty list is returned
     * If the value is not an integer it is not included in the returned list.
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of integers in the array or Collections.emptyList()
     */
    @NonNull
    public static List<Integer> readIntegerListOrEmpty(@Nullable JsonValue jv, @NonNull String key) {
        List<JsonValue> source = read(jv, key, null, JsonValueType.ARRAY, v -> v.array);
        return source == null ? Collections.emptyList() : convertToIntegerList(source);
    }

    /**
     * Convert a list of JsonValue to a list of Integer. Ignores any JsonValue that is not an int or long
     * @param source the source list
     * @return the list of Integer
     */
    @NonNull
    public static List<Integer> convertToIntegerList(@NonNull List<JsonValue> source) {
        List<Integer> result = new ArrayList<>();
        for (JsonValue v : source) {
            Integer i = getInteger(v);
            if (i != null) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, null is returned
     * If the value is not an integer or long it is not included in the returned list.
     * Returns null if the array is empty
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of longs in the array or null
     */
    @Nullable
    public static List<Long> readLongListOrNull(@Nullable JsonValue jv, @NonNull String key) {
        List<JsonValue> source = read(jv, key, null, JsonValueType.ARRAY, v -> v.array);
        if (source == null) {
            return null;
        }
        List<Long> list = convertToLongList(source);
        return list.size() == 0 ? null : list;
    }

    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#ARRAY},
     * If the key is not found or the type is not ARRAY, an empty list is returned
     * If the value is not an integer or long it is not included in the returned list.
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of longs in the array or Collections.emptyList()
     */
    @NonNull
    public static List<Long> readLongListOrEmpty(@Nullable JsonValue jv, @NonNull String key) {
        List<JsonValue> source = read(jv, key, null, JsonValueType.ARRAY, v -> v.array);
        return source == null ? Collections.emptyList() : convertToLongList(source);
    }

    /**
     * Convert a list of JsonValue to a list of Long. Ignores any JsonValue that is not a long
     * @param source the source list
     * @return the list of Long
     */
    @NonNull
    public static List<Long> convertToLongList(@NonNull List<JsonValue> source) {
        List<Long> result = new ArrayList<>();
        for (JsonValue v : source) {
            Long l = getLong(v);
            if (l != null) {
                result.add(l);
            }
        }
        return result;
    }

    /**
     * Read a key's value expecting the val!ue to be of type {@link JsonValueType#INTEGER} or {@link JsonValueType#LONG}.
     * If the key is not found or readable as a long, null is returned
     * If the value is not an integer or long it is not considered in the returned list.
     * The values are converted to a Duration assuming the value represents nanoseconds
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of Duration or null
     */
    @Nullable
    public static List<Duration> readNanosAsDurationListOrNull(@Nullable JsonValue jv, @NonNull String key) {
        List<Long> source = readLongListOrNull(jv, key);
        return source == null ? null : convertNanosToDuration(source);
    }


    /**
     * Read a key's value expecting the value to be of type {@link JsonValueType#INTEGER} or {@link JsonValueType#LONG}.
     * If the key is not found or readable as a long, an empty list is returned
     * If the value is not an integer or long it is not considered in the returned list.
     * The values are converted to a Duration assuming the value represents nanoseconds
     * @param jv the jsonValue that is an object (type is {@link JsonValueType#MAP})
     * @param key the key to look up
     * @return The list of Duration or Collections.emptyList()
     */
    @NonNull
    public static List<Duration> readNanosAsDurationListOrEmpty(@Nullable JsonValue jv, @NonNull String key) {
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

    /**
     * Get the int value of the JsonValue
     * @param jv the jsonValue that represents an Integer or a Long with a value in the range of valid Integer
     * @return the Integer value, which may be null
     */
    @Nullable
    public static Integer getInteger(@NonNull JsonValue jv) {
        if (jv.i != null) {
            return jv.i;
        }
        // just in case the number was stored as long, which is unlikely, but I want to handle it
        if (jv.l != null && jv.l <= Integer.MAX_VALUE && jv.l >= Integer.MIN_VALUE) {
            return jv.l.intValue();
        }
        return null;
    }

    /**
     * Get the int value of the JsonValue
     * @param jv the jsonValue that represents an Integer
     * @param dflt the default value to use if the object does not represent an Integer
     * @return the int value or default
     */
    public static int getInt(@NonNull JsonValue jv, int dflt) {
        Integer i = getInteger(jv);
        return  i == null ? dflt : i;
    }

    /**
     * Get the Long value of the JsonValue
     * @param jv the jsonValue that represents a Long or an Integer
     * @return the Long value, which may be null
     */
    @Nullable
    public static Long getLong(@NonNull JsonValue jv) {
        return jv.l != null ? jv.l : (jv.i != null ? (long)jv.i : null);
    }

    /**
     * Get the long value of the JsonValue
     * @param jv the jsonValue that represents a Long or an Integer
     * @param dflt the default value to use if the object does not represent a Long or Integer
     * @return the long value or default
     */
    public static long getLong(@NonNull JsonValue jv, long dflt) {
        return jv.l != null ? jv.l : (jv.i != null ? (long)jv.i : dflt);
    }
}
