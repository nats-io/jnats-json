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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nats.json.Encoding.jsonEncode;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional parity tests. For every scenario tested in {@link JsonParsingTests},
 * parse the same JSON with all three parsers and verify that converting to
 * {@link JsonValue} via {@code toJsonValue()} produces identical results.
 * <p>
 * This ensures the indexed and deep parsers behave identically to the
 * original eager parser for all value types, escapes, numbers, nesting,
 * errors, and edge cases.
 */
public final class ParserParityTests {

    static final List<String> UTF_STRINGS = ResourceUtils.resourceAsLines("utf8-only-no-ws-test-strings.txt");

    // -----------------------------------------------------------------------
    // Helpers: parse with all three and assert toJsonValue() equality
    // -----------------------------------------------------------------------

    /**
     * Parse json with all three parsers (DECIMALS mode for indexed/deep)
     * and assert the indexed and deep results match the eager result.
     */
    private void assertParity(String json) throws Exception {
        JsonValue eager = JsonParser.parse(json, JsonParser.Option.DECIMALS);
        IndexedJsonValue indexed = IndexedJsonParser.parse(json, JsonParser.Option.DECIMALS);
        LazyJsonValue deep = LazyJsonParser.parse(json, JsonParser.Option.DECIMALS);

        assertEquals(eager, indexed.toJsonValue(),
            "Indexed mismatch for: " + truncate(json));
        assertEquals(eager, deep.toJsonValue(),
            "Deep mismatch for: " + truncate(json));
    }

    /**
     * Same but integers-only mode (default for indexed/deep).
     */
    private void assertParityIntOnly(String json) throws Exception {
        JsonValue eager = JsonParser.parse(json);
        IndexedJsonValue indexed = IndexedJsonParser.parse(json);
        LazyJsonValue deep = LazyJsonParser.parse(json);

        assertEquals(eager, indexed.toJsonValue(),
            "Indexed mismatch for: " + truncate(json));
        assertEquals(eager, deep.toJsonValue(),
            "Deep mismatch for: " + truncate(json));
    }

    /** Assert all three parsers throw on the given JSON (at parse or access time). */
    private void assertAllThrow(String json) {
        assertThrows(JsonParseException.class, () -> JsonParser.parse(json));
        assertThrows(Exception.class, () -> {
            IndexedJsonValue v = IndexedJsonParser.parse(json);
            v.toJsonValue(); // force full materialization to catch deferred errors
        });
        assertThrows(Exception.class, () -> {
            LazyJsonValue v = LazyJsonParser.parse(json);
            v.toJsonValue(); // force full materialization to catch deferred errors
        });
    }

    private static String truncate(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }

    // -----------------------------------------------------------------------
    // String parsing parity (mirrors testStringParsing)
    // -----------------------------------------------------------------------

    @Test
    public void testStringParsingParity() throws Exception {
        List<String> testStrings = new ArrayList<>();
        testStrings.add("b4\\after");
        testStrings.add("b4/after");
        testStrings.add("b4\"after");
        testStrings.add("b4\tafter");
        testStrings.add("b4\\bafter");
        testStrings.add("b4\\fafter");
        testStrings.add("b4\\nafter");
        testStrings.add("b4\\rafter");
        testStrings.add("b4\\tafter");
        testStrings.add("b4" + (char) 0 + "after");
        testStrings.add("b4" + (char) 1 + "after");
        testStrings.add("plain");
        testStrings.add("has space");
        testStrings.add("has-print!able");
        testStrings.add("has.dot");
        testStrings.add("star*not*segment");
        testStrings.add("gt>not>segment");
        testStrings.add("has-dash");
        testStrings.add("has_under");
        testStrings.add("has$dollar");
        testStrings.add("has" + (char) 0 + "low");
        testStrings.add("has" + (char) 127 + "127");
        testStrings.add("has/fwd/slash");
        testStrings.add("has\\back\\slash");
        testStrings.add("has=equals");
        testStrings.add("has`tic");

        for (String u : UTF_STRINGS) {
            testStrings.add("b4\b\f\n\r\t" + u + "after");
        }

        for (String decoded : testStrings) {
            String json = "\"" + jsonEncode(decoded) + "\"";
            assertParity(json);
        }
    }

    @Test
    public void testStringEscapesParity() throws Exception {
        String s = "foo \b \t \n \f \r \" \\ /";
        String json = "\"" + jsonEncode(s) + "\"";
        assertParity(json);
    }

