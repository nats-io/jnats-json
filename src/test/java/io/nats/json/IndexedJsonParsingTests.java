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

import io.ResourceUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IndexedJsonParser / IndexedJsonValue / IndexedJsonValueUtils.
 * Validates that the indexed (lazy) parser produces the same results as
 * the eager {@link JsonParser} for a wide range of inputs.
 */
public final class IndexedJsonParsingTests {

    // ---- basic value types ----

    @Test
    public void testStringValues() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("\"hello world\"");
        assertEquals(JsonValueType.STRING, v.type);
        assertEquals("hello world", v.getString());
    }

    @Test
    public void testStringEscapes() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("\"line1\\nline2\\ttab\\\\back\\\"quote\"");
        assertEquals("line1\nline2\ttab\\back\"quote", v.getString());
    }

    @Test
    public void testUnicodeEscape() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("\"\\u0041\\u0042\\u0043\"");
        assertEquals("ABC", v.getString());
    }

    @Test
    public void testTrueFalseNull() throws Exception {
        assertSame(IndexedJsonValue.TRUE, IndexedJsonParser.parse("true"));
        assertSame(IndexedJsonValue.FALSE, IndexedJsonParser.parse("false"));
        assertSame(IndexedJsonValue.NULL, IndexedJsonParser.parse("null"));
    }

    @Test
    public void testIntegerValue() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertEquals(Integer.valueOf(42), v.getInteger());
        assertEquals(Long.valueOf(42), v.getLong());
    }

    @Test
    public void testNegativeInteger() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("-7");
        assertEquals(Integer.valueOf(-7), v.getInteger());
    }

    @Test
    public void testLongValue() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("3000000000");
        assertNull(v.getInteger()); // too large for int
        assertEquals(Long.valueOf(3000000000L), v.getLong());
    }

    @Test
    public void testBigDecimalValue() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("3.14159", JsonParser.Option.DECIMALS);
        assertNotNull(v.getBigDecimal());
        assertEquals(new BigDecimal("3.14159"), v.getBigDecimal());
    }

    @Test
    public void testBigIntegerValue() throws Exception {
        String huge = "99999999999999999999999999999";
        IndexedJsonValue v = IndexedJsonParser.parse(huge);
        assertNotNull(v.getBigInteger());
        assertEquals(new BigInteger(huge), v.getBigInteger());
    }

    @Test
    public void testNegativeZero() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("-0", JsonParser.Option.DECIMALS);
        Double d = v.getDouble();
        assertNotNull(d);
        assertEquals(-0.0, d, 0.0);
    }

    // ---- arrays ----

    @Test
    public void testEmptyArray() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[]");
        assertEquals(JsonValueType.ARRAY, v.type);
        assertNotNull(v.getArray());
        assertTrue(v.getArray().isEmpty());
    }

    @Test
    public void testArrayOfInts() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[1, 2, 3]");
        assertEquals(JsonValueType.ARRAY, v.type);
        List<IndexedJsonValue> arr = v.getArray();
        assertNotNull(arr);
        assertEquals(3, arr.size());
        assertEquals(Integer.valueOf(1), arr.get(0).getInteger());
        assertEquals(Integer.valueOf(2), arr.get(1).getInteger());
        assertEquals(Integer.valueOf(3), arr.get(2).getInteger());
    }

    @Test
    public void testMixedArray() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[\"hello\", 42, true, null, 3.14]",
            JsonParser.Option.DECIMALS);
        List<IndexedJsonValue> arr = v.getArray();
        assertNotNull(arr);
        assertEquals(5, arr.size()); // null is NOT dropped in arrays (same as JsonParser)
        assertEquals("hello", arr.get(0).getString());
        assertEquals(Integer.valueOf(42), arr.get(1).getInteger());
        assertSame(IndexedJsonValue.TRUE, arr.get(2));
        assertSame(IndexedJsonValue.NULL, arr.get(3));
        assertEquals(new BigDecimal("3.14"), arr.get(4).getBigDecimal());
    }

    // ---- objects ----

    @Test
    public void testEmptyObject() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{}");
        assertEquals(JsonValueType.MAP, v.type);
        assertNotNull(v.getMap());
        assertTrue(v.getMap().isEmpty());
    }

    @Test
    public void testSimpleObject() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"name\":\"Alice\",\"age\":30}");
        Map<String, IndexedJsonValue> map = v.getMap();
        assertNotNull(map);
        assertEquals("Alice", map.get("name").getString());
        assertEquals(Integer.valueOf(30), map.get("age").getInteger());
    }

    @Test
    public void testNestedObject() throws Exception {
        String json = "{\"outer\":{\"inner\":\"value\"},\"list\":[1,2]}";
        IndexedJsonValue v = IndexedJsonParser.parse(json);
        Map<String, IndexedJsonValue> map = v.getMap();
        assertNotNull(map);

        IndexedJsonValue outer = map.get("outer");
        assertEquals(JsonValueType.MAP, outer.type);
        assertEquals("value", outer.getMap().get("inner").getString());

        IndexedJsonValue list = map.get("list");
        assertEquals(JsonValueType.ARRAY, list.type);
        assertEquals(2, list.getArray().size());
    }

    @Test
    public void testNullsDroppedByDefault() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":1,\"b\":null,\"c\":\"x\"}");
        Map<String, IndexedJsonValue> map = v.getMap();
        assertNotNull(map);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testKeepNulls() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":1,\"b\":null}".toCharArray(), JsonParser.Option.KEEP_NULLS);
        Map<String, IndexedJsonValue> map = v.getMap();
        assertNotNull(map);
        assertTrue(map.containsKey("b"));
        assertSame(IndexedJsonValue.NULL, map.get("b"));
    }

    @Test
    public void testDanglingComma() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":1,}");
        assertNotNull(v.getMap());
        assertEquals(Integer.valueOf(1), v.getMap().get("a").getInteger());
    }

    // ---- IndexedJsonValueUtils ----

    @Test
    public void testUtilsReadString() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"name\":\"Bob\",\"age\":25}");
        assertEquals("Bob", IndexedJsonValueUtils.readString(v, "name"));
        assertNull(IndexedJsonValueUtils.readString(v, "missing"));
        assertEquals("default", IndexedJsonValueUtils.readString(v, "missing", "default"));
    }

    @Test
    public void testUtilsReadInteger() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"count\":42}");
        assertEquals(Integer.valueOf(42), IndexedJsonValueUtils.readInteger(v, "count"));
        assertEquals(99, IndexedJsonValueUtils.readInteger(v, "missing", 99));
    }

    @Test
    public void testUtilsReadLong() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"big\":3000000000}");
        assertEquals(Long.valueOf(3000000000L), IndexedJsonValueUtils.readLong(v, "big"));
        assertEquals(0L, IndexedJsonValueUtils.readLong(v, "missing", 0L));
    }

    @Test
    public void testUtilsReadBoolean() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"flag\":true,\"off\":false}");
        assertEquals(Boolean.TRUE, IndexedJsonValueUtils.readBoolean(v, "flag"));
        assertEquals(Boolean.FALSE, IndexedJsonValueUtils.readBoolean(v, "off"));
        assertNull(IndexedJsonValueUtils.readBoolean(v, "missing"));
        assertTrue(IndexedJsonValueUtils.readBoolean(v, "missing", true));
    }

    @Test
    public void testUtilsReadNanosAsDuration() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"dur\":1000000000}");
        Duration d = IndexedJsonValueUtils.readNanosAsDuration(v, "dur");
        assertNotNull(d);
        assertEquals(Duration.ofSeconds(1), d);
    }

    @Test
    public void testUtilsReadStringList() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"tags\":[\"a\",\"b\",\"c\"]}");
        List<String> tags = IndexedJsonValueUtils.readStringListOrNull(v, "tags");
        assertNotNull(tags);
        assertEquals(Arrays.asList("a", "b", "c"), tags);

        assertNull(IndexedJsonValueUtils.readStringListOrNull(v, "missing"));
        assertEquals(Collections.emptyList(), IndexedJsonValueUtils.readStringListOrEmpty(v, "missing"));
    }

    @Test
    public void testUtilsReadIntegerList() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"nums\":[1,2,3]}");
        List<Integer> nums = IndexedJsonValueUtils.readIntegerListOrNull(v, "nums");
        assertNotNull(nums);
        assertEquals(Arrays.asList(1, 2, 3), nums);
    }

    @Test
    public void testUtilsReadLongList() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"nums\":[3000000000,1]}");
        List<Long> nums = IndexedJsonValueUtils.readLongListOrNull(v, "nums");
        assertNotNull(nums);
        assertEquals(Arrays.asList(3000000000L, 1L), nums);
    }

    @Test
    public void testUtilsReadNestedMap() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"config\":{\"key\":\"val\"}}");
        IndexedJsonValue config = IndexedJsonValueUtils.readMapObjectOrNull(v, "config");
        assertNotNull(config);
        assertEquals("val", IndexedJsonValueUtils.readString(config, "key"));

        assertNull(IndexedJsonValueUtils.readMapObjectOrNull(v, "missing"));
        assertSame(IndexedJsonValue.EMPTY_MAP, IndexedJsonValueUtils.readMapObjectOrEmpty(v, "missing"));
    }

    @Test
    public void testUtilsReadStringMap() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"headers\":{\"Content-Type\":\"application/json\",\"Accept\":\"*/*\"}}");
        Map<String, String> headers = IndexedJsonValueUtils.readStringMapOrNull(v, "headers");
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("*/*", headers.get("Accept"));
    }

    @Test
    public void testUtilsReadBytes() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"data\":\"hello\"}");
        byte[] bytes = IndexedJsonValueUtils.readBytes(v, "data");
        assertNotNull(bytes);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
    }

    // ---- toJsonValue conversion ----

    @Test
    public void testToJsonValueRoundTrip() throws Exception {
        String json = "{\"name\":\"Alice\",\"age\":30,\"scores\":[95,87,100],\"active\":true,\"meta\":{\"role\":\"admin\"}}";

        // Parse with both parsers
        JsonValue eager = JsonParser.parse(json);
        IndexedJsonValue indexed = IndexedJsonParser.parse(json);

        // Convert indexed to JsonValue and compare
        JsonValue fromIndexed = indexed.toJsonValue();
        assertEquals(eager, fromIndexed);
    }

    @Test
    public void testToJsonValueWithEscapes() throws Exception {
        String json = "{\"msg\":\"hello\\nworld\\t!\"}";
        JsonValue eager = JsonParser.parse(json);
        IndexedJsonValue indexed = IndexedJsonParser.parse(json);
        assertEquals(eager, indexed.toJsonValue());
    }

    // ---- parity with JsonParser for the test resource ----

    @Test
    public void testParseTestJson() throws Exception {
        String json = ResourceUtils.resourceAsString("test.json");
        JsonValue eager = JsonParser.parse(json, JsonParser.Option.DECIMALS);
        IndexedJsonValue indexed = IndexedJsonParser.parse(json, JsonParser.Option.DECIMALS);
        assertEquals(eager, indexed.toJsonValue());
    }

    @Test
    public void testParseStreamInfoJson() throws Exception {
        String json = ResourceUtils.resourceAsString("stream_info.json");
        JsonValue eager = JsonParser.parse(json);
        IndexedJsonValue indexed = IndexedJsonParser.parse(json, JsonParser.Option.DECIMALS);
        assertEquals(eager, indexed.toJsonValue());
    }

    // ---- byte array and unchecked entry points ----

    @Test
    public void testParseFromBytes() throws Exception {
        byte[] bytes = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);
        IndexedJsonValue v = IndexedJsonParser.parse(bytes);
        assertEquals(Integer.valueOf(1), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseUnchecked() {
        IndexedJsonValue v = IndexedJsonParser.parseUnchecked("{\"y\":\"z\"}");
        assertEquals("z", v.getMap().get("y").getString());
    }

    @Test
    public void testParseUncheckedInvalid() {
        assertThrows(RuntimeException.class, () -> IndexedJsonParser.parseUnchecked("{invalid}"));
    }

    // ---- error cases ----

    @Test
    public void testInvalidJson() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("{invalid}"));
    }

    @Test
    public void testUnterminatedString() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("\"unterminated"));
    }

    @Test
    public void testLeadingZeroRejected() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("01"));
    }

    @Test
    public void testNullInput() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse((char[]) null);
        assertSame(IndexedJsonValue.NULL, v);
    }

    // ---- laziness verification ----

    @Test
    public void testStringNotCopiedUntilAccessed() throws Exception {
        // Parse a large object — the string values should not be materialized
        // until getString() is called. We verify this indirectly by checking
        // that parsing succeeds and values are correct on access.
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 100; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"key").append(i).append("\":\"value").append(i).append("\"");
        }
        sb.append("}");

        IndexedJsonValue v = IndexedJsonParser.parse(sb.toString());
        Map<String, IndexedJsonValue> map = v.getMap();
        assertNotNull(map);
        assertEquals(100, map.size());

        // Spot-check a few values — these trigger the lazy copy
        assertEquals("value0", map.get("key0").getString());
        assertEquals("value50", map.get("key50").getString());
        assertEquals("value99", map.get("key99").getString());
    }

    @Test
    public void testNumberNotParsedUntilAccessed() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":12345,\"b\":9.99}",
            JsonParser.Option.DECIMALS);
        Map<String, IndexedJsonValue> map = v.getMap();

        // Numbers are lazy — accessing them parses for the first time
        assertEquals(Integer.valueOf(12345), map.get("a").getInteger());
        assertEquals(new BigDecimal("9.99"), map.get("b").getBigDecimal());

        // Second access should return cached values
        assertEquals(Integer.valueOf(12345), map.get("a").getInteger());
    }

    // ---- UTF-8 strings ----

    @Test
    public void testUtf8Strings() throws Exception {
        List<String> utfStrings = ResourceUtils.resourceAsLines("utf8-only-no-ws-test-strings.txt");
        for (String u : utfStrings) {
            String json = "{\"v\":\"" + Encoding.jsonEncode(u) + "\"}";
            JsonValue eager = JsonParser.parse(json);
            IndexedJsonValue indexed = IndexedJsonParser.parse(json);
            assertEquals(eager.map.get("v").string, indexed.getMap().get("v").getString(),
                "UTF string mismatch for: " + u);
        }
    }

    // ---- integers-only mode (the default) ----

    @Test
    public void testIntegersOnlyBasic() throws Exception {
        // integers-only is the default — no options needed
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":42,\"b\":-7,\"c\":3000000000}");
        Map<String, IndexedJsonValue> map = v.getMap();
        assertNotNull(map);
        assertEquals(Integer.valueOf(42), map.get("a").getInteger());
        assertEquals(Integer.valueOf(-7), map.get("b").getInteger());
        assertEquals(Long.valueOf(3000000000L), map.get("c").getLong());
    }

    @Test
    public void testIntegersOnlyLargeValues() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"max\":9223372036854775807,\"min\":-9223372036854775808}");
        Map<String, IndexedJsonValue> map = v.getMap();
        assertEquals(Long.valueOf(Long.MAX_VALUE), map.get("max").getLong());
        assertEquals(Long.valueOf(Long.MIN_VALUE), map.get("min").getLong());
    }

    @Test
    public void testIntegersOnlyZero() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"z\":0}");
        assertEquals(Integer.valueOf(0), v.getMap().get("z").getInteger());
    }

    @Test
    public void testIntegersOnlyWithStringsAndBooleans() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse(
            "{\"name\":\"test\",\"count\":99,\"flag\":true,\"items\":[1,2,3]}");
        Map<String, IndexedJsonValue> map = v.getMap();
        assertEquals("test", map.get("name").getString());
        assertEquals(Integer.valueOf(99), map.get("count").getInteger());
        assertSame(IndexedJsonValue.TRUE, map.get("flag"));
        assertEquals(3, map.get("items").getArray().size());
    }

    @Test
    public void testIntegersOnlyStreamInfoV3() throws Exception {
        // stream_info.json has only integer numbers — perfect for the default mode
        String json = ResourceUtils.resourceAsString("stream_info.json");
        IndexedJsonValue v = IndexedJsonParser.parse(json);
        IndexedJsonValue config = IndexedJsonValueUtils.readMapObjectOrNull(v, "config");
        assertNotNull(config);
        assertEquals("streamName", IndexedJsonValueUtils.readString(config, "name"));
        assertEquals(Long.valueOf(100000000000L), IndexedJsonValueUtils.readLong(config, "max_age"));
        assertEquals(Integer.valueOf(4), IndexedJsonValueUtils.readInteger(config, "max_msg_size"));
        assertEquals(Long.valueOf(82942), IndexedJsonValueUtils.readLong(config, "first_seq"));
    }

    @Test
    public void testIntegersOnlyConsumerInfoV3() throws Exception {
        String json = ResourceUtils.resourceAsString("consumer_info.json");
        IndexedJsonValue v = IndexedJsonParser.parse(json);
        IndexedJsonValue config = IndexedJsonValueUtils.readMapObjectOrNull(v, "config");
        assertNotNull(config);
        assertEquals("foo-name", IndexedJsonValueUtils.readString(config, "name"));
        assertEquals(Long.valueOf(30000000000L), IndexedJsonValueUtils.readLong(config, "ack_wait"));
        assertEquals(Long.valueOf(6666666666L), IndexedJsonValueUtils.readLong(config, "max_bytes"));
    }

    @Test
    public void testKeepNullsWithDefaultIntegersOnly() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":1,\"b\":null}".toCharArray(),
            JsonParser.Option.KEEP_NULLS);
        Map<String, IndexedJsonValue> map = v.getMap();
        assertNotNull(map);
        assertEquals(Integer.valueOf(1), map.get("a").getInteger());
        assertTrue(map.containsKey("b"));
        assertSame(IndexedJsonValue.NULL, map.get("b"));
    }

    // ---- DECIMALS mode (opt-in) ----

    @Test
    public void testDecimalsMode() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"pi\":3.14,\"n\":42}",
            JsonParser.Option.DECIMALS);
        assertEquals(new BigDecimal("3.14"), v.getMap().get("pi").getBigDecimal());
        assertEquals(Integer.valueOf(42), v.getMap().get("n").getInteger());
    }

    // -----------------------------------------------------------------------
    // Additional coverage tests
    // -----------------------------------------------------------------------

    // -- Parser entry point variants --

    @Test
    public void testParseCharArrayWithStartIndex() throws Exception {
        char[] json = "   {\"x\":1}".toCharArray();
        IndexedJsonValue v = IndexedJsonParser.parse(json, 3);
        assertEquals(Integer.valueOf(1), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseStringWithStartIndex() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("   {\"x\":2}", 3);
        assertEquals(Integer.valueOf(2), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseCharArrayWithOptions() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":null}".toCharArray(),
            JsonParser.Option.KEEP_NULLS);
        assertTrue(v.getMap().containsKey("a"));
    }

    @Test
    public void testParseStringWithLegacyOptions() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":null}", JsonParser.Option.KEEP_NULLS);
        assertTrue(v.getMap().containsKey("a"));
    }

    @Test
    public void testParseByteArrayWithLegacyOptions() throws Exception {
        byte[] json = "{\"a\":null}".getBytes(StandardCharsets.UTF_8);
        IndexedJsonValue v = IndexedJsonParser.parse(json, JsonParser.Option.KEEP_NULLS);
        assertTrue(v.getMap().containsKey("a"));
    }

    @Test
    public void testParseByteArrayWithOptions() throws Exception {
        byte[] json = "{\"x\":5}".getBytes(StandardCharsets.UTF_8);
        IndexedJsonValue v = IndexedJsonParser.parse(json, JsonParser.Option.DECIMALS);
        assertEquals(Integer.valueOf(5), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseUncheckedCharArray() {
        IndexedJsonValue v = IndexedJsonParser.parseUnchecked("{\"x\":3}".toCharArray());
        assertEquals(Integer.valueOf(3), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseUncheckedByteArray() {
        IndexedJsonValue v = IndexedJsonParser.parseUnchecked("{\"x\":4}".getBytes(StandardCharsets.UTF_8));
        assertEquals(Integer.valueOf(4), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseUncheckedStringWithOptions() {
        IndexedJsonValue v = IndexedJsonParser.parseUnchecked("{\"x\":6}",
            JsonParser.Option.DECIMALS);
        assertEquals(Integer.valueOf(6), v.getMap().get("x").getInteger());
    }

    @Test
    public void testNegativeStartIndexThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> IndexedJsonParser.parse("{}", -1));
    }

    @Test
    public void testNegativeStartIndexCharArrayThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> IndexedJsonParser.parse("{}".toCharArray(), -1));
    }

    // -- Parser error cases --

    @Test
    public void testDirectNestedObject() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("{{}}"));
    }

    @Test
    public void testDirectNestedArray() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("{[]}"));
    }

    @Test
    public void testMissingColonAfterKey() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("{\"key\" \"value\"}"));
    }

    @Test
    public void testBadTokenAfterValue() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("{\"a\":1 \"b\":2}"));
    }

    @Test
    public void testUnterminatedObject() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("{\"a\":1"));
    }

    @Test
    public void testLeadingZeroNegativeRejected() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("-01"));
    }

    @Test
    public void testUnterminatedEscapeInString() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("\"abc\\"));
    }

    @Test
    public void testNewlineInString() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("\"abc\ndef\""));
    }

    @Test
    public void testCarriageReturnInString() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("\"abc\rdef\""));
    }

    @Test
    public void testInvalidEscapeChar() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("\"\\x\""));
    }

    @Test
    public void testIncompleteUnicodeEscape() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("\"\\u00\""));
    }

    @Test
    public void testInvalidHexInUnicode() {
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse("\"\\u00GG\""));
    }

    @Test
    public void testInvalidBytesThrows() {
        byte[] bad = new byte[]{(byte)0xFF, (byte)0xFE};
        assertThrows(JsonParseException.class, () -> IndexedJsonParser.parse(bad));
    }

    // -- Key string escapes --

    @Test
    public void testEscapedMapKeys() throws Exception {
        String json = "{\"key\\nwith\\tnewlines\":\"val\",\"key\\\\slash\":\"val2\",\"key\\/fwd\":\"val3\"}";
        IndexedJsonValue v = IndexedJsonParser.parse(json);
        Map<String, IndexedJsonValue> map = v.getMap();
        assertEquals("val", map.get("key\nwith\tnewlines").getString());
        assertEquals("val2", map.get("key\\slash").getString());
        assertEquals("val3", map.get("key/fwd").getString());
    }

    @Test
    public void testUnicodeMapKey() throws Exception {
        String json = "{\"\\u0041\\u0042\":\"val\"}";
        IndexedJsonValue v = IndexedJsonParser.parse(json);
        assertEquals("val", v.getMap().get("AB").getString());
    }

    @Test
    public void testUnicodeMapKeyUppercaseHex() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"\\u00AB\":\"val\"}");
        assertEquals("val", v.getMap().get("\u00AB").getString());
    }

    @Test
    public void testUnicodeMapKeyLowercaseHex() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"\\u00ab\":\"val\"}");
        assertEquals("val", v.getMap().get("\u00ab").getString());
    }

    @Test
    public void testMapKeyWithAllEscapes() throws Exception {
        String json = "{\"\\b\\f\\r\\n\\t\\\"\\'\\\\\\/\":\"val\"}";
        IndexedJsonValue v = IndexedJsonParser.parse(json);
        assertEquals("val", v.getMap().get("\b\f\r\n\t\"'\\/").getString());
    }

    // -- String escape extraction --

    @Test
    public void testStringEscapesAllTypes() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"s\":\"\\b\\f\\r\\n\\t\\\\\\/\\\"\\'\"}");
        assertEquals("\b\f\r\n\t\\/\"'", v.getMap().get("s").getString());
    }

    // -- IndexedJsonValue accessor edge cases --

    @Test
    public void testGetStringOnNonString() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertNull(v.getString());
    }

    @Test
    public void testGetBooleanFalse() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("false");
        assertEquals(Boolean.FALSE, v.getBoolean());
    }

    @Test
    public void testGetBooleanOnNonBool() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertNull(v.getBoolean());
    }

    @Test
    public void testGetIntDefault() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("3000000000"); // too big for int
        assertEquals(99, v.getInt(99));
    }

    @Test
    public void testGetLongOnNonNumber() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("\"text\"");
        assertNull(v.getLong());
        assertEquals(42L, v.getLong(42L));
    }

    @Test
    public void testGetDoubleOnNonDouble() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertNull(v.getDouble());
    }

    @Test
    public void testGetBigDecimalOnNonDecimal() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertNull(v.getBigDecimal());
    }

    @Test
    public void testGetBigIntegerOnNonBigInt() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertNull(v.getBigInteger());
    }

    @Test
    public void testGetNumber() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertNotNull(v.getNumber());
        assertEquals(42, v.getNumber().intValue());
    }

    @Test
    public void testGetNumberOnString() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("\"text\"");
        assertNull(v.getNumber());
    }

    @Test
    public void testIntegerFromLongInRange() throws Exception {
        // Value stored as long but fits in int range
        IndexedJsonValue v = IndexedJsonParser.parse("{\"n\":42}", JsonParser.Option.DECIMALS);
        assertEquals(Integer.valueOf(42), v.getMap().get("n").getInteger());
    }

    @Test
    public void testGetLongFromInteger() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertEquals(Long.valueOf(42L), v.getLong());
    }

    // -- BigInteger in integers-only mode --

    @Test
    public void testBigIntegerInIntegersOnlyMode() throws Exception {
        String huge = "99999999999999999999999999999";
        IndexedJsonValue v = IndexedJsonParser.parse(huge);
        assertNotNull(v.getBigInteger());
        assertEquals(new BigInteger(huge), v.getBigInteger());
    }

    // -- Decimals mode: Double fallback (hex float), BigInteger --

    @Test
    public void testDecimalsModeScientificNotation() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("1.5e10", JsonParser.Option.DECIMALS);
        assertNotNull(v.getBigDecimal());
    }

    @Test
    public void testDecimalsModeBigInteger() throws Exception {
        String huge = "99999999999999999999999999999";
        IndexedJsonValue v = IndexedJsonParser.parse(huge, JsonParser.Option.DECIMALS);
        assertNotNull(v.getBigInteger());
        assertEquals(new BigInteger(huge), v.getBigInteger());
    }

    // -- toJsonValue edge cases --

    @Test
    public void testToJsonValueNull() {
        assertEquals(JsonValue.NULL, IndexedJsonValue.NULL.toJsonValue());
    }

    @Test
    public void testToJsonValueEmptyMap() {
        assertEquals(JsonValue.EMPTY_MAP, IndexedJsonValue.EMPTY_MAP.toJsonValue());
    }

    @Test
    public void testToJsonValueEmptyArray() {
        assertEquals(JsonValue.EMPTY_ARRAY, IndexedJsonValue.EMPTY_ARRAY.toJsonValue());
    }

    @Test
    public void testToJsonValueBigInteger() throws Exception {
        String huge = "99999999999999999999999999999";
        IndexedJsonValue v = IndexedJsonParser.parse(huge);
        JsonValue jv = v.toJsonValue();
        assertEquals(new BigInteger(huge), jv.bi);
    }

    @Test
    public void testToJsonValueDouble() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("-0", JsonParser.Option.DECIMALS);
        JsonValue jv = v.toJsonValue();
        assertEquals(JsonValueType.DOUBLE, jv.type);
    }

    // -- IndexedJsonValueUtils additional coverage --

    @Test
    public void testUtilsReadValueNull() {
        assertNull(IndexedJsonValueUtils.readValue(null, "key"));
    }

    @Test
    public void testUtilsReadValueNonMap() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertNull(IndexedJsonValueUtils.readValue(v, "key"));
    }

    @Test
    public void testUtilsReadNanosAsDurationWithDefault() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"x\":\"notnum\"}");
        Duration dflt = Duration.ofSeconds(5);
        assertEquals(dflt, IndexedJsonValueUtils.readNanosAsDuration(v, "missing", dflt));
    }

    @Test
    public void testUtilsReadBytesWithCharset() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"d\":\"hello\"}");
        byte[] bytes = IndexedJsonValueUtils.readBytes(v, "d", StandardCharsets.US_ASCII);
        assertNotNull(bytes);
        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII), bytes);
        assertNull(IndexedJsonValueUtils.readBytes(v, "missing", StandardCharsets.US_ASCII));
    }

    @Test
    public void testUtilsReadBase64Basic() throws Exception {
        String encoded = java.util.Base64.getEncoder().encodeToString("test".getBytes());
        IndexedJsonValue v = IndexedJsonParser.parse("{\"b\":\"" + encoded + "\"}");
        byte[] decoded = IndexedJsonValueUtils.readBase64Basic(v, "b");
        assertNotNull(decoded);
        assertArrayEquals("test".getBytes(), decoded);
        assertNull(IndexedJsonValueUtils.readBase64Basic(v, "missing"));
    }

    @Test
    public void testUtilsReadBase64Url() throws Exception {
        String encoded = java.util.Base64.getUrlEncoder().encodeToString("test".getBytes());
        IndexedJsonValue v = IndexedJsonParser.parse("{\"b\":\"" + encoded + "\"}");
        byte[] decoded = IndexedJsonValueUtils.readBase64Url(v, "b");
        assertNotNull(decoded);
        assertArrayEquals("test".getBytes(), decoded);
        assertNull(IndexedJsonValueUtils.readBase64Url(v, "missing"));
    }

    @Test
    public void testUtilsReadMapMapOrEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"m\":{\"k\":\"v\"}}");
        Map<String, IndexedJsonValue> map = IndexedJsonValueUtils.readMapMapOrEmpty(v, "m");
        assertEquals(1, map.size());
        // missing key returns empty map
        Map<String, IndexedJsonValue> empty = IndexedJsonValueUtils.readMapMapOrEmpty(v, "missing");
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testUtilsReadStringMapOrEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"m\":{\"k\":\"v\",\"n\":42}}");
        Map<String, String> map = IndexedJsonValueUtils.readStringMapOrEmpty(v, "m");
        assertEquals(1, map.size()); // only string values
        assertEquals("v", map.get("k"));
        // missing key
        assertTrue(IndexedJsonValueUtils.readStringMapOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsReadArrayOrEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[1,2]}");
        assertEquals(2, IndexedJsonValueUtils.readArrayOrEmpty(v, "a").size());
        assertTrue(IndexedJsonValueUtils.readArrayOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsReadStringListWithIgnoreBlank() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"s\":[\"a\",\" \",\"b\",\"\"]}");
        List<String> withBlanks = IndexedJsonValueUtils.readStringListOrNull(v, "s", false);
        assertNotNull(withBlanks);
        assertEquals(4, withBlanks.size());
        List<String> noBlanks = IndexedJsonValueUtils.readStringListOrNull(v, "s", true);
        assertNotNull(noBlanks);
        assertEquals(2, noBlanks.size());
        // empty with ignoreBlank
        assertEquals(2, IndexedJsonValueUtils.readStringListOrEmpty(v, "s", true).size());
    }

    @Test
    public void testUtilsReadIntegerListOrEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"n\":[1,2]}");
        assertEquals(Arrays.asList(1, 2), IndexedJsonValueUtils.readIntegerListOrEmpty(v, "n"));
        assertTrue(IndexedJsonValueUtils.readIntegerListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsReadLongListOrEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"n\":[1,2]}");
        assertEquals(Arrays.asList(1L, 2L), IndexedJsonValueUtils.readLongListOrEmpty(v, "n"));
        assertTrue(IndexedJsonValueUtils.readLongListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsReadNanosAsDurationListOrEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"d\":[1000000000]}");
        List<Duration> durations = IndexedJsonValueUtils.readNanosAsDurationListOrEmpty(v, "d");
        assertEquals(1, durations.size());
        assertEquals(Duration.ofSeconds(1), durations.get(0));
        assertTrue(IndexedJsonValueUtils.readNanosAsDurationListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsListOfOrNull() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[\"a\",\"b\"]");
        List<String> list = IndexedJsonValueUtils.listOfOrNull(v, IndexedJsonValue::getString);
        assertNotNull(list);
        assertEquals(Arrays.asList("a", "b"), list);
        assertNull(IndexedJsonValueUtils.listOfOrNull(null, IndexedJsonValue::getString));
    }

    @Test
    public void testUtilsListOfOrEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[1,2]");
        List<Integer> list = IndexedJsonValueUtils.listOfOrEmpty(v, IndexedJsonValue::getInteger);
        assertEquals(Arrays.asList(1, 2), list);
        assertTrue(IndexedJsonValueUtils.listOfOrEmpty(null, IndexedJsonValue::getInteger).isEmpty());
    }

    @Test
    public void testUtilsConvertToList() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[1,\"skip\",3]");
        List<IndexedJsonValue> arr = v.getArray();
        assertNotNull(arr);
        List<Integer> ints = IndexedJsonValueUtils.convertToList(arr, IndexedJsonValue::getInteger);
        assertEquals(Arrays.asList(1, 3), ints); // "skip" returns null for getInteger, filtered out
    }

    // -- Integer-only mode: non-digit fallback to full parse --

    @Test
    public void testIntegersOnlyFallsBackForDecimal() throws Exception {
        // In integers-only mode, a decimal will fallback to ensureNumberFull
        IndexedJsonValue v = IndexedJsonParser.parse("{\"n\":3.14}");
        // The type is INTEGER (default marker) but ensureIntegerOnly will detect '.'
        // and fall back to ensureNumberFull which handles decimals
        assertNotNull(v.getMap().get("n").getBigDecimal());
        assertEquals(new BigDecimal("3.14"), v.getMap().get("n").getBigDecimal());
    }

    // -- Dangling comma in arrays --

    @Test
    public void testTrailingCommaInArray() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[1,2,]");
        assertEquals(2, v.getArray().size());
    }

    // -----------------------------------------------------------------------
    // Branch coverage: legitimate data paths
    // -----------------------------------------------------------------------

    @Test
    public void testGetIntWithValidValue() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertEquals(42, v.getInt(99)); // non-null path of getInt(dflt)
    }

    @Test
    public void testGetLongDefaultWithValidValue() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("3000000000");
        assertEquals(3000000000L, v.getLong(0L)); // non-null path of getLong(dflt)
    }

    @Test
    public void testGetIntegerFromLongInRange() throws Exception {
        // A number stored as LONG that fits in int range — exercises the LONG-to-int conversion in getInteger()
        IndexedJsonValue v = IndexedJsonParser.parse("100");
        // Force through the long path by reading getLong first, verifying it's an int too
        assertNotNull(v.getLong());
        assertNotNull(v.getInteger());
        assertEquals(Integer.valueOf(100), v.getInteger());
    }

    @Test
    public void testScientificNotationUpperE() throws Exception {
        // Covers the 'E' branch in isDecimalNotation
        IndexedJsonValue v = IndexedJsonParser.parse("1E5", JsonParser.Option.DECIMALS);
        assertNotNull(v.getBigDecimal());
    }

    @Test
    public void testScientificNotationLowerE() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("3.14e-2", JsonParser.Option.DECIMALS);
        assertNotNull(v.getBigDecimal());
    }

    @Test
    public void testNegativeZeroDecimalNotation() throws Exception {
        // Covers the "-0" branch in isDecimalNotation
        IndexedJsonValue v = IndexedJsonParser.parse("-0.0", JsonParser.Option.DECIMALS);
        assertNotNull(v.getDouble());
    }

    @Test
    public void testUnicodeEscapeUppercaseHex() throws Exception {
        // Covers A-F hex digit branch in extractString
        IndexedJsonValue v = IndexedJsonParser.parse("\"\\u00AB\"");
        assertEquals("\u00AB", v.getString());
    }

    @Test
    public void testUnicodeEscapeLowercaseHex() throws Exception {
        // Covers a-f hex digit branch in extractString
        IndexedJsonValue v = IndexedJsonParser.parse("\"\\u00ab\"");
        assertEquals("\u00ab", v.getString());
    }

    @Test
    public void testUnicodeEscapeDigitsOnly() throws Exception {
        // Covers 0-9 hex digit branch
        IndexedJsonValue v = IndexedJsonParser.parse("\"\\u0039\"");
        assertEquals("9", v.getString());
    }

    @Test
    public void testParseNullCharArrayWithOptions() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse((char[]) null, JsonParser.Option.DECIMALS);
        assertSame(IndexedJsonValue.NULL, v);
    }

    @Test
    public void testParseStringWithDecimalsOption() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"pi\":3.14}", JsonParser.Option.DECIMALS);
        assertEquals(new BigDecimal("3.14"), v.getMap().get("pi").getBigDecimal());
    }

    @Test
    public void testParseByteArrayWithKeepNullsAndDecimals() throws Exception {
        byte[] json = "{\"a\":null,\"b\":1.5}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        IndexedJsonValue v = IndexedJsonParser.parse(json, JsonParser.Option.KEEP_NULLS, JsonParser.Option.DECIMALS);
        assertTrue(v.getMap().containsKey("a"));
        assertNotNull(v.getMap().get("b").getBigDecimal());
    }

    @Test
    public void testUtilsReadDateValid() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"d\":\"2021-01-25T20:09:10.6225191Z\"}");
        assertNotNull(IndexedJsonValueUtils.readDate(v, "d"));
    }

    @Test
    public void testUtilsReadMapObjectOrEmptyWithData() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"m\":{\"k\":\"v\"}}");
        IndexedJsonValue m = IndexedJsonValueUtils.readMapObjectOrEmpty(v, "m");
        assertNotNull(m.getMap());
        assertEquals(1, m.getMap().size());
    }

    @Test
    public void testUtilsReadWithRequiredTypeMismatch() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"s\":\"hello\"}");
        // readInteger on a STRING value — type mismatch returns null
        assertNull(IndexedJsonValueUtils.readInteger(v, "s"));
    }

    @Test
    public void testUtilsReadStringListEmptyArray() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[]}");
        assertNull(IndexedJsonValueUtils.readStringListOrNull(v, "a"));
        assertTrue(IndexedJsonValueUtils.readStringListOrEmpty(v, "a").isEmpty());
    }

    @Test
    public void testUtilsReadIntegerListEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[]}");
        assertNull(IndexedJsonValueUtils.readIntegerListOrNull(v, "a"));
    }

    @Test
    public void testUtilsReadLongListEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[]}");
        assertNull(IndexedJsonValueUtils.readLongListOrNull(v, "a"));
    }

    @Test
    public void testUtilsConvertToStringListWithStrings() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[\"a\",\"b\"]");
        List<String> list = IndexedJsonValueUtils.convertToStringList(v.getArray(), false);
        assertEquals(java.util.Arrays.asList("a", "b"), list);
    }

    @Test
    public void testToJsonValueBigDecimalViaDecimals() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("3.14", JsonParser.Option.DECIMALS);
        JsonValue jv = v.toJsonValue();
        assertEquals(JsonValueType.BIG_DECIMAL, jv.type);
    }

    // -----------------------------------------------------------------------
    // Utils: type mismatch, mixed-type arrays, non-MAP inputs
    // -----------------------------------------------------------------------

    @Test
    public void testUtilsReadOnNonMapType() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("\"just a string\"");
        assertNull(IndexedJsonValueUtils.readValue(v, "key"));
        assertNull(IndexedJsonValueUtils.readString(v, "key"));
        assertEquals("dflt", IndexedJsonValueUtils.readString(v, "key", "dflt"));
        assertNull(IndexedJsonValueUtils.readInteger(v, "key"));
        assertNull(IndexedJsonValueUtils.readLong(v, "key"));
        assertNull(IndexedJsonValueUtils.readBoolean(v, "key"));
        assertEquals(99, IndexedJsonValueUtils.readInteger(v, "key", 99));
        assertEquals(99L, IndexedJsonValueUtils.readLong(v, "key", 99L));
        assertTrue(IndexedJsonValueUtils.readBoolean(v, "key", true));
    }

    @Test
    public void testUtilsReadStringOnIntegerValue() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"n\":42}");
        // readString with default requires STRING type — n is INTEGER, so returns default
        assertEquals("dflt", IndexedJsonValueUtils.readString(v, "n", "dflt"));
    }

    @Test
    public void testUtilsConvertMixedArrayToIntegers() throws Exception {
        // Array with strings mixed in — non-integers filtered out
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[1,\"skip\",3]}");
        List<Integer> ints = IndexedJsonValueUtils.readIntegerListOrNull(v, "a");
        assertNotNull(ints);
        assertEquals(java.util.Arrays.asList(1, 3), ints);
    }

    @Test
    public void testUtilsConvertMixedArrayToLongs() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[1,\"skip\",3]}");
        List<Long> longs = IndexedJsonValueUtils.readLongListOrNull(v, "a");
        assertNotNull(longs);
        assertEquals(java.util.Arrays.asList(1L, 3L), longs);
    }

    @Test
    public void testUtilsConvertMixedArrayToStrings() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[\"a\",42,\"b\"]}");
        List<String> strs = IndexedJsonValueUtils.readStringListOrNull(v, "a");
        assertNotNull(strs);
        assertEquals(java.util.Arrays.asList("a", "b"), strs);
    }

    @Test
    public void testUtilsListOfOrNullOnNonArray() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("42");
        assertNull(IndexedJsonValueUtils.listOfOrNull(v, IndexedJsonValue::getString));
        assertTrue(IndexedJsonValueUtils.listOfOrEmpty(v, IndexedJsonValue::getString).isEmpty());
    }

    @Test
    public void testUtilsListOfOrNullEmptyArray() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("[]");
        assertNull(IndexedJsonValueUtils.listOfOrNull(v, IndexedJsonValue::getString));
        assertTrue(IndexedJsonValueUtils.listOfOrEmpty(v, IndexedJsonValue::getString).isEmpty());
    }

    @Test
    public void testUtilsReadNanosAsDurationList() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"d\":[1000000000,2000000000]}");
        List<Duration> durations = IndexedJsonValueUtils.readNanosAsDurationListOrNull(v, "d");
        assertNotNull(durations);
        assertEquals(2, durations.size());
        assertNull(IndexedJsonValueUtils.readNanosAsDurationListOrNull(v, "missing"));
        assertTrue(IndexedJsonValueUtils.readNanosAsDurationListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsReadDateMissing() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"x\":1}");
        assertNull(IndexedJsonValueUtils.readDate(v, "missing"));
    }

    @Test
    public void testUtilsReadBytesMissing() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"x\":1}");
        assertNull(IndexedJsonValueUtils.readBytes(v, "missing"));
    }

    @Test
    public void testUtilsReadStringMapOrNullMissing() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"x\":1}");
        assertNull(IndexedJsonValueUtils.readStringMapOrNull(v, "missing"));
    }

    // -----------------------------------------------------------------------
    // Value: number boundary conversions
    // -----------------------------------------------------------------------

    @Test
    public void testGetIntegerFromLongOutOfRange() throws Exception {
        // Long.MAX_VALUE doesn't fit in int — getInteger should return null
        IndexedJsonValue v = IndexedJsonParser.parse(String.valueOf(Long.MAX_VALUE));
        assertNotNull(v.getLong());
        assertNull(v.getInteger());
        // Long.MIN_VALUE also out of int range
        IndexedJsonValue v2 = IndexedJsonParser.parse(String.valueOf(Long.MIN_VALUE));
        assertNotNull(v2.getLong());
        assertNull(v2.getInteger());
    }

    @Test
    public void testHexFloatViaDecimals() throws Exception {
        // Hex float: BigDecimal constructor fails, falls back to Double.parseDouble
        IndexedJsonValue v = IndexedJsonParser.parse("0x1.0P-1074", JsonParser.Option.DECIMALS);
        assertNotNull(v.getDouble());
        assertEquals(JsonValueType.DOUBLE, v.toJsonValue().type);
    }

    @Test
    public void testToJsonValueLong() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("3000000000");
        JsonValue jv = v.toJsonValue();
        assertEquals(JsonValueType.LONG, jv.type);
        assertEquals(Long.valueOf(3000000000L), jv.l);
    }

    // -----------------------------------------------------------------------
    // Utils: null jv, found-value branches, type-checked reads
    // -----------------------------------------------------------------------

    @Test
    public void testUtilsReadValueWithNullJv() {
        assertNull(IndexedJsonValueUtils.readValue(null, "key"));
        assertNull(IndexedJsonValueUtils.readString(null, "key"));
        assertEquals("dflt", IndexedJsonValueUtils.readString(null, "key", "dflt"));
        assertNull(IndexedJsonValueUtils.readInteger(null, "key"));
        assertEquals(0, IndexedJsonValueUtils.readInteger(null, "key", 0));
        assertNull(IndexedJsonValueUtils.readLong(null, "key"));
        assertEquals(0L, IndexedJsonValueUtils.readLong(null, "key", 0L));
        assertNull(IndexedJsonValueUtils.readBoolean(null, "key"));
        assertFalse(IndexedJsonValueUtils.readBoolean(null, "key", false));
        assertNull(IndexedJsonValueUtils.readDate(null, "key"));
        assertNull(IndexedJsonValueUtils.readNanosAsDuration(null, "key"));
        assertNull(IndexedJsonValueUtils.readNanosAsDuration(null, "key", null));
        assertNull(IndexedJsonValueUtils.readBytes(null, "key"));
        assertNull(IndexedJsonValueUtils.readMapObjectOrNull(null, "key"));
        assertNull(IndexedJsonValueUtils.readMapMapOrNull(null, "key"));
        assertNull(IndexedJsonValueUtils.readArrayOrNull(null, "key"));
        assertNull(IndexedJsonValueUtils.readStringListOrNull(null, "key"));
        assertNull(IndexedJsonValueUtils.readIntegerListOrNull(null, "key"));
        assertNull(IndexedJsonValueUtils.readLongListOrNull(null, "key"));
    }

    @Test
    public void testUtilsFoundValueBranches() throws Exception {
        // Ensures the "value found" branch is hit for every read*WithDefault method
        IndexedJsonValue v = IndexedJsonParser.parse(
            "{\"i\":42,\"l\":3000000000,\"b\":true,\"s\":\"hello\",\"d\":1000000000}");
        // readInteger with default — value IS found
        assertEquals(42, IndexedJsonValueUtils.readInteger(v, "i", 0));
        // readLong with default — value IS found
        assertEquals(3000000000L, IndexedJsonValueUtils.readLong(v, "l", 0L));
        // readBoolean with default — value IS found
        assertTrue(IndexedJsonValueUtils.readBoolean(v, "b", false));
        // readNanosAsDuration — value IS found
        assertNotNull(IndexedJsonValueUtils.readNanosAsDuration(v, "d"));
        // readNanosAsDuration with default — value IS found
        assertEquals(Duration.ofSeconds(1),
            IndexedJsonValueUtils.readNanosAsDuration(v, "d", Duration.ZERO));
    }

    @Test
    public void testUtilsTypedReadKeyExistsWrongType() throws Exception {
        // Key exists but has wrong type — exercises the v.type != requiredType branch
        IndexedJsonValue v = IndexedJsonParser.parse("{\"n\":42,\"s\":\"hello\"}");
        // readString(key, dflt) requires STRING — "n" is INTEGER
        assertEquals("dflt", IndexedJsonValueUtils.readString(v, "n", "dflt"));
        // readMapObjectOrNull requires MAP — "s" is STRING
        assertNull(IndexedJsonValueUtils.readMapObjectOrNull(v, "s"));
        // readArrayOrNull requires ARRAY — "n" is INTEGER
        assertNull(IndexedJsonValueUtils.readArrayOrNull(v, "n"));
    }

    @Test
    public void testUtilsStringListOrEmptyMissing() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"x\":1}");
        assertTrue(IndexedJsonValueUtils.readStringListOrEmpty(v, "missing").isEmpty());
        assertTrue(IndexedJsonValueUtils.readStringListOrEmpty(v, "missing", true).isEmpty());
    }

    @Test
    public void testUtilsIntegerListOrNullEmpty() throws Exception {
        // Array exists but contains no integers — returns null
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[\"not_int\"]}");
        assertNull(IndexedJsonValueUtils.readIntegerListOrNull(v, "a"));
    }

    @Test
    public void testUtilsLongListOrNullEmpty() throws Exception {
        IndexedJsonValue v = IndexedJsonParser.parse("{\"a\":[\"not_long\"]}");
        assertNull(IndexedJsonValueUtils.readLongListOrNull(v, "a"));
    }

    @Test
    public void testUtilsReadValueSuccess() throws Exception {
        // Exercises the successful path of readValue (jv is MAP, key exists)
        IndexedJsonValue v = IndexedJsonParser.parse("{\"k\":\"v\"}");
        IndexedJsonValue result = IndexedJsonValueUtils.readValue(v, "k");
        assertNotNull(result);
        assertEquals("v", result.getString());
    }

    @Test
    public void testUtilsTypedReadSuccess() throws Exception {
        // Exercises the typed read() where key exists and type matches (line 66-71)
        IndexedJsonValue v = IndexedJsonParser.parse("{\"s\":\"hello\",\"m\":{\"a\":1}}");
        // readString with default — key found, type STRING matches
        assertEquals("hello", IndexedJsonValueUtils.readString(v, "s", "dflt"));
        // readMapObjectOrNull — key found, type MAP matches
        assertNotNull(IndexedJsonValueUtils.readMapObjectOrNull(v, "m"));
    }
}
