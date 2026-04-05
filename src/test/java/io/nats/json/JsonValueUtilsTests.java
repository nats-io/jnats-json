// Copyright 2020-2026 The NATS Authors
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

import io.ResourceUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nats.json.JsonValue.*;
import static io.nats.json.JsonValueUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public final class JsonValueUtilsTests {
    private static final String TEST_JSON = ResourceUtils.resourceAsString("test.json");
    private static final JsonValue TEST_JV = JsonParser.parseUnchecked(TEST_JSON, JsonParser.Option.DECIMALS);
    private static final String STRING_STRING = "Hello";
    private static final String DATE_STRING = "2021-01-25T20:09:10.6225191Z";
    private static final String BASE64_BASIC_STRING = "AGFiY2RlZgECBAg==";
    private static final String BASE64_URL_STRING = "AGFiY2RlZgECBAg";
    private static final byte[] BASE64_DECODED = new byte[] {0, 'a', 'b', 'c', 'd', 'e', 'f', 1, 2, 4, 8};
    private static final ZonedDateTime TEST_DATE = DateTimeUtils.parseDateTime(DATE_STRING);

    private static final String STRING = "string";
    private static final String INTEGER = "integer";
    private static final String LONG = "long";
    private static final String BIG_DECIMAL = "big_decimal";
    private static final String BOOL = "bool";
    private static final String DATE = "date";
    private static final String NANOS = "nanos";
    private static final String BASE_64_BASIC = "base_64_basic";
    private static final String BASE_64_URL = "base_64_url";
    private static final String MAP = "map";
    private static final String ARRAY = "array";
    private static final String MMAP = "mmap";
    private static final String SMAP = "smap";
    private static final String SLIST = "slist";
    private static final String MLIST = "mlist";
    private static final String ILIST = "ilist";
    private static final String LLIST = "llist";
    private static final String NLIST = "nlist";
    private static final String NOT_A_KEY = "not-a-key";

    @Test
    public void testReadValue() {
        assertNotNull(readValue(TEST_JV, STRING));
        assertNull(readValue(null, STRING));
        assertNull(readValue(NULL, STRING));
    }

    @Test
    public void testRead() {
        assertNotNull(read(TEST_JV, STRING, null, v -> v));
        // these JsonValues are not MAPS
        assertNull(read(null, NOT_A_KEY, null, v -> v));
        assertNull(read(EMPTY_ARRAY, NOT_A_KEY, null, v -> v));
        assertNull(read(TRUE, NOT_A_KEY, null, v -> v));
        assertNull(read(FALSE, NOT_A_KEY, null, v -> v));
        assertNull(read(NULL, NOT_A_KEY, null, v -> v));

        assertNotNull(read(TEST_JV, STRING, null, JsonValueType.STRING, v -> v));
        // these wrong requiredType
        assertNull(read(TEST_JV, STRING, null, JsonValueType.INTEGER, v -> v));
        // these JsonValues are not MAPS
        assertNull(read(null, NOT_A_KEY, null, JsonValueType.STRING, v -> v));
        assertNull(read(EMPTY_ARRAY, NOT_A_KEY, null, JsonValueType.STRING, v -> v));
        assertNull(read(TRUE, NOT_A_KEY, null, JsonValueType.STRING, v -> v));
        assertNull(read(FALSE, NOT_A_KEY, null, JsonValueType.STRING, v -> v));
        assertNull(read(NULL, NOT_A_KEY, null, JsonValueType.STRING, v -> v));
    }

    @Test
    public void testReadStrings() {
        String s = readString(TEST_JV, STRING);
        assertEquals(STRING_STRING, s);

        byte[] b = readBytes(TEST_JV, STRING);
        assertNotNull(b);
        assertEquals(s, new String(b));

        b = readBytes(TEST_JV, STRING, StandardCharsets.UTF_8);
        assertNotNull(b);
        assertEquals(s, new String(b));

        assertEquals(DATE_STRING, readString(TEST_JV, DATE));
        assertEquals(BASE64_BASIC_STRING, readString(TEST_JV, BASE_64_BASIC));
        assertEquals(BASE64_URL_STRING, readString(TEST_JV, BASE_64_URL));

        ZonedDateTime zdt = readDate(TEST_JV, DATE);
        assertEquals(TEST_DATE, zdt);

        b = readBase64Basic(TEST_JV, BASE_64_BASIC);
        assertArrayEquals(BASE64_DECODED, b);
        b = readBase64Url(TEST_JV, BASE_64_URL);
        assertArrayEquals(BASE64_DECODED, b);

        assertNull(readBase64Basic(TEST_JV, INTEGER));
        assertNull(readBase64Url(TEST_JV, INTEGER));

        assertNull(readString(TEST_JV, INTEGER));
        assertNull(readString(TEST_JV, LONG));
        assertNull(readString(TEST_JV, BOOL));
        assertNull(readString(TEST_JV, MAP));
        assertNull(readString(TEST_JV, ARRAY));
        assertNull(readString(TEST_JV, NOT_A_KEY));

        String dflt = "dflt";
        assertEquals(dflt, readString(TEST_JV, INTEGER, dflt));
        assertEquals(dflt, readString(TEST_JV, LONG, dflt));
        assertEquals(dflt, readString(TEST_JV, BOOL, dflt));
        assertEquals(dflt, readString(TEST_JV, MAP, dflt));
        assertEquals(dflt, readString(TEST_JV, ARRAY, dflt));
        assertEquals(dflt, readString(TEST_JV, NOT_A_KEY, dflt));

        assertNull(readString(TEST_JV, NOT_A_KEY));
        assertNull(readString(TEST_JV, INTEGER));

        assertEquals(STRING_STRING, readString(TEST_JV, STRING, dflt));
        assertEquals(dflt, readString(TEST_JV, NOT_A_KEY, dflt));
        assertEquals(dflt, readString(TEST_JV, INTEGER, dflt));
        assertEquals(dflt, readString(null, NOT_A_KEY, dflt));
    }

    @Test
    public void testReadInteger() {
        Integer i = readInteger(TEST_JV, INTEGER);
        assertNotNull(i);
        assertEquals(42, i);

        assertNull(readInteger(TEST_JV, STRING));
        assertNull(readInteger(TEST_JV, BOOL));
        assertNull(readInteger(TEST_JV, MAP));
        assertNull(readInteger(TEST_JV, ARRAY));
        assertNull(readInteger(TEST_JV, NOT_A_KEY));

        assertEquals(i, readInteger(TEST_JV, STRING, i));
        assertEquals(i, readInteger(TEST_JV, BOOL, i));
        assertEquals(i, readInteger(TEST_JV, MAP, i));
        assertEquals(i, readInteger(TEST_JV, ARRAY, i));
        assertEquals(i, readInteger(TEST_JV, NOT_A_KEY, i));

        int dflt = 99;
        assertEquals(42, readInteger(TEST_JV, INTEGER, dflt));
        assertEquals(dflt, readInteger(TEST_JV, STRING, dflt));
        assertEquals(dflt, readInteger(TEST_JV, BOOL, dflt));
        assertEquals(dflt, readInteger(TEST_JV, MAP, dflt));
        assertEquals(dflt, readInteger(TEST_JV, ARRAY, dflt));
        assertEquals(dflt, readInteger(TEST_JV, NOT_A_KEY, dflt));
    }

    @Test
    public void testReadLong() {
        assertEquals(42, readLong(TEST_JV, INTEGER));
        assertEquals(9223372036854775806L, readLong(TEST_JV, LONG));

        assertNull(readLong(TEST_JV, STRING));
        assertNull(readLong(TEST_JV, BOOL));
        assertNull(readLong(TEST_JV, MAP));
        assertNull(readLong(TEST_JV, ARRAY));
        assertNull(readLong(TEST_JV, NOT_A_KEY));

        long dflt = 99;
        assertEquals(dflt, readLong(TEST_JV, STRING, dflt));
        assertEquals(dflt, readLong(TEST_JV, BOOL, dflt));
        assertEquals(dflt, readLong(TEST_JV, MAP, dflt));
        assertEquals(dflt, readLong(TEST_JV, ARRAY, dflt));
        assertEquals(dflt, readLong(TEST_JV, NOT_A_KEY, dflt));

        assertEquals(42, readLong(TEST_JV, INTEGER, dflt));
        assertEquals(9223372036854775806L, readLong(TEST_JV, LONG, dflt));
        assertEquals(dflt, readLong(TEST_JV, STRING, dflt));
        assertEquals(dflt, readLong(TEST_JV, BOOL, dflt));
        assertEquals(dflt, readLong(TEST_JV, MAP, dflt));
        assertEquals(dflt, readLong(TEST_JV, ARRAY, dflt));
        assertEquals(dflt, readLong(TEST_JV, NOT_A_KEY, dflt));
    }

    @Test
    public void testReadBoolean() {
        Boolean b = readBoolean(TEST_JV, BOOL);
        assertNotNull(b);
        assertTrue(b);
        assertNull(readBoolean(TEST_JV, STRING));
        assertNull(readBoolean(TEST_JV, INTEGER));
        assertNull(readBoolean(TEST_JV, LONG));
        assertNull(readBoolean(TEST_JV, MAP));
        assertNull(readBoolean(TEST_JV, ARRAY));
        assertNull(readBoolean(TEST_JV, NOT_A_KEY));

        assertTrue(readBoolean(TEST_JV, BOOL, true));

        assertTrue(readBoolean(TEST_JV, STRING, true));
        assertTrue(readBoolean(TEST_JV, INTEGER, true));
        assertTrue(readBoolean(TEST_JV, LONG, true));
        assertTrue(readBoolean(TEST_JV, MAP, true));
        assertTrue(readBoolean(TEST_JV, ARRAY, true));
        assertTrue(readBoolean(TEST_JV, NOT_A_KEY, true));

        assertFalse(readBoolean(TEST_JV, STRING, false));
        assertFalse(readBoolean(TEST_JV, INTEGER, false));
        assertFalse(readBoolean(TEST_JV, LONG, false));
        assertFalse(readBoolean(TEST_JV, MAP, false));
        assertFalse(readBoolean(TEST_JV, ARRAY, false));
        assertFalse(readBoolean(TEST_JV, NOT_A_KEY, false));
    }

    @Test
    public void testReadDate() {
        ZonedDateTime t = readDate(TEST_JV, DATE);
        assertEquals(TEST_DATE, t);

        assertThrows(DateTimeParseException.class, () -> readDate(TEST_JV, STRING));
        assertThrows(DateTimeParseException.class, () -> readDate(TEST_JV, BASE_64_BASIC));

        assertNull(readDate(TEST_JV, BOOL));
        assertNull(readDate(TEST_JV, MAP));
        assertNull(readDate(TEST_JV, ARRAY));
        assertNull(readDate(TEST_JV, NOT_A_KEY));
    }

    @Test
    public void testReadNanosAsDuration() {
        assertEquals(Duration.ofSeconds(1), readNanosAsDuration(TEST_JV, NANOS));

        assertNull(readNanosAsDuration(TEST_JV, STRING));
        assertNull(readNanosAsDuration(TEST_JV, BOOL));
        assertNull(readNanosAsDuration(TEST_JV, MAP));
        assertNull(readNanosAsDuration(TEST_JV, ARRAY));
        assertNull(readNanosAsDuration(TEST_JV, NOT_A_KEY));

        Duration dflt = Duration.ofSeconds(99);
        assertEquals(Duration.ofSeconds(1), readNanosAsDuration(TEST_JV, NANOS, dflt));
        assertEquals(dflt, readNanosAsDuration(TEST_JV, STRING, dflt));
        assertEquals(dflt, readNanosAsDuration(TEST_JV, BOOL, dflt));
        assertEquals(dflt, readNanosAsDuration(TEST_JV, MAP, dflt));
        assertEquals(dflt, readNanosAsDuration(TEST_JV, ARRAY, dflt));
        assertEquals(dflt, readNanosAsDuration(TEST_JV, NOT_A_KEY, dflt));
    }

    @Test
    public void testObjectAndMaps() {
        // smap has all string values
        List<JsonValue> jvvs = new ArrayList<>();
        jvvs.add(readMapObjectOrNull(TEST_JV, SMAP));
        jvvs.add(readMapObjectOrEmpty(TEST_JV, SMAP));
        for (JsonValue jvv : jvvs) {
            assertEquals("A", readString(jvv, "a"));
            assertEquals("B", readString(jvv, "b"));
            assertEquals("C", readString(jvv, "c"));
        }

        List<Map<String, JsonValue>> maps = new ArrayList<>();
        maps.add(readMapMapOrNull(TEST_JV, SMAP));
        maps.add(readMapMapOrEmpty(TEST_JV, SMAP));
        for (Map<String, JsonValue> map : maps) {
            assertNotNull(map);
            assertEquals(3, map.size());
            assertEquals("A", map.get("a").string);
            assertEquals("B", map.get("b").string);
            assertEquals("C", map.get("c").string);
        }

        // mmap has different types of values
        jvvs.clear();
        jvvs.add(readMapObjectOrNull(TEST_JV, MMAP));
        jvvs.add(readMapObjectOrEmpty(TEST_JV, MMAP));
        for (JsonValue jvv : jvvs) {
            assertEquals("ss", readString(jvv, "s"));
            assertEquals(73, readInteger(jvv, "i"));
        }

        maps.clear();
        maps.add(readMapMapOrNull(TEST_JV, MMAP));
        maps.add(readMapMapOrEmpty(TEST_JV, MMAP));
        for (Map<String, JsonValue> map : maps) {
            assertNotNull(map);
            assertEquals(2, map.size());
            assertEquals("ss", map.get("s").string);
            assertEquals(73, map.get("i").i);
        }

        List<Map<String, String>> strmaps = new ArrayList<>();
        strmaps.add(readStringMapOrNull(TEST_JV, MMAP));
        strmaps.add(readStringMapOrEmpty(TEST_JV, MMAP));
        for (Map<String, String> strmap : strmaps) {
            assertNotNull(strmap);
            assertEquals(1, strmap.size());
            assertEquals("ss", strmap.get("s"));
            assertNull(strmap.get("i"));
        }

        assertNotNull(readMapMapOrNull(TEST_JV, SMAP));
        assertNotNull(readMapMapOrEmpty(TEST_JV, SMAP));
        assertNull(readMapMapOrNull(TEST_JV, NOT_A_KEY));
        assertSame(EMPTY_MAP_MAP, readMapMapOrEmpty(TEST_JV, NOT_A_KEY));
        assertNull(readMapMapOrNull(TEST_JV, STRING));
        assertSame(EMPTY_MAP_MAP, readMapMapOrEmpty(TEST_JV, STRING));
    }

    @Test
    public void testConvertToList() {
        List<JsonValue> list = new ArrayList<>();
        list.add(JsonValue.NULL);
        list.add(readValue(TEST_JV, STRING));

        List<String> strings = convertToList(list, jv -> {
            if (jv.type == JsonValueType.STRING) {
                return jv.string;
            }
            return null;
        });

        assertEquals(1, strings.size());
        assertEquals(STRING_STRING, strings.get(0));
    }

    @Test
    public void testArrays() {
        assertNull(listOfOrNull(null, jv -> jv));
        assertNull(listOfOrNull(readValue(TEST_JV, STRING), jv -> jv));
        assertNotNull(listOfOrNull(readValue(TEST_JV, SLIST), jv -> jv));
        assertTrue(listOfOrEmpty(null, jv -> jv).isEmpty());
        assertTrue(listOfOrEmpty(readValue(TEST_JV, STRING), jv -> jv).isEmpty());
        assertFalse(listOfOrEmpty(readValue(TEST_JV, SLIST), jv -> jv).isEmpty());

        assertNull(readArrayOrNull(TEST_JV, STRING));
        assertTrue(readArrayOrEmpty(TEST_JV, STRING).isEmpty());

        // slist has just strings
        List<List<JsonValue>> arrays = new ArrayList<>();
        arrays.add(readArrayOrNull(TEST_JV, SLIST));
        arrays.add(readArrayOrEmpty(TEST_JV, SLIST));
        for (List<JsonValue> array : arrays) {
            assertNotNull(array);
            assertEquals(3, array.size());
            assertTrue(array.contains(new JsonValue("X")));
            assertTrue(array.contains(new JsonValue("Y")));
            assertTrue(array.contains(new JsonValue("Z")));
        }

        // mlist has a mix of value types
        arrays.clear();
        arrays.add(readArrayOrNull(TEST_JV, MLIST));
        arrays.add(readArrayOrEmpty(TEST_JV, MLIST));
        for (List<JsonValue> array : arrays) {
            assertNotNull(array);
            assertEquals(4, array.size());
            assertTrue(array.contains(new JsonValue("Q")));
            assertTrue(array.contains(new JsonValue("R")));
            assertTrue(array.contains(new JsonValue(" ")));
            assertTrue(array.contains(new JsonValue(98)));
        }

        assertNull(readStringListOrNull(TEST_JV, STRING));
        assertTrue(readStringListOrEmpty(TEST_JV, STRING).isEmpty());

        List<List<String>> stringLists = new ArrayList<>();
        stringLists.add(readStringListOrNull(TEST_JV, SLIST));
        stringLists.add(readStringListOrEmpty(TEST_JV, SLIST));
        for (List<String> aos : stringLists) {
            assertNotNull(aos);
            assertEquals(3, aos.size());
            assertTrue(aos.contains("X"));
            assertTrue(aos.contains("Y"));
            assertTrue(aos.contains("Z"));
        }

        stringLists.clear();
        stringLists.add(readStringListOrNull(TEST_JV, MLIST));
        stringLists.add(readStringListOrEmpty(TEST_JV, MLIST));
        for (List<String> aos : stringLists) {
            assertNotNull(aos);
            assertEquals(3, aos.size());
            assertTrue(aos.contains("Q"));
            assertTrue(aos.contains("R"));
            assertTrue(aos.contains(" "));
        }

        assertNull(readIntegerListOrNull(TEST_JV, STRING));
        assertNull(readIntegerListOrNull(TEST_JV, SLIST));
        assertNull(readIntegerListOrNull(TEST_JV, LLIST));

        assertTrue(readIntegerListOrEmpty(TEST_JV, STRING).isEmpty());
        assertTrue(readIntegerListOrEmpty(TEST_JV, SLIST).isEmpty());
        assertTrue(readIntegerListOrEmpty(TEST_JV, LLIST).isEmpty());

        List<List<Integer>> intLists = new ArrayList<>();
        intLists.add(readIntegerListOrNull(TEST_JV, MLIST));
        intLists.add(readIntegerListOrEmpty(TEST_JV, MLIST));
        for (List<Integer> aoi : intLists) {
            assertNotNull(aoi);
            assertEquals(1, aoi.size());
            assertTrue(aoi.contains(98));
        }

        intLists.clear();
        intLists.add(readIntegerListOrNull(TEST_JV, ILIST));
        intLists.add(readIntegerListOrEmpty(TEST_JV, ILIST));
        for (List<Integer> aoi : intLists) {
            assertNotNull(aoi);
            assertEquals(3, aoi.size());
            assertTrue(aoi.contains(42));
            assertTrue(aoi.contains(73));
            assertTrue(aoi.contains(99));
        }

        assertNull(readLongListOrNull(TEST_JV, STRING));
        assertNull(readLongListOrNull(TEST_JV, SLIST));

        assertTrue(readLongListOrEmpty(TEST_JV, STRING).isEmpty());
        assertTrue(readLongListOrEmpty(TEST_JV, SLIST).isEmpty());

        List<List<Long>> longLists = new ArrayList<>();
        longLists.add(readLongListOrNull(TEST_JV, MLIST));
        longLists.add(readLongListOrEmpty(TEST_JV, MLIST));
        for (List<Long> aol : longLists) {
            assertNotNull(aol);
            assertEquals(1, aol.size());
            assertTrue(aol.contains(98L));
        }

        longLists.clear();
        longLists.add(readLongListOrNull(TEST_JV, ILIST));
        longLists.add(readLongListOrEmpty(TEST_JV, ILIST));
        for (List<Long> aol : longLists) {
            assertNotNull(aol);
            assertEquals(3, aol.size());
            assertTrue(aol.contains(42L));
            assertTrue(aol.contains(73L));
            assertTrue(aol.contains(99L));
        }

        longLists.clear();
        longLists.add(readLongListOrNull(TEST_JV, LLIST));
        longLists.add(readLongListOrEmpty(TEST_JV, LLIST));
        for (List<Long> aol : longLists) {
            assertNotNull(aol);
            assertEquals(3, aol.size());
            assertTrue(aol.contains(9223372036854775801L));
            assertTrue(aol.contains(9223372036854775802L));
            assertTrue(aol.contains(9223372036854775803L));
        }

        assertNull(readNanosAsDurationListOrNull(TEST_JV, STRING));
        assertTrue(readNanosAsDurationListOrEmpty(TEST_JV, STRING).isEmpty());

        List<List<Duration>> durLists = new ArrayList<>();
        durLists.add(readNanosAsDurationListOrNull(TEST_JV, NLIST));
        durLists.add(readNanosAsDurationListOrEmpty(TEST_JV, NLIST));
        for (List<Duration> aod : durLists) {
            assertNotNull(aod);
            assertEquals(3, aod.size());
            assertTrue(aod.contains(Duration.ofSeconds(2)));
            assertTrue(aod.contains(Duration.ofSeconds(3)));
            assertTrue(aod.contains(Duration.ofSeconds(4)));
        }
    }

    @Test
    public void testNotFoundOrWrongType() {
        validateNotFoundOrWrongType(STRING, true, false, true, true, true, true, true, true);
        validateNotFoundOrWrongType(INTEGER, true, true, true, false, false, true, true, true);
        validateNotFoundOrWrongType(LONG, true, true, true, true, false, true, true, true);
        validateNotFoundOrWrongType(BOOL, true, true, true, true, true, false, true, true);
        validateNotFoundOrWrongType(DATE, true, false, false, true, true, false, true, true);
        validateNotFoundOrWrongType(BASE_64_BASIC, true, false, true, true, true, true, true, true);
        validateNotFoundOrWrongType(BIG_DECIMAL, true, true, true, true, true, true, true, true);
        validateNotFoundOrWrongType(MAP, true, true, true, true, true, true, false, true);
        validateNotFoundOrWrongType(ARRAY, true, true, true, true, true, true, true, false);
        validateNotFoundOrWrongType(SMAP, true, true, true, true, true, true, false, true);
        validateNotFoundOrWrongType(SLIST, true, true, true, true, true, true, true, false);
        validateNotFoundOrWrongType(NOT_A_KEY, false, true, true, true, true, true, true, true);
    }

    private static void validateNotFoundOrWrongType(
        String key,
        boolean isKey,
        boolean notString,
        boolean notDate,
        boolean notInteger,
        boolean notLong,
        boolean notBoolean,
        boolean notMap,
        boolean notArray)
    {
        JsonValue jv = readValue(TEST_JV, key);
        if (isKey) {
            assertNotNull(jv);
        }
        else {
            assertNull(jv);
        }
        if (notString) {
            assertNull(readBytes(TEST_JV, key));
            assertNull(readBytes(TEST_JV, key, StandardCharsets.UTF_8));
            assertNull(readDate(TEST_JV, key));
            if (jv != null) {
                assertNotSame(JsonValueType.STRING, jv.type);
                assertNull(jv.string);
            }
        }
        else {
            assertNotNull(readBytes(TEST_JV, key));
            assertNotNull(readBytes(TEST_JV, key, StandardCharsets.UTF_8));
        }
        if (notDate) {
            if (jv == null || jv.string == null) {
                assertNull(readDate(TEST_JV, key));
            }
            else {
                assertThrows(DateTimeParseException.class, () -> readDate(TEST_JV, key));
            }
        }
        if (notInteger) {
            assertNull(readInteger(TEST_JV, key));
            assertEquals(-1, readInteger(TEST_JV, key, -1));
        }
        if (notLong) {
            assertNull(readLong(TEST_JV, key));
            assertEquals(-1, readLong(TEST_JV, key, -1));
        }
        if (notBoolean) {
            assertNull(readBoolean(TEST_JV, key));
            assertFalse(readBoolean(TEST_JV, key, false));
        }

        if (notMap) {
            assertNull(readMapObjectOrNull(TEST_JV, key));
            assertEquals(EMPTY_MAP, readMapObjectOrEmpty(TEST_JV, key));
            assertNull(readMapMapOrNull(TEST_JV, key));
            assertEquals(EMPTY_MAP.map, readMapMapOrEmpty(TEST_JV, key));
            assertNull(readStringMapOrNull(TEST_JV, key));
            assertEquals(new HashMap<>(), readStringMapOrEmpty(TEST_JV, key));
            if (jv != null) {
                assertNotSame(JsonValueType.MAP, jv.type);
                assertNull(jv.map);
            }
        }
        if (notArray) {
            assertNull(readArrayOrNull(TEST_JV, key));
            assertEquals(EMPTY_ARRAY.array, readArrayOrEmpty(TEST_JV, key));
            if (jv != null) {
                assertNotSame(JsonValueType.ARRAY, jv.type);
                assertNull(jv.array);
            }
        }
    }

    @Test
    public void testGetIntLong() {
        JsonValue x = readValue(TEST_JV, STRING);
        assertNotNull(x);
        JsonValue i = new JsonValue(Integer.MAX_VALUE);
        JsonValue li = new JsonValue((long)Integer.MAX_VALUE);
        JsonValue lmax = new JsonValue(Long.MAX_VALUE);
        JsonValue lmin = new JsonValue(Long.MIN_VALUE);

        assertEquals(Integer.MAX_VALUE, getInt(i, -1));
        assertEquals(Integer.MAX_VALUE, getInt(li, -1));
        assertEquals(-1, getInt(x, -1));
        assertEquals(-1, getInt(JsonValue.NULL, -1));
        assertEquals(-1, getInt(EMPTY_MAP, -1));
        assertEquals(-1, getInt(EMPTY_ARRAY, -1));

        assertEquals(Integer.MAX_VALUE, getInteger(i));
        assertEquals(Integer.MAX_VALUE, getInteger(li));
        assertNull(getInteger(x));
        assertNull(getInteger(lmax));
        assertNull(getInteger(lmin));
        assertNull(getInteger(JsonValue.NULL));
        assertNull(getInteger(EMPTY_MAP));
        assertNull(getInteger(EMPTY_ARRAY));

        assertEquals(Integer.MAX_VALUE, getLong(i));
        assertEquals(Integer.MAX_VALUE, getLong(li));
        assertEquals(Long.MAX_VALUE, getLong(lmax));
        assertEquals(Long.MIN_VALUE, getLong(lmin));
        assertNull(getLong(x));
        assertNull(getLong(JsonValue.NULL));
        assertNull(getLong(EMPTY_MAP));
        assertNull(getLong(EMPTY_ARRAY));

        assertEquals(Integer.MAX_VALUE, getLong(i, -1));
        assertEquals(Integer.MAX_VALUE, getLong(li, -1));
        assertEquals(Long.MAX_VALUE, getLong(lmax, -1));
        assertEquals(Long.MIN_VALUE, getLong(lmin, -1));
        assertEquals(-1, getLong(x, -1));
        assertEquals(-1, getLong(JsonValue.NULL, -1));
        assertEquals(-1, getLong(EMPTY_MAP, -1));
        assertEquals(-1, getLong(EMPTY_ARRAY, -1));
    }
}