    // -----------------------------------------------------------------------
    // Primitives parity (mirrors testJsonValuePrimitives)
    // -----------------------------------------------------------------------

    @Test
    public void testPrimitivesInMapParity() throws Exception {
        Map<String, JsonValue> oMap = new HashMap<>();
        oMap.put("trueKey1", new JsonValue(true));
        oMap.put("falseKey1", new JsonValue(false));
        oMap.put("stringKey", new JsonValue("hello world!"));
        oMap.put("escapeStringKey", new JsonValue("h\be\tllo w\u1234orld!"));
        oMap.put("intKey1", new JsonValue(Integer.MAX_VALUE));
        oMap.put("intKey2", new JsonValue(Integer.MIN_VALUE));
        oMap.put("longKey1", new JsonValue(Long.MAX_VALUE));
        oMap.put("longKey2", new JsonValue(Long.MIN_VALUE));
        oMap.put("bigDecimalKey1", new JsonValue(new BigDecimal("9223372036854775807.123")));
        oMap.put("bigIntegerKey1", new JsonValue(new BigInteger("9223372036854775807")));

        String json = new JsonValue(oMap).toJson();
        assertParity(json);
    }

    @Test
    public void testKeepNullsParity() throws Exception {
        Map<String, JsonValue> oMap = new HashMap<>();
        oMap.put("a", new JsonValue(1));
        oMap.put("nullKey", JsonValue.NULL);
        oMap.put("b", new JsonValue("hello"));
        String json = new JsonValue(oMap).toJson();

        // Without keep nulls
        JsonValue eager = JsonParser.parse(json);
        IndexedJsonValue indexed = IndexedJsonParser.parse(json, JsonParser.Option.DECIMALS);
        LazyJsonValue deep = LazyJsonParser.parse(json, JsonParser.Option.DECIMALS);
        assertEquals(eager, indexed.toJsonValue());
        assertEquals(eager, deep.toJsonValue());
        assertFalse(eager.map.containsKey("nullKey"));

        // With keep nulls
        eager = JsonParser.parse(json, JsonParser.Option.KEEP_NULLS);
        indexed = IndexedJsonParser.parse(json.toCharArray(), JsonParser.Option.KEEP_NULLS);
        deep = LazyJsonParser.parse(json, JsonParser.Option.KEEP_NULLS);
        assertEquals(eager, indexed.toJsonValue());
        assertEquals(eager, deep.toJsonValue());
        assertTrue(eager.map.containsKey("nullKey"));
    }

    // -----------------------------------------------------------------------
    // Number parsing parity (mirrors testNumberParsing)
    // -----------------------------------------------------------------------

    @Test
    public void testNumberTypesParity() throws Exception {
        // Integer
        assertParity("1");
        assertParity(Integer.toString(Integer.MAX_VALUE));
        assertParity(Integer.toString(Integer.MIN_VALUE));

        // Long
        assertParity(Long.toString((long) Integer.MAX_VALUE + 1));
        assertParity(Long.toString((long) Integer.MIN_VALUE - 1));

        // Double (-0)
        assertParity("-0");
        assertParity("-0.0");

        // BigDecimal
        assertParity("0.2");
        assertParity("244273.456789012345");
        assertParity("0.1234567890123456789");
        assertParity("-24.42e7345");
        assertParity("-24.42E7345");
        assertParity("-.01");
        assertParity("00.001");

        // BigInteger
        assertParity("12345678901234567890");
        String bigInt = new BigInteger(Long.toString(Long.MAX_VALUE)).add(BigInteger.ONE).toString();
        assertParity(bigInt);
    }

    @Test
    public void testNumberErrorsParity() {
        // Leading zero errors — caught at parse time by all parsers
        assertAllThrow("00");
        assertAllThrow("01");
        assertAllThrow("-01");

        // Bare "-" — the eager parser catches this at parse time.
        // The indexed/deep parsers accept it structurally (starts with '-',
        // looks like a number token) but ensureNumber() fails silently,
        // producing null. This is a known trade-off of deferred validation.
        assertThrows(JsonParseException.class, () -> JsonParser.parse("-"));
        // Indexed/deep: parse succeeds but the value is not a usable number
        IndexedJsonValue iv = IndexedJsonParser.parseUnchecked("-");
        assertNull(iv.getInteger());
        assertNull(iv.getLong());
        LazyJsonValue dv = LazyJsonParser.parseUnchecked("-");
        assertNull(dv.getInteger());
        assertNull(dv.getLong());
    }

