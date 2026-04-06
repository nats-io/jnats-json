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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class LazyJsonParsingTests {

    // ---- basic leaf values ----

    @Test
    public void testStringValue() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("\"hello\"");
        assertEquals("hello", v.getString());
    }

    @Test
    public void testStringEscapes() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("\"a\\nb\\tc\\\\d\\\"e\"");
        assertEquals("a\nb\tc\\d\"e", v.getString());
    }

    @Test
    public void testUnicodeEscape() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("\"\\u0041\\u0042\"");
        assertEquals("AB", v.getString());
    }

    @Test
    public void testTrueFalseNull() throws Exception {
        assertSame(LazyJsonValue.TRUE, LazyJsonParser.parse("true"));
        assertSame(LazyJsonValue.FALSE, LazyJsonParser.parse("false"));
        assertSame(LazyJsonValue.NULL, LazyJsonParser.parse("null"));
    }

    @Test
    public void testInteger() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("42");
        assertEquals(Integer.valueOf(42), v.getInteger());
    }

    @Test
    public void testLong() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("3000000000");
        assertEquals(Long.valueOf(3000000000L), v.getLong());
    }

    @Test
    public void testBigDecimal() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("3.14", JsonParser.Option.DECIMALS);
        assertEquals(new BigDecimal("3.14"), v.getBigDecimal());
    }

    // ---- simple flat object ----

    @Test
    public void testFlatObject() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"name\":\"Alice\",\"age\":30}");
        Map<String, LazyJsonValue> map = v.getMap();
        assertNotNull(map);
        assertEquals("Alice", map.get("name").getString());
        assertEquals(Integer.valueOf(30), map.get("age").getInteger());
    }

    @Test
    public void testEmptyObject() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{}");
        assertNotNull(v.getMap());
        assertTrue(v.getMap().isEmpty());
    }

    @Test
    public void testEmptyArray() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("[]");
        assertNotNull(v.getArray());
        assertTrue(v.getArray().isEmpty());
    }

    // ---- nested containers are deferred ----

    @Test
    public void testNestedObjectDeferred() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse(
            "{\"name\":\"test\",\"config\":{\"key\":\"val\",\"num\":42}}");
        Map<String, LazyJsonValue> map = v.getMap();
        assertNotNull(map);
        assertEquals("test", map.get("name").getString());

        // config is a MAP type but hasn't been parsed yet — accessing getMap() triggers it
        LazyJsonValue config = map.get("config");
        assertNotNull(config);
        assertEquals(JsonValueType.MAP, config.type);

        Map<String, LazyJsonValue> configMap = config.getMap();
        assertNotNull(configMap);
        assertEquals("val", configMap.get("key").getString());
        assertEquals(Integer.valueOf(42), configMap.get("num").getInteger());
    }

    @Test
    public void testNestedArrayDeferred() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"items\":[1,2,3]}");
        LazyJsonValue items = v.getMap().get("items");
        assertEquals(JsonValueType.ARRAY, items.type);

        List<LazyJsonValue> arr = items.getArray();
        assertNotNull(arr);
        assertEquals(3, arr.size());
        assertEquals(Integer.valueOf(1), arr.get(0).getInteger());
        assertEquals(Integer.valueOf(3), arr.get(2).getInteger());
    }

    @Test
    public void testDeeplyNestedDeferred() throws Exception {
        String json = "{\"a\":{\"b\":{\"c\":{\"d\":\"deep\"}}}}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        LazyJsonValue a = v.getMap().get("a");
        LazyJsonValue b = a.getMap().get("b");
        LazyJsonValue c = b.getMap().get("c");
        assertEquals("deep", c.getMap().get("d").getString());
    }

    @Test
    public void testNestedEmptyContainers() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"m\":{},\"a\":[]}");
        Map<String, LazyJsonValue> map = v.getMap();
        assertTrue(map.get("m").getMap().isEmpty());
        assertTrue(map.get("a").getArray().isEmpty());
    }

    @Test
    public void testArrayOfObjects() throws Exception {
        String json = "{\"items\":[{\"id\":1},{\"id\":2}]}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        List<LazyJsonValue> items = v.getMap().get("items").getArray();
        assertEquals(2, items.size());
        assertEquals(Integer.valueOf(1), items.get(0).getMap().get("id").getInteger());
        assertEquals(Integer.valueOf(2), items.get(1).getMap().get("id").getInteger());
    }

    @Test
    public void testArrayOfArrays() throws Exception {
        String json = "[[1,2],[3,4]]";
        LazyJsonValue v = LazyJsonParser.parse(json);
        List<LazyJsonValue> outer = v.getArray();
        assertEquals(2, outer.size());
        assertEquals(Integer.valueOf(1), outer.get(0).getArray().get(0).getInteger());
        assertEquals(Integer.valueOf(4), outer.get(1).getArray().get(1).getInteger());
    }

    // ---- skip scan handles strings with braces/brackets ----

    @Test
    public void testSkipHandlesStringWithBraces() throws Exception {
        String json = "{\"a\":{\"msg\":\"contains { and } braces\"},\"b\":42}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        assertEquals(Integer.valueOf(42), v.getMap().get("b").getInteger());
        assertEquals("contains { and } braces",
            v.getMap().get("a").getMap().get("msg").getString());
    }

    @Test
    public void testSkipHandlesStringWithBrackets() throws Exception {
        String json = "{\"a\":[\"has [ and ] brackets\"],\"b\":1}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        assertEquals(Integer.valueOf(1), v.getMap().get("b").getInteger());
        assertEquals("has [ and ] brackets",
            v.getMap().get("a").getArray().get(0).getString());
    }

    @Test
    public void testSkipHandlesEscapedQuotesInStrings() throws Exception {
        String json = "{\"a\":{\"msg\":\"escaped \\\" quote\"},\"b\":1}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        assertEquals(Integer.valueOf(1), v.getMap().get("b").getInteger());
        assertEquals("escaped \" quote",
            v.getMap().get("a").getMap().get("msg").getString());
    }

    @Test
    public void testSkipHandlesEscapedBackslashBeforeQuote() throws Exception {
        // The string value is: ends with backslash\  (the \" is \\\" = escaped backslash + end quote)
        String json = "{\"a\":{\"msg\":\"ends with backslash\\\\\"},\"b\":1}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        assertEquals(Integer.valueOf(1), v.getMap().get("b").getInteger());
        assertEquals("ends with backslash\\",
            v.getMap().get("a").getMap().get("msg").getString());
    }

    // ---- nulls handling ----

    @Test
    public void testNullsDropped() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":1,\"b\":null,\"c\":2}");
        Map<String, LazyJsonValue> map = v.getMap();
        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testKeepNulls() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":1,\"b\":null}",
            JsonParser.Option.KEEP_NULLS);
        assertTrue(v.getMap().containsKey("b"));
        assertSame(LazyJsonValue.NULL, v.getMap().get("b"));
    }

    @Test
    public void testDanglingComma() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":1,}");
        assertEquals(Integer.valueOf(1), v.getMap().get("a").getInteger());
    }

    // ---- LazyJsonValueUtils ----

    @Test
    public void testUtilsReadString() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"name\":\"Bob\"}");
        assertEquals("Bob", LazyJsonValueUtils.readString(v, "name"));
        assertNull(LazyJsonValueUtils.readString(v, "missing"));
    }

    @Test
    public void testUtilsReadInteger() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":42}");
        assertEquals(Integer.valueOf(42), LazyJsonValueUtils.readInteger(v, "n"));
    }

    @Test
    public void testUtilsReadLong() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":3000000000}");
        assertEquals(Long.valueOf(3000000000L), LazyJsonValueUtils.readLong(v, "n"));
    }

    @Test
    public void testUtilsReadBoolean() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"f\":true}");
        assertEquals(Boolean.TRUE, LazyJsonValueUtils.readBoolean(v, "f"));
    }

    @Test
    public void testUtilsReadNestedMap() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"cfg\":{\"k\":\"v\"}}");
        LazyJsonValue cfg = LazyJsonValueUtils.readMapObjectOrNull(v, "cfg");
        assertNotNull(cfg);
        assertEquals("v", LazyJsonValueUtils.readString(cfg, "k"));
    }

    @Test
    public void testUtilsReadStringList() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"tags\":[\"a\",\"b\"]}");
        List<String> tags = LazyJsonValueUtils.readStringListOrNull(v, "tags");
        assertNotNull(tags);
        assertEquals(Arrays.asList("a", "b"), tags);
    }

    @Test
    public void testUtilsReadNanosAsDuration() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"d\":1000000000}");
        assertEquals(Duration.ofSeconds(1), LazyJsonValueUtils.readNanosAsDuration(v, "d"));
    }

    // ---- toJsonValue parity with JsonParser ----

    @Test
    public void testToJsonValueRoundTrip() throws Exception {
        String json = "{\"name\":\"Alice\",\"age\":30,\"scores\":[95,87],\"meta\":{\"role\":\"admin\"}}";
        JsonValue eager = JsonParser.parse(json);
        LazyJsonValue deep = LazyJsonParser.parse(json);
        assertEquals(eager, deep.toJsonValue());
    }

    @Test
    public void testStreamInfoParity() throws Exception {
        String json = ResourceUtils.resourceAsString("stream_info.json");
        // stream_info.json has only integers — default mode works
        JsonValue eager = JsonParser.parse(json);
        LazyJsonValue deep = LazyJsonParser.parse(json);
        assertEquals(eager, deep.toJsonValue());
    }

    @Test
    public void testConsumerInfoParity() throws Exception {
        String json = ResourceUtils.resourceAsString("consumer_info.json");
        JsonValue eager = JsonParser.parse(json);
        LazyJsonValue deep = LazyJsonParser.parse(json);
        assertEquals(eager, deep.toJsonValue());
    }

    // ---- selective reading (the main use case) ----

    @Test
    public void testReadOnlyConfigFromStreamInfo() throws Exception {
        String json = ResourceUtils.resourceAsString("stream_info.json");
        LazyJsonValue v = LazyJsonParser.parse(json);
        // Read just a few config fields — cluster, state, sources etc. are never parsed
        LazyJsonValue config = LazyJsonValueUtils.readMapObjectOrNull(v, "config");
        assertNotNull(config);
        assertEquals("streamName", LazyJsonValueUtils.readString(config, "name"));
        assertEquals(Long.valueOf(100000000000L), LazyJsonValueUtils.readLong(config, "max_age"));
    }

    @Test
    public void testReadOnlyConfigFromConsumerInfo() throws Exception {
        String json = ResourceUtils.resourceAsString("consumer_info.json");
        LazyJsonValue v = LazyJsonParser.parse(json);
        LazyJsonValue config = LazyJsonValueUtils.readMapObjectOrNull(v, "config");
        assertNotNull(config);
        assertEquals("foo-name", LazyJsonValueUtils.readString(config, "name"));
        assertEquals(Long.valueOf(30000000000L), LazyJsonValueUtils.readLong(config, "ack_wait"));
        // cluster, delivered, ack_floor etc. are never parsed
    }

    // ---- entry point variants ----

    @Test
    public void testParseFromBytes() throws Exception {
        byte[] bytes = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);
        LazyJsonValue v = LazyJsonParser.parse(bytes);
        assertEquals(Integer.valueOf(1), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseFromBytesWithOptions() throws Exception {
        byte[] bytes = "{\"x\":null}".getBytes(StandardCharsets.UTF_8);
        LazyJsonValue v = LazyJsonParser.parse(bytes, JsonParser.Option.KEEP_NULLS);
        assertTrue(v.getMap().containsKey("x"));
    }

    @Test
    public void testParseCharArrayWithOptions() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"x\":null}".toCharArray(),
            JsonParser.Option.KEEP_NULLS);
        assertTrue(v.getMap().containsKey("x"));
    }

    @Test
    public void testParseWithStartIndex() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("   {\"x\":1}", 3);
        assertEquals(Integer.valueOf(1), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseCharArrayWithStartIndex() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"x\":1}".toCharArray(), 0);
        assertEquals(Integer.valueOf(1), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseStringWithStartIndex() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("   42", 3);
        assertEquals(Integer.valueOf(42), v.getInteger());
    }

    @Test
    public void testParseUncheckedString() {
        LazyJsonValue v = LazyJsonParser.parseUnchecked("{\"x\":1}");
        assertEquals(Integer.valueOf(1), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseUncheckedCharArray() {
        LazyJsonValue v = LazyJsonParser.parseUnchecked("{\"x\":2}".toCharArray());
        assertEquals(Integer.valueOf(2), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseUncheckedBytes() {
        LazyJsonValue v = LazyJsonParser.parseUnchecked("{\"x\":3}".getBytes(StandardCharsets.UTF_8));
        assertEquals(Integer.valueOf(3), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseUncheckedWithOptions() {
        LazyJsonValue v = LazyJsonParser.parseUnchecked("{\"x\":3.14}",
            JsonParser.Option.DECIMALS);
        assertEquals(new BigDecimal("3.14"), v.getMap().get("x").getBigDecimal());
    }

    @Test
    public void testNullInput() throws Exception {
        assertSame(LazyJsonValue.NULL, LazyJsonParser.parse((char[]) null));
    }

    // ---- error cases ----

    @Test
    public void testInvalidJson() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("{invalid}"));
    }

    @Test
    public void testUnterminatedString() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("\"unterminated"));
    }

    @Test
    public void testLeadingZeroRejected() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("01"));
    }

    @Test
    public void testUnterminatedContainer() {
        // The outer object is unterminated — detected during top-level parse
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("{\"a\":{\"b\":1}"));
        // Unterminated nested container — detected during skip scan
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("{\"a\":{\"b\":1"));
    }

    @Test
    public void testParseUncheckedInvalid() {
        assertThrows(RuntimeException.class, () -> LazyJsonParser.parseUnchecked("{bad}"));
    }

    // -----------------------------------------------------------------------
    // Additional coverage tests
    // -----------------------------------------------------------------------

    // -- Parser entry points and error cases --

    @Test
    public void testParseCharArrayNoArgs() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"x\":1}".toCharArray());
        assertEquals(Integer.valueOf(1), v.getMap().get("x").getInteger());
    }

    @Test
    public void testParseStringWithOptions() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":null}",
            JsonParser.Option.KEEP_NULLS);
        assertTrue(v.getMap().containsKey("a"));
    }

    @Test
    public void testNegativeStartIndex() {
        assertThrows(IllegalArgumentException.class,
            () -> LazyJsonParser.parse("{}", -1));
    }

    @Test
    public void testDirectNestedObject() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("{{}}"));
    }

    @Test
    public void testMissingColon() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("{\"a\" 1}"));
    }

    @Test
    public void testBadTokenAfterValue() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("{\"a\":1 \"b\":2}"));
    }

    @Test
    public void testUnterminatedObject() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("{\"a\":1"));
    }

    @Test
    public void testLeadingZeroNegative() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("-01"));
    }

    @Test
    public void testNewlineInString() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("\"a\nb\""));
    }

    @Test
    public void testCarriageReturnInString() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("\"a\rb\""));
    }

    @Test
    public void testInvalidEscape() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("\"\\x\""));
    }

    @Test
    public void testIncompleteUnicode() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("\"\\u00\""));
    }

    @Test
    public void testInvalidHexInUnicode() {
        assertThrows(JsonParseException.class, () -> LazyJsonParser.parse("\"\\u00GG\""));
    }

    @Test
    public void testInvalidBytes() {
        assertThrows(JsonParseException.class,
            () -> LazyJsonParser.parse(new byte[]{(byte) 0xFF, (byte) 0xFE}));
    }

    // -- Key escapes --

    @Test
    public void testEscapedKeys() throws Exception {
        String json = "{\"k\\n1\":\"v1\",\"k\\t2\":\"v2\",\"k\\\\3\":\"v3\",\"k\\/4\":\"v4\"}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        Map<String, LazyJsonValue> map = v.getMap();
        assertEquals("v1", map.get("k\n1").getString());
        assertEquals("v2", map.get("k\t2").getString());
        assertEquals("v3", map.get("k\\3").getString());
        assertEquals("v4", map.get("k/4").getString());
    }

    @Test
    public void testKeyWithAllEscapes() throws Exception {
        String json = "{\"\\b\\f\\r\\n\\t\\\"\\'\\\\\\/\":\"val\"}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        assertEquals("val", v.getMap().get("\b\f\r\n\t\"'\\/").getString());
    }

    @Test
    public void testUnicodeKey() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"\\u0041B\":\"val\"}");
        assertEquals("val", v.getMap().get("AB").getString());
    }

    @Test
    public void testUnicodeKeyUppercaseHex() throws Exception {
        // \u00AB — uppercase A,B hits the 'A'-'F' branch in parseU()
        LazyJsonValue v = LazyJsonParser.parse("{\"\\u00AB\":\"val\"}");
        assertEquals("val", v.getMap().get("\u00AB").getString());
    }

    @Test
    public void testUnicodeKeyLowercaseHex() throws Exception {
        // \u00ab — lowercase a,b hits the 'a'-'f' branch in parseU()
        LazyJsonValue v = LazyJsonParser.parse("{\"\\u00ab\":\"val\"}");
        assertEquals("val", v.getMap().get("\u00ab").getString());
    }

    // -- String escape extraction --

    @Test
    public void testAllStringEscapes() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"s\":\"\\b\\f\\r\\n\\t\\\\\\/\\\"\\'\"}");
        assertEquals("\b\f\r\n\t\\/\"'", v.getMap().get("s").getString());
    }

    // -- Value accessor edge cases --

    @Test
    public void testGetStringOnNonString() throws Exception {
        assertNull(LazyJsonParser.parse("42").getString());
    }

    @Test
    public void testGetBooleanFalse() throws Exception {
        assertEquals(Boolean.FALSE, LazyJsonParser.parse("false").getBoolean());
    }

    @Test
    public void testGetBooleanOnNonBool() throws Exception {
        assertNull(LazyJsonParser.parse("42").getBoolean());
    }

    @Test
    public void testGetIntDefault() throws Exception {
        assertEquals(99, LazyJsonParser.parse("3000000000").getInt(99));
    }

    @Test
    public void testGetLongDefault() throws Exception {
        assertEquals(42L, LazyJsonParser.parse("\"text\"").getLong(42L));
    }

    @Test
    public void testGetLongFromInt() throws Exception {
        assertEquals(Long.valueOf(5L), LazyJsonParser.parse("5").getLong());
    }

    @Test
    public void testGetDoubleOnNonDouble() throws Exception {
        assertNull(LazyJsonParser.parse("42").getDouble());
    }

    @Test
    public void testGetBigDecimalOnNonDecimal() throws Exception {
        assertNull(LazyJsonParser.parse("42").getBigDecimal());
    }

    @Test
    public void testGetBigIntegerOnNonBigInt() throws Exception {
        assertNull(LazyJsonParser.parse("42").getBigInteger());
    }

    @Test
    public void testGetNumber() throws Exception {
        assertEquals(42, LazyJsonParser.parse("42").getNumber().intValue());
    }

    @Test
    public void testGetNumberOnString() throws Exception {
        assertNull(LazyJsonParser.parse("\"text\"").getNumber());
    }

    @Test
    public void testGetMapOnNonMap() throws Exception {
        assertNull(LazyJsonParser.parse("42").getMap());
    }

    @Test
    public void testGetArrayOnNonArray() throws Exception {
        assertNull(LazyJsonParser.parse("42").getArray());
    }

    // -- Number edge cases --

    @Test
    public void testBigIntegerInIntegersOnly() throws Exception {
        String huge = "99999999999999999999999999999";
        LazyJsonValue v = LazyJsonParser.parse(huge);
        assertEquals(new BigInteger(huge), v.getBigInteger());
    }

    @Test
    public void testNegativeZeroDecimals() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("-0", JsonParser.Option.DECIMALS);
        assertNotNull(v.getDouble());
        assertEquals(-0.0, v.getDouble(), 0.0);
    }

    @Test
    public void testScientificNotation() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("1.5e10", JsonParser.Option.DECIMALS);
        assertNotNull(v.getBigDecimal());
    }

    @Test
    public void testDecimalsBigInteger() throws Exception {
        String huge = "99999999999999999999999999999";
        LazyJsonValue v = LazyJsonParser.parse(huge, JsonParser.Option.DECIMALS);
        assertEquals(new BigInteger(huge), v.getBigInteger());
    }

    @Test
    public void testLongMinMax() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse(
            "{\"max\":" + Long.MAX_VALUE + ",\"min\":" + Long.MIN_VALUE + "}");
        assertEquals(Long.valueOf(Long.MAX_VALUE), v.getMap().get("max").getLong());
        assertEquals(Long.valueOf(Long.MIN_VALUE), v.getMap().get("min").getLong());
    }

    @Test
    public void testIntegersOnlyFallbackForDecimal() throws Exception {
        // In default integers-only mode, a decimal hits non-digit and falls back to full parse
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":3.14}");
        assertEquals(new BigDecimal("3.14"), v.getMap().get("n").getBigDecimal());
    }

    // -- toJsonValue edge cases --

    @Test
    public void testToJsonValueNull() {
        assertEquals(JsonValue.NULL, LazyJsonValue.NULL.toJsonValue());
    }

    @Test
    public void testToJsonValueEmptyMap() {
        assertEquals(JsonValue.EMPTY_MAP, LazyJsonValue.EMPTY_MAP.toJsonValue());
    }

    @Test
    public void testToJsonValueEmptyArray() {
        assertEquals(JsonValue.EMPTY_ARRAY, LazyJsonValue.EMPTY_ARRAY.toJsonValue());
    }

    @Test
    public void testToJsonValueBigInteger() throws Exception {
        String huge = "99999999999999999999999999999";
        JsonValue jv = LazyJsonParser.parse(huge).toJsonValue();
        assertEquals(new BigInteger(huge), jv.bi);
    }

    @Test
    public void testToJsonValueDouble() throws Exception {
        JsonValue jv = LazyJsonParser.parse("-0", JsonParser.Option.DECIMALS).toJsonValue();
        assertEquals(JsonValueType.DOUBLE, jv.type);
    }

    // -- Utils additional coverage --

    @Test
    public void testUtilsReadValueNull() {
        assertNull(LazyJsonValueUtils.readValue(null, "k"));
        assertNull(LazyJsonValueUtils.readValue(LazyJsonValue.NULL, "k"));
    }

    @Test
    public void testUtilsReadStringWithDefault() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":\"x\"}");
        assertEquals("x", LazyJsonValueUtils.readString(v, "a", "d"));
        assertEquals("d", LazyJsonValueUtils.readString(v, "missing", "d"));
    }

    @Test
    public void testUtilsReadIntegerWithDefault() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":5}");
        assertEquals(5, LazyJsonValueUtils.readInteger(v, "n", 0));
        assertEquals(99, LazyJsonValueUtils.readInteger(v, "missing", 99));
    }

    @Test
    public void testUtilsReadLongWithDefault() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":5}");
        assertEquals(5L, LazyJsonValueUtils.readLong(v, "n", 0L));
        assertEquals(99L, LazyJsonValueUtils.readLong(v, "missing", 99L));
    }

    @Test
    public void testUtilsReadBooleanWithDefault() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"f\":false}");
        assertFalse(LazyJsonValueUtils.readBoolean(v, "f", true));
        assertTrue(LazyJsonValueUtils.readBoolean(v, "missing", true));
    }

    @Test
    public void testUtilsReadDate() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"d\":\"2021-01-25T20:09:10.6225191Z\"}");
        assertNotNull(LazyJsonValueUtils.readDate(v, "d"));
        assertNull(LazyJsonValueUtils.readDate(v, "missing"));
    }

    @Test
    public void testUtilsReadNanosWithDefault() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"d\":1000000000}");
        Duration dflt = Duration.ofSeconds(5);
        assertEquals(Duration.ofSeconds(1), LazyJsonValueUtils.readNanosAsDuration(v, "d", dflt));
        assertEquals(dflt, LazyJsonValueUtils.readNanosAsDuration(v, "missing", dflt));
    }

    @Test
    public void testUtilsReadBytes() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"d\":\"hello\"}");
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8),
            LazyJsonValueUtils.readBytes(v, "d"));
        assertNull(LazyJsonValueUtils.readBytes(v, "missing"));
    }

    @Test
    public void testUtilsReadBytesCharset() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"d\":\"hello\"}");
        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII),
            LazyJsonValueUtils.readBytes(v, "d", StandardCharsets.US_ASCII));
        assertNull(LazyJsonValueUtils.readBytes(v, "missing", StandardCharsets.US_ASCII));
    }

    @Test
    public void testUtilsReadBase64() throws Exception {
        String enc = java.util.Base64.getEncoder().encodeToString("test".getBytes());
        LazyJsonValue v = LazyJsonParser.parse("{\"b\":\"" + enc + "\"}");
        assertArrayEquals("test".getBytes(), LazyJsonValueUtils.readBase64Basic(v, "b"));
        assertNull(LazyJsonValueUtils.readBase64Basic(v, "missing"));

        String urlEnc = java.util.Base64.getUrlEncoder().encodeToString("test".getBytes());
        LazyJsonValue v2 = LazyJsonParser.parse("{\"b\":\"" + urlEnc + "\"}");
        assertArrayEquals("test".getBytes(), LazyJsonValueUtils.readBase64Url(v2, "b"));
        assertNull(LazyJsonValueUtils.readBase64Url(v2, "missing"));
    }

    @Test
    public void testUtilsMapOrEmpty() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"m\":{\"k\":\"v\"}}");
        assertNotNull(LazyJsonValueUtils.readMapObjectOrEmpty(v, "m").getMap());
        assertSame(LazyJsonValue.EMPTY_MAP, LazyJsonValueUtils.readMapObjectOrEmpty(v, "missing"));

        assertEquals(1, LazyJsonValueUtils.readMapMapOrEmpty(v, "m").size());
        assertTrue(LazyJsonValueUtils.readMapMapOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsStringMapOrEmpty() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"m\":{\"k\":\"v\",\"n\":42}}");
        Map<String, String> map = LazyJsonValueUtils.readStringMapOrEmpty(v, "m");
        assertEquals(1, map.size()); // only string values
        assertTrue(LazyJsonValueUtils.readStringMapOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsArrayOrEmpty() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[1]}");
        assertEquals(1, LazyJsonValueUtils.readArrayOrEmpty(v, "a").size());
        assertTrue(LazyJsonValueUtils.readArrayOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsStringListIgnoreBlank() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"s\":[\"a\",\" \",\"b\"]}");
        assertEquals(Arrays.asList("a", " ", "b"),
            LazyJsonValueUtils.readStringListOrNull(v, "s", false));
        assertEquals(Arrays.asList("a", "b"),
            LazyJsonValueUtils.readStringListOrNull(v, "s", true));
        assertEquals(Arrays.asList("a", "b"),
            LazyJsonValueUtils.readStringListOrEmpty(v, "s", true));
        assertTrue(LazyJsonValueUtils.readStringListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsIntegerList() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":[1,2,3]}");
        assertEquals(Arrays.asList(1, 2, 3), LazyJsonValueUtils.readIntegerListOrNull(v, "n"));
        assertNull(LazyJsonValueUtils.readIntegerListOrNull(v, "missing"));
        assertEquals(Arrays.asList(1, 2, 3), LazyJsonValueUtils.readIntegerListOrEmpty(v, "n"));
        assertTrue(LazyJsonValueUtils.readIntegerListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsLongList() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":[3000000000,1]}");
        assertEquals(Arrays.asList(3000000000L, 1L), LazyJsonValueUtils.readLongListOrNull(v, "n"));
        assertEquals(Arrays.asList(3000000000L, 1L), LazyJsonValueUtils.readLongListOrEmpty(v, "n"));
        assertTrue(LazyJsonValueUtils.readLongListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsDurationList() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"d\":[1000000000,2000000000]}");
        List<Duration> durations = LazyJsonValueUtils.readNanosAsDurationListOrNull(v, "d");
        assertNotNull(durations);
        assertEquals(Duration.ofSeconds(1), durations.get(0));
        assertNull(LazyJsonValueUtils.readNanosAsDurationListOrNull(v, "missing"));
        assertEquals(2, LazyJsonValueUtils.readNanosAsDurationListOrEmpty(v, "d").size());
        assertTrue(LazyJsonValueUtils.readNanosAsDurationListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsListOfOrNull() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("[\"a\",\"b\"]");
        List<String> list = LazyJsonValueUtils.listOfOrNull(v, LazyJsonValue::getString);
        assertEquals(Arrays.asList("a", "b"), list);
        assertNull(LazyJsonValueUtils.listOfOrNull(null, LazyJsonValue::getString));
    }

    @Test
    public void testUtilsListOfOrEmpty() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("[1,2]");
        List<Integer> list = LazyJsonValueUtils.listOfOrEmpty(v, LazyJsonValue::getInteger);
        assertEquals(Arrays.asList(1, 2), list);
        assertTrue(LazyJsonValueUtils.listOfOrEmpty(null, LazyJsonValue::getInteger).isEmpty());
    }

    @Test
    public void testUtilsConvertToList() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("[1,\"skip\",3]");
        List<Integer> ints = LazyJsonValueUtils.convertToList(v.getArray(), LazyJsonValue::getInteger);
        assertEquals(Arrays.asList(1, 3), ints);
    }

    // -- Trailing comma in array --

    @Test
    public void testTrailingCommaInArray() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("[1,2,]");
        assertEquals(2, v.getArray().size());
    }

    // -----------------------------------------------------------------------
    // Branch coverage: legitimate data paths
    // -----------------------------------------------------------------------

    @Test
    public void testGetIntWithValidValue() throws Exception {
        assertEquals(42, LazyJsonParser.parse("42").getInt(99));
    }

    @Test
    public void testGetLongDefaultWithValidValue() throws Exception {
        assertEquals(3000000000L, LazyJsonParser.parse("3000000000").getLong(0L));
    }

    @Test
    public void testGetIntegerFromLongInRange() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("100");
        assertNotNull(v.getLong());
        assertEquals(Integer.valueOf(100), v.getInteger());
    }

    @Test
    public void testScientificNotationUpperE() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("1E5", JsonParser.Option.DECIMALS);
        assertNotNull(v.getBigDecimal());
    }

    @Test
    public void testScientificNotationLowerE() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("3.14e-2", JsonParser.Option.DECIMALS);
        assertNotNull(v.getBigDecimal());
    }

    @Test
    public void testNegativeZeroDecimalNotation() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("-0.0", JsonParser.Option.DECIMALS);
        assertNotNull(v.getDouble());
    }

    @Test
    public void testUnicodeEscapeUppercaseHex() throws Exception {
        assertEquals("\u00AB", LazyJsonParser.parse("\"\\u00AB\"").getString());
    }

    @Test
    public void testUnicodeEscapeLowercaseHex() throws Exception {
        assertEquals("\u00ab", LazyJsonParser.parse("\"\\u00ab\"").getString());
    }

    @Test
    public void testUnicodeEscapeDigitsOnly() throws Exception {
        assertEquals("9", LazyJsonParser.parse("\"\\u0039\"").getString());
    }

    @Test
    public void testParseNullCharArrayWithOptions() throws Exception {
        assertSame(LazyJsonValue.NULL, LazyJsonParser.parse((char[]) null, JsonParser.Option.DECIMALS));
    }

    @Test
    public void testParseStringWithDecimalsAndKeepNulls() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":null,\"b\":1.5}",
            JsonParser.Option.KEEP_NULLS, JsonParser.Option.DECIMALS);
        assertTrue(v.getMap().containsKey("a"));
        assertNotNull(v.getMap().get("b").getBigDecimal());
    }

    @Test
    public void testNestedArrayResolvedLazily() throws Exception {
        // Exercises the ARRAY branch in resolve()
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":{\"inner\":[1,2,3]}}");
        LazyJsonValue inner = v.getMap().get("a").getMap().get("inner");
        assertEquals(JsonValueType.ARRAY, inner.type);
        assertEquals(3, inner.getArray().size());
    }

    @Test
    public void testUtilsReadDateValid() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"d\":\"2021-01-25T20:09:10.6225191Z\"}");
        assertNotNull(LazyJsonValueUtils.readDate(v, "d"));
    }

    @Test
    public void testUtilsReadMapObjectOrEmptyWithData() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"m\":{\"k\":\"v\"}}");
        LazyJsonValue m = LazyJsonValueUtils.readMapObjectOrEmpty(v, "m");
        assertNotNull(m.getMap());
        assertEquals(1, m.getMap().size());
    }

    @Test
    public void testUtilsReadWithRequiredTypeMismatch() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"s\":\"hello\"}");
        assertNull(LazyJsonValueUtils.readInteger(v, "s"));
    }

    @Test
    public void testUtilsReadStringListEmptyArray() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[]}");
        assertNull(LazyJsonValueUtils.readStringListOrNull(v, "a"));
        assertTrue(LazyJsonValueUtils.readStringListOrEmpty(v, "a").isEmpty());
    }

    @Test
    public void testUtilsReadIntegerListEmpty() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[]}");
        assertNull(LazyJsonValueUtils.readIntegerListOrNull(v, "a"));
    }

    @Test
    public void testUtilsReadLongListEmpty() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[]}");
        assertNull(LazyJsonValueUtils.readLongListOrNull(v, "a"));
    }

    @Test
    public void testUtilsConvertToStringListWithStrings() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("[\"a\",\"b\"]");
        List<String> list = LazyJsonValueUtils.convertToStringList(v.getArray(), false);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testToJsonValueBigDecimalViaDecimals() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("3.14", JsonParser.Option.DECIMALS);
        assertEquals(JsonValueType.BIG_DECIMAL, v.toJsonValue().type);
    }

    @Test
    public void testSkippedArrayWithNestedObjects() throws Exception {
        String json = "{\"items\":[{\"a\":1},{\"b\":2},{\"c\":3}],\"x\":42}";
        LazyJsonValue v = LazyJsonParser.parse(json);
        assertEquals(Integer.valueOf(42), v.getMap().get("x").getInteger());
        List<LazyJsonValue> items = v.getMap().get("items").getArray();
        assertEquals(3, items.size());
        assertEquals(Integer.valueOf(2), items.get(1).getMap().get("b").getInteger());
    }

    // -----------------------------------------------------------------------
    // Utils: type mismatch, mixed-type arrays, non-MAP inputs
    // -----------------------------------------------------------------------

    @Test
    public void testUtilsReadOnNonMapType() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("\"just a string\"");
        assertNull(LazyJsonValueUtils.readValue(v, "key"));
        assertNull(LazyJsonValueUtils.readString(v, "key"));
        assertEquals("dflt", LazyJsonValueUtils.readString(v, "key", "dflt"));
        assertNull(LazyJsonValueUtils.readInteger(v, "key"));
        assertNull(LazyJsonValueUtils.readLong(v, "key"));
        assertNull(LazyJsonValueUtils.readBoolean(v, "key"));
        assertEquals(99, LazyJsonValueUtils.readInteger(v, "key", 99));
        assertEquals(99L, LazyJsonValueUtils.readLong(v, "key", 99L));
        assertTrue(LazyJsonValueUtils.readBoolean(v, "key", true));
    }

    @Test
    public void testUtilsReadStringOnIntegerValue() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":42}");
        assertEquals("dflt", LazyJsonValueUtils.readString(v, "n", "dflt"));
    }

    @Test
    public void testUtilsConvertMixedArrayToIntegers() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[1,\"skip\",3]}");
        List<Integer> ints = LazyJsonValueUtils.readIntegerListOrNull(v, "a");
        assertNotNull(ints);
        assertEquals(Arrays.asList(1, 3), ints);
    }

    @Test
    public void testUtilsConvertMixedArrayToLongs() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[1,\"skip\",3]}");
        List<Long> longs = LazyJsonValueUtils.readLongListOrNull(v, "a");
        assertNotNull(longs);
        assertEquals(Arrays.asList(1L, 3L), longs);
    }

    @Test
    public void testUtilsConvertMixedArrayToStrings() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[\"a\",42,\"b\"]}");
        List<String> strs = LazyJsonValueUtils.readStringListOrNull(v, "a");
        assertNotNull(strs);
        assertEquals(Arrays.asList("a", "b"), strs);
    }

    @Test
    public void testUtilsListOfOrNullOnNonArray() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("42");
        assertNull(LazyJsonValueUtils.listOfOrNull(v, LazyJsonValue::getString));
        assertTrue(LazyJsonValueUtils.listOfOrEmpty(v, LazyJsonValue::getString).isEmpty());
    }

    @Test
    public void testUtilsListOfOrNullEmptyArray() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("[]");
        assertNull(LazyJsonValueUtils.listOfOrNull(v, LazyJsonValue::getString));
        assertTrue(LazyJsonValueUtils.listOfOrEmpty(v, LazyJsonValue::getString).isEmpty());
    }

    @Test
    public void testUtilsReadNanosAsDurationList() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"d\":[1000000000,2000000000]}");
        List<Duration> durations = LazyJsonValueUtils.readNanosAsDurationListOrNull(v, "d");
        assertNotNull(durations);
        assertEquals(2, durations.size());
        assertNull(LazyJsonValueUtils.readNanosAsDurationListOrNull(v, "missing"));
        assertTrue(LazyJsonValueUtils.readNanosAsDurationListOrEmpty(v, "missing").isEmpty());
    }

    @Test
    public void testUtilsReadDateMissing() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"x\":1}");
        assertNull(LazyJsonValueUtils.readDate(v, "missing"));
    }

    @Test
    public void testUtilsReadBytesMissing() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"x\":1}");
        assertNull(LazyJsonValueUtils.readBytes(v, "missing"));
    }

    @Test
    public void testUtilsReadStringMapOrNullMissing() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"x\":1}");
        assertNull(LazyJsonValueUtils.readStringMapOrNull(v, "missing"));
    }

    // -----------------------------------------------------------------------
    // Value: number boundary conversions
    // -----------------------------------------------------------------------

    @Test
    public void testGetIntegerFromLongOutOfRange() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse(String.valueOf(Long.MAX_VALUE));
        assertNotNull(v.getLong());
        assertNull(v.getInteger());
        LazyJsonValue v2 = LazyJsonParser.parse(String.valueOf(Long.MIN_VALUE));
        assertNotNull(v2.getLong());
        assertNull(v2.getInteger());
    }

    @Test
    public void testHexFloatViaDecimals() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("0x1.0P-1074", JsonParser.Option.DECIMALS);
        assertNotNull(v.getDouble());
        assertEquals(JsonValueType.DOUBLE, v.toJsonValue().type);
    }

    @Test
    public void testToJsonValueLong() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("3000000000");
        JsonValue jv = v.toJsonValue();
        assertEquals(JsonValueType.LONG, jv.type);
        assertEquals(Long.valueOf(3000000000L), jv.l);
    }

    // -----------------------------------------------------------------------
    // Utils: null jv, found-value branches, type-checked reads
    // -----------------------------------------------------------------------

    @Test
    public void testUtilsReadValueWithNullJv() {
        assertNull(LazyJsonValueUtils.readValue(null, "key"));
        assertNull(LazyJsonValueUtils.readString(null, "key"));
        assertEquals("dflt", LazyJsonValueUtils.readString(null, "key", "dflt"));
        assertNull(LazyJsonValueUtils.readInteger(null, "key"));
        assertEquals(0, LazyJsonValueUtils.readInteger(null, "key", 0));
        assertNull(LazyJsonValueUtils.readLong(null, "key"));
        assertEquals(0L, LazyJsonValueUtils.readLong(null, "key", 0L));
        assertNull(LazyJsonValueUtils.readBoolean(null, "key"));
        assertFalse(LazyJsonValueUtils.readBoolean(null, "key", false));
        assertNull(LazyJsonValueUtils.readDate(null, "key"));
        assertNull(LazyJsonValueUtils.readNanosAsDuration(null, "key"));
        assertNull(LazyJsonValueUtils.readNanosAsDuration(null, "key", null));
        assertNull(LazyJsonValueUtils.readBytes(null, "key"));
        assertNull(LazyJsonValueUtils.readMapObjectOrNull(null, "key"));
        assertNull(LazyJsonValueUtils.readMapMapOrNull(null, "key"));
        assertNull(LazyJsonValueUtils.readArrayOrNull(null, "key"));
        assertNull(LazyJsonValueUtils.readStringListOrNull(null, "key"));
        assertNull(LazyJsonValueUtils.readIntegerListOrNull(null, "key"));
        assertNull(LazyJsonValueUtils.readLongListOrNull(null, "key"));
    }

    @Test
    public void testUtilsFoundValueBranches() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse(
            "{\"i\":42,\"l\":3000000000,\"b\":true,\"s\":\"hello\",\"d\":1000000000}");
        assertEquals(42, LazyJsonValueUtils.readInteger(v, "i", 0));
        assertEquals(3000000000L, LazyJsonValueUtils.readLong(v, "l", 0L));
        assertTrue(LazyJsonValueUtils.readBoolean(v, "b", false));
        assertNotNull(LazyJsonValueUtils.readNanosAsDuration(v, "d"));
        assertEquals(Duration.ofSeconds(1),
            LazyJsonValueUtils.readNanosAsDuration(v, "d", Duration.ZERO));
    }

    @Test
    public void testUtilsTypedReadKeyExistsWrongType() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"n\":42,\"s\":\"hello\"}");
        assertEquals("dflt", LazyJsonValueUtils.readString(v, "n", "dflt"));
        assertNull(LazyJsonValueUtils.readMapObjectOrNull(v, "s"));
        assertNull(LazyJsonValueUtils.readArrayOrNull(v, "n"));
    }

    @Test
    public void testUtilsStringListOrEmptyMissing() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"x\":1}");
        assertTrue(LazyJsonValueUtils.readStringListOrEmpty(v, "missing").isEmpty());
        assertTrue(LazyJsonValueUtils.readStringListOrEmpty(v, "missing", true).isEmpty());
    }

    @Test
    public void testUtilsIntegerListOrNullEmpty() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[\"not_int\"]}");
        assertNull(LazyJsonValueUtils.readIntegerListOrNull(v, "a"));
    }

    @Test
    public void testUtilsLongListOrNullEmpty() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"a\":[\"not_long\"]}");
        assertNull(LazyJsonValueUtils.readLongListOrNull(v, "a"));
    }

    @Test
    public void testUtilsReadStringMapOrNullOnNonMap() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"s\":\"hello\"}");
        assertNull(LazyJsonValueUtils.readStringMapOrNull(v, "s"));
    }

    @Test
    public void testUtilsReadValueSuccess() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"k\":\"v\"}");
        LazyJsonValue result = LazyJsonValueUtils.readValue(v, "k");
        assertNotNull(result);
        assertEquals("v", result.getString());
    }

    @Test
    public void testUtilsTypedReadSuccess() throws Exception {
        LazyJsonValue v = LazyJsonParser.parse("{\"s\":\"hello\",\"m\":{\"a\":1}}");
        assertEquals("hello", LazyJsonValueUtils.readString(v, "s", "dflt"));
        assertNotNull(LazyJsonValueUtils.readMapObjectOrNull(v, "m"));
    }

    @Test
    public void testUtilsReadStringMapOrNullSuccess() throws Exception {
        // Exercises the map != null path of readStringMapOrNull (line 252)
        LazyJsonValue v = LazyJsonParser.parse("{\"m\":{\"k\":\"v\"}}");
        Map<String, String> map = LazyJsonValueUtils.readStringMapOrNull(v, "m");
        assertNotNull(map);
        assertEquals("v", map.get("k"));
    }
}