    // -----------------------------------------------------------------------
    // Array parity (mirrors testArray)
    // -----------------------------------------------------------------------

    @Test
    public void testArrayOfPrimitivesParity() throws Exception {
        List<JsonValue> list = new ArrayList<>();
        list.add(new JsonValue("string"));
        list.add(new JsonValue(true));
        list.add(JsonValue.NULL);
        list.add(JsonValue.EMPTY_MAP);
        list.add(JsonValue.EMPTY_ARRAY);
        assertParity(new JsonValue(list).toJson());
    }

    @Test
    public void testArrayOfNumbersParity() throws Exception {
        List<JsonValue> list = new ArrayList<>();
        list.add(new JsonValue(1));
        list.add(new JsonValue(Long.MAX_VALUE));
        list.add(new JsonValue(Double.MAX_VALUE));
        list.add(new JsonValue(Float.MAX_VALUE));
        list.add(new JsonValue(new BigDecimal(Double.toString(Double.MAX_VALUE))));
        list.add(new JsonValue(new BigInteger(Long.toString(Long.MAX_VALUE))));
        assertParity(new JsonValue(list).toJson());
    }

    // -----------------------------------------------------------------------
    // Parse error parity (mirrors validateThrows in testParsingCoverage)
    // -----------------------------------------------------------------------

    @Test
    public void testStructuralErrorsParity() {
        // These are structural errors detected by all parsers at parse time
        assertAllThrow("{");
        assertAllThrow("{{");
        assertAllThrow("{[");
        assertAllThrow("{\"foo\":1 ]");
        assertAllThrow("{\"foo\" 1");
        assertAllThrow("\"u");
        assertAllThrow("\"u\r");
        assertAllThrow("\"u\n");
        assertAllThrow("\"\\x\"");
        assertAllThrow("\"\\u000");
        assertAllThrow("\"\\uzzzz");
    }

    @Test
    public void testPrimitiveErrorsParity() {
        // "t" and "f" are caught by all parsers — they don't match "true"/"false"
        // and don't start with a digit or '-', so nextPrimitiveValue throws.
        assertAllThrow("t");
        assertAllThrow("f");

        // "[1Z]" — the eager parser catches "1Z" as an invalid number at parse
        // time. The indexed/deep parsers accept "1Z" structurally (starts with a
        // digit) but the lazy number parse fails silently — getInteger() returns
        // null. This is a known trade-off of deferred validation: malformed
        // numbers that pass structural checks don't throw, they produce null.
        assertThrows(JsonParseException.class, () -> JsonParser.parse("[1Z]"));
        IndexedJsonValue iv = IndexedJsonParser.parseUnchecked("[1Z]");
        assertNull(iv.getArray().get(0).getInteger());
        LazyJsonValue dv = LazyJsonParser.parseUnchecked("[1Z]");
        assertNull(dv.getArray().get(0).getInteger());
    }

    // -----------------------------------------------------------------------
    // Edge cases parity (mirrors testParsingCoverage)
    // -----------------------------------------------------------------------

    @Test
    public void testNullInputParity() throws Exception {
        assertEquals(JsonValue.NULL, JsonParser.parse((char[]) null));
        assertEquals(JsonValue.NULL, IndexedJsonParser.parse((char[]) null).toJsonValue());
        assertEquals(JsonValue.NULL, LazyJsonParser.parse((char[]) null).toJsonValue());
    }

    @Test
    public void testEmptyParity() throws Exception {
        assertParity("{}");
        assertParity("[]");
    }

    @Test
    public void testDanglingCommaParity() throws Exception {
        assertParityIntOnly("{\"foo\":1,}");
        assertParityIntOnly("[\"foo\",]");
    }

    @Test
    public void testStartIndexParity() throws Exception {
        String json = "INFO{\"foo\":1,}";
        JsonValue eager = JsonParser.parse(json, 4);
        IndexedJsonValue indexed = IndexedJsonParser.parse(json, 4);
        LazyJsonValue deep = LazyJsonParser.parse(json, 4);
        assertEquals(eager, indexed.toJsonValue());
        assertEquals(eager, deep.toJsonValue());
    }

    // -----------------------------------------------------------------------
    // Nested structures parity
    // -----------------------------------------------------------------------

    @Test
    public void testNestedObjectParity() throws Exception {
        assertParityIntOnly("{\"a\":{\"b\":{\"c\":1}},\"d\":2}");
    }

    @Test
    public void testNestedArrayParity() throws Exception {
        assertParityIntOnly("[[1,2],[3,[4,5]]]");
    }

    @Test
    public void testMixedNestingParity() throws Exception {
        assertParityIntOnly("{\"a\":[{\"b\":1},{\"c\":[2,3]}],\"d\":{\"e\":[4]}}");
    }

    // -----------------------------------------------------------------------
    // Real-world JSON resource parity
    // -----------------------------------------------------------------------

    @Test
    public void testStreamInfoResourceParity() throws Exception {
        String json = ResourceUtils.resourceAsString("stream_info.json");
        assertParity(json);
    }

    @Test
    public void testTestJsonResourceParity() throws Exception {
        String json = ResourceUtils.resourceAsString("test.json");
        assertParity(json);
    }

    @Test
    public void testStreamInfoV3Parity() throws Exception {
        String json = ResourceUtils.resourceAsString("stream_info.json");
        assertParityIntOnly(json);
    }

    @Test
    public void testConsumerInfoV3Parity() throws Exception {
        String json = ResourceUtils.resourceAsString("consumer_info.json");
        assertParityIntOnly(json);
    }

    // -----------------------------------------------------------------------
    // UTF-8 strings parity
    // -----------------------------------------------------------------------

    @Test
    public void testUtf8StringsParity() throws Exception {
        for (String u : UTF_STRINGS) {
            String json = "{\"v\":\"" + jsonEncode(u) + "\"}";
            assertParity(json);
        }
    }

    // -----------------------------------------------------------------------
    // Strings with braces/brackets (deep parser skip-scan edge cases)
    // -----------------------------------------------------------------------

    @Test
    public void testStringWithBracesInNestedObject() throws Exception {
        assertParityIntOnly("{\"a\":{\"msg\":\"contains { and } and [ and ]\"}}");
    }

    @Test
    public void testStringWithEscapedQuotesInNestedObject() throws Exception {
        assertParityIntOnly("{\"a\":{\"msg\":\"escaped \\\" quote\"}}");
    }

    @Test
    public void testStringWithBackslashQuoteInNestedObject() throws Exception {
        // \\\" in JSON = backslash followed by end-of-string
        assertParityIntOnly("{\"a\":{\"msg\":\"ends \\\\\"}}");
    }

    // -----------------------------------------------------------------------
    // Complex real-world-like structures
    // -----------------------------------------------------------------------

    @Test
    public void testComplexStructureParity() throws Exception {
        // Build a complex JSON that exercises many features
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"test-stream\",");
        sb.append("\"subjects\":[\"sub.>\",\"other.*\"],");
        sb.append("\"retention\":\"limits\",");
        sb.append("\"max_consumers\":100,");
        sb.append("\"max_msgs\":1000000,");
        sb.append("\"max_bytes\":1073741824,");
        sb.append("\"max_age\":86400000000000,");
        sb.append("\"storage\":\"file\",");
        sb.append("\"num_replicas\":3,");
        sb.append("\"placement\":{\"cluster\":\"us-east\",\"tags\":[\"fast\",\"ssd\"]},");
        sb.append("\"mirror\":{\"name\":\"backup\",\"external\":{\"api\":\"api.prefix\",\"deliver\":\"dlvr.prefix\"}},");
        sb.append("\"sources\":[");
        sb.append("{\"name\":\"src1\",\"external\":{\"api\":\"a1\",\"deliver\":\"d1\"}},");
        sb.append("{\"name\":\"src2\",\"external\":{\"api\":\"a2\",\"deliver\":\"d2\"}}");
        sb.append("],");
        sb.append("\"metadata\":{\"env\":\"prod\",\"team\":\"platform\"},");
        sb.append("\"state\":{\"messages\":42,\"bytes\":1024,\"first_seq\":1,\"last_seq\":42,");
        sb.append("\"deleted\":[5,10,15],\"subjects\":{\"sub.a\":10,\"sub.b\":32}},");
        sb.append("\"cluster\":{\"name\":\"nats-cluster\",\"leader\":\"node-1\",");
        sb.append("\"replicas\":[{\"name\":\"node-2\",\"current\":true,\"lag\":0},{\"name\":\"node-3\",\"current\":true,\"lag\":1}]}");
        sb.append("}");

        assertParityIntOnly(sb.toString());
    }
}
