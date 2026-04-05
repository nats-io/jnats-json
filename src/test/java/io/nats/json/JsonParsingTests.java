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

import io.ResourceUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static io.nats.json.Encoding.jsonEncode;
import static io.nats.json.JsonParseException.INVALID_VALUE;
import static io.nats.json.JsonParser.Option.DECIMALS;
import static io.nats.json.JsonParser.Option.KEEP_NULLS;
import static io.nats.json.JsonParser.parse;
import static io.nats.json.JsonParser.parseUnchecked;
import static io.nats.json.JsonValue.instance;
import static io.nats.json.JsonWriteUtils.printFormatted;
import static io.nats.json.JsonWriteUtils.toKey;
import static org.junit.jupiter.api.Assertions.*;

public final class JsonParsingTests {
    static List<String> UTF_STRINGS = ResourceUtils.resourceAsLines("utf8-only-no-ws-test-strings.txt");

    private String key(int i) {
        return "key" + i;
    }

    @Test
    public void testStringParsing() {
        List<String> encodeds = new ArrayList<>();
        List<String> decodeds = new ArrayList<>();
        Map<String, JsonValue> oMap = new HashMap<>();
        List<JsonValue> list = new ArrayList<>();

        int x = 0;
        addField(key(x++), "b4\\after", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4/after", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4\"after", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4\tafter", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4\\bafter", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4\\fafter", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4\\nafter", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4\\rafter", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4\\tafter", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4" + (char) 0 + "after", oMap, list, encodeds, decodeds);
        addField(key(x++), "b4" + (char) 1 + "after", oMap, list, encodeds, decodeds);

        for (String u : UTF_STRINGS) {
            String uu = "b4\b\f\n\r\t" + u + "after";
            addField(key(x++), uu, oMap, list, encodeds, decodeds);
        }

        addField(key(x++), "plain", oMap, list, encodeds, decodeds);
        addField(key(x++), "has space", oMap, list, encodeds, decodeds);
        addField(key(x++), "has-print!able", oMap, list, encodeds, decodeds);
        addField(key(x++), "has.dot", oMap, list, encodeds, decodeds);
        addField(key(x++), "star*not*segment", oMap, list, encodeds, decodeds);
        addField(key(x++), "gt>not>segment", oMap, list, encodeds, decodeds);
        addField(key(x++), "has-dash", oMap, list, encodeds, decodeds);
        addField(key(x++), "has_under", oMap, list, encodeds, decodeds);
        addField(key(x++), "has$dollar", oMap, list, encodeds, decodeds);
        addField(key(x++), "has" + (char)0 + "low", oMap, list, encodeds, decodeds);
        addField(key(x++), "has" + (char)127 + "127", oMap, list, encodeds, decodeds);
        addField(key(x++), "has/fwd/slash", oMap, list, encodeds, decodeds);
        addField(key(x++), "has\\back\\slash", oMap, list, encodeds, decodeds);
        addField(key(x++), "has=equals", oMap, list, encodeds, decodeds);
        addField(key(x), "has`tic", oMap, list, encodeds, decodeds);

        for (int i = 0; i < list.size(); i++) {
            JsonValue v = list.get(i);
            assertEquals(v, v.toJsonValue());
            assertEquals(decodeds.get(i), v.string);
            assertEquals(v.toJson(), "\"" + encodeds.get(i) + "\"");
            assertEquals(v.toString(), "\"" + encodeds.get(i) + "\"");
        }
    }

    private void addField(String name, String decoded,
                          Map<String, JsonValue> map, List<JsonValue> list,
                          List<String> encodeds, List<String> decodeds) {
        String enc = jsonEncode(decoded);
        encodeds.add(enc);
        decodeds.add(decoded);
        JsonValue jv = new JsonValue(decoded);
        map.put(name, jv);
        list.add(jv);
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @Test
    public void testJsonValuePrimitives() throws JsonParseException {
        Map<String, JsonValue> oMap = new HashMap<>();
        oMap.put("trueKey1", new JsonValue(true));
        oMap.put("trueKey2", new JsonValue(Boolean.TRUE));
        oMap.put("falseKey1", new JsonValue(false));
        oMap.put("falseKey2", new JsonValue(Boolean.FALSE));
        oMap.put("stringKey", new JsonValue("hello world!"));
        oMap.put("charKey", new JsonValue('x'));
        oMap.put("escapeStringKey", new JsonValue("h\be\tllo w\u1234orld!"));
        oMap.put("nullKey", JsonValue.NULL);
        oMap.put("intKey1", new JsonValue(Integer.MAX_VALUE));
        oMap.put("intKey2", new JsonValue(Integer.MIN_VALUE));
        oMap.put("longKey1", new JsonValue(Long.MAX_VALUE));
        oMap.put("longKey2", new JsonValue(Long.MIN_VALUE));
        oMap.put("doubleKey1", new JsonValue(Double.MAX_VALUE));
        oMap.put("doubleKey2", new JsonValue(Double.MIN_VALUE));
        oMap.put("floatKey1", new JsonValue(Float.MAX_VALUE));
        oMap.put("floatKey2", new JsonValue(Float.MIN_VALUE));
        oMap.put("bigDecimalKey1", new JsonValue(new BigDecimal("9223372036854775807.123")));
        oMap.put("bigDecimalKey2", new JsonValue(new BigDecimal("-9223372036854775808.123")));
        oMap.put("bigIntegerKey1", new JsonValue(new BigInteger("9223372036854775807")));
        oMap.put("bigIntegerKey2", new JsonValue(new BigInteger("-9223372036854775808")));

        validateMapTypes(oMap, oMap, true);

        // don't keep nulls
        JsonValue parsed = parse(new JsonValue(oMap).toJson(), DECIMALS);
        assertNotNull(parsed.map);
        assertEquals(oMap.size() - 1, parsed.map.size());
        validateMapTypes(parsed.map, oMap, false);

        // keep nulls
        parsed = parse(new JsonValue(oMap).toJson(), KEEP_NULLS, DECIMALS);
        assertNotNull(parsed.map);
        assertEquals(oMap.size(), parsed.map.size());
        validateMapTypes(parsed.map, oMap, true);

        // just some coverage
        assertEquals(parsed.toJson(), new String(parsed.serialize(), StandardCharsets.UTF_8));
        String ts = parsed.toString(getClass());
        assertTrue(ts.startsWith('"' + getClass().getSimpleName() + "\":{"));
        assertTrue(ts.endsWith("}"));
        ts = parsed.toString(getClass().getSimpleName());
        assertTrue(ts.startsWith('"' + getClass().getSimpleName() + "\":{"));
        assertTrue(ts.endsWith("}"));
    }

    private static void validateMapTypes(Map<String, JsonValue> map, Map<String, JsonValue> oMap, boolean original) {
        assertEquals(JsonValueType.BOOL, map.get("trueKey1").type);
        assertEquals(JsonValueType.BOOL, map.get("trueKey2").type);
        assertEquals(JsonValueType.BOOL, map.get("falseKey1").type);
        assertEquals(JsonValueType.BOOL, map.get("falseKey2").type);
        assertEquals(JsonValueType.STRING, map.get("stringKey").type);
        assertEquals(JsonValueType.STRING, map.get("charKey").type);
        assertEquals(JsonValueType.STRING, map.get("escapeStringKey").type);
        assertEquals(JsonValueType.INTEGER, map.get("intKey1").type);
        assertEquals(JsonValueType.INTEGER, map.get("intKey2").type);
        assertEquals(JsonValueType.LONG, map.get("longKey1").type);
        assertEquals(JsonValueType.LONG, map.get("longKey2").type);

        assertNotNull(map.get("trueKey1").bool);
        assertNotNull(map.get("trueKey2").bool);
        assertNotNull(map.get("falseKey1").bool);
        assertNotNull(map.get("falseKey2").bool);
        assertNotNull(map.get("stringKey").string);
        assertNotNull(map.get("escapeStringKey").string);
        assertNotNull(map.get("intKey1").i);
        assertNotNull(map.get("intKey2").i);
        assertNotNull(map.get("longKey1").l);
        assertNotNull(map.get("longKey2").l);

        assertEquals(oMap.get("trueKey1"), map.get("trueKey1"));
        assertEquals(oMap.get("trueKey2"), map.get("trueKey2"));
        assertEquals(oMap.get("falseKey1"), map.get("falseKey1"));
        assertEquals(oMap.get("falseKey2"), map.get("falseKey2"));
        assertEquals(oMap.get("stringKey"), map.get("stringKey"));
        assertEquals(oMap.get("escapeStringKey"), map.get("escapeStringKey"));
        assertEquals(oMap.get("intKey1"), map.get("intKey1"));
        assertEquals(oMap.get("intKey2"), map.get("intKey2"));
        assertEquals(oMap.get("longKey1"), map.get("longKey1"));
        assertEquals(oMap.get("longKey2"), map.get("longKey2"));

        if (original) {
            assertNotNull(oMap.get("intKey1").i);
            assertNotNull(oMap.get("intKey2").i);
            assertNotNull(oMap.get("longKey1").l);
            assertNotNull(oMap.get("longKey2").l);
            assertNotNull(oMap.get("doubleKey1").d);
            assertNotNull(oMap.get("doubleKey2").d);
            assertNotNull(oMap.get("floatKey1").f);
            assertNotNull(oMap.get("floatKey2").f);
            assertNotNull(oMap.get("bigDecimalKey1").bd);
            assertNotNull(oMap.get("bigDecimalKey2").bd);
            assertNotNull(oMap.get("bigIntegerKey1").bi);
            assertNotNull(oMap.get("bigIntegerKey2").bi);

            assertEquals(JsonValueType.NULL, map.get("nullKey").type);
            assertNull(map.get("nullKey").object);
            assertEquals(oMap.get("nullKey"), map.get("nullKey"));
        }
        else {
            assertNotNull(oMap.get("intKey1").number);
            assertNotNull(oMap.get("intKey2").number);
            assertNotNull(oMap.get("longKey1").number);
            assertNotNull(oMap.get("longKey2").number);
            assertNotNull(oMap.get("doubleKey1").number);
            assertNotNull(oMap.get("doubleKey2").number);
            assertNotNull(oMap.get("floatKey1").number);
            assertNotNull(oMap.get("floatKey2").number);
            assertNotNull(oMap.get("bigDecimalKey1").number);
            assertNotNull(oMap.get("bigDecimalKey2").number);
            assertNotNull(oMap.get("bigIntegerKey1").number);
            assertNotNull(oMap.get("bigIntegerKey2").number);
        }
    }

    @Test
    public void testArray() throws JsonParseException {
        List<JsonValue> list = new ArrayList<>();
        list.add(new JsonValue("string"));
        list.add(new JsonValue(true));
        list.add(JsonValue.NULL);
        list.add(JsonValue.EMPTY_MAP);
        list.add(JsonValue.EMPTY_ARRAY);

        JsonValue root = parse(new JsonValue(list).toJson());
        assertNotNull(root.array);
        assertEquals(list.size(), root.array.size());
        List<JsonValue> array = root.array;
        for (int i = 0; i < array.size(); i++) {
            JsonValue v = array.get(i);
            JsonValue p = root.array.get(i);
            assertEquals(v.object, p.object);
            assertTrue(list.contains(v));
        }

        list.clear();
        list.add(new JsonValue(1));
        list.add(new JsonValue(Long.MAX_VALUE));
        list.add(new JsonValue(Double.MAX_VALUE));
        list.add(new JsonValue(Float.MAX_VALUE));
        list.add(new JsonValue(new BigDecimal(Double.toString(Double.MAX_VALUE))));
        list.add(new JsonValue(new BigInteger(Long.toString(Long.MAX_VALUE))));

        root = parse(new JsonValue(list).toJson(), DECIMALS);
        assertNotNull(root.array);
        assertEquals(list.size(), root.array.size());
        array = root.array;
        for (int i = 0; i < array.size(); i++) {
            JsonValue v = array.get(i);
            JsonValue p = root.array.get(i);
            assertEquals(v.object, p.object);
            assertEquals(v.number, p.number);
        }

        List<JsonValue> mappedList = new JsonValue(list).array;
        List<JsonValue> mappedList2 = parse(new JsonValue(mappedList).toJson(), DECIMALS).array;
        List<JsonValue> mappedArray = new JsonValue(list.toArray(new JsonValue[0])).array;
        List<JsonValue> mappedArray2 = parse(new JsonValue(list.toArray(new JsonValue[0])).toJson(), DECIMALS).array;
        assertNotNull(mappedList);
        assertNotNull(mappedList2);
        assertNotNull(mappedArray);
        assertNotNull(mappedArray2);
        for (int i = 0; i < mappedList.size(); i++) {
            JsonValue v = list.get(i);
            JsonValue lv = mappedList.get(i);
            JsonValue lv2 = mappedList2.get(i);
            JsonValue av = mappedArray.get(i);
            JsonValue av2 = mappedArray2.get(i);
            assertNotNull(lv);
            assertNotNull(lv2);
            assertNotNull(av);
            assertNotNull(av2);
            assertEquals(v, lv);
            assertEquals(v, av);

            // conversions are not perfect for doubles and floats, but that's a java thing, not a parser thing
            if (v.type == lv2.type) {
                assertEquals(v, lv2);
            }
            if (v.type == av2.type) {
                assertEquals(v, av2);
            }
        }
    }

    @Test
    public void testConstantsAreReadOnly() {
        //noinspection DataFlowIssue // NO ISSUE, WE KNOW jv.map is NOT NULL
        assertThrows(UnsupportedOperationException.class, () -> JsonValue.EMPTY_MAP.map.put("foo", null));
        //noinspection DataFlowIssue // NO ISSUE, WE KNOW jv.array is NOT NULL
        assertThrows(UnsupportedOperationException.class, () -> JsonValue.EMPTY_ARRAY.array.add(null));
    }

    @Test
    public void testNullJsonValue() {
        assertEquals(JsonValueType.NULL, JsonValue.NULL.type);
        assertNull(JsonValue.NULL.object);
        assertNull(JsonValue.NULL.map);
        assertNull(JsonValue.NULL.array);
        assertNull(JsonValue.NULL.string);
        assertNull(JsonValue.NULL.bool);
        assertNull(JsonValue.NULL.number);
        assertNull(JsonValue.NULL.i);
        assertNull(JsonValue.NULL.l);
        assertNull(JsonValue.NULL.d);
        assertNull(JsonValue.NULL.f);
        assertNull(JsonValue.NULL.bd);
        assertNull(JsonValue.NULL.bi);
        assertEquals(JsonValue.NULL, new JsonValue((String)null));
        assertEquals(JsonValue.NULL, new JsonValue((Boolean) null));
        assertEquals(JsonValue.NULL, new JsonValue((Map<String, JsonValue>)null));
        assertEquals(JsonValue.NULL, new JsonValue((List<JsonValue>)null));
        assertEquals(JsonValue.NULL, new JsonValue((JsonValue[])null));
        assertEquals(JsonValue.NULL, new JsonValue((BigDecimal)null));
        assertEquals(JsonValue.NULL, new JsonValue((BigInteger) null));
    }

    private void validateParse(JsonValue expected, String json) throws JsonParseException {
        char[] ca = json.toCharArray();
        byte[] ba = json.getBytes();

        assertEquals(expected, parse(json));
        assertEquals(expected, parse(json, 0));
        assertEquals(expected, parse(json, KEEP_NULLS));
        assertEquals(expected, parse(ca));
        assertEquals(expected, parse(ca, 0));
        assertEquals(expected, parse(ca, KEEP_NULLS));
        assertEquals(expected, parse(ca, 0, KEEP_NULLS));
        assertEquals(expected, parse(ba));
        assertEquals(expected, parse(ba, 0));
        assertEquals(expected, parse(ba, KEEP_NULLS));
        assertEquals(expected, parse(ba, 0, KEEP_NULLS));

        assertEquals(expected, parseUnchecked(json));
        assertEquals(expected, parseUnchecked(json, 0));
        assertEquals(expected, parseUnchecked(json, KEEP_NULLS));
        assertEquals(expected, parseUnchecked(ca));
        assertEquals(expected, parseUnchecked(ca, 0));
        assertEquals(expected, parseUnchecked(ca, KEEP_NULLS));
        assertEquals(expected, parseUnchecked(ba));
        assertEquals(expected, parseUnchecked(ba, 0));
        assertEquals(expected, parseUnchecked(ba, KEEP_NULLS));
    }

    @Test
    public void testParsingCoverage() throws JsonParseException {
        validateParse(JsonValue.NULL, "");
        validateParse(JsonValue.EMPTY_MAP, "{}");
        validateParse(JsonValue.EMPTY_ARRAY, "[]");

        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> parse("{}", -1));
        assertTrue(iae.getMessage().contains("Invalid start index."));

        validateThrows("{", "Text must end with '}'");
        validateThrows("{{", "Cannot directly nest another Object or Array.");
        validateThrows("{[", "Cannot directly nest another Object or Array.");
        validateThrows("{\"foo\":1 ]", "Expected a ',' or '}'.");
        validateThrows("{\"foo\" 1", "Expected a ':' after a key.");
        validateThrows("[\"bad\",", "Unexpected end of data."); // missing close
        validateThrows("[1Z]", INVALID_VALUE);
        validateThrows("t", INVALID_VALUE);
        validateThrows("f", INVALID_VALUE);
        validateThrows("\"u", "Unterminated string.");
        validateThrows("\"u\r", "Unterminated string.");
        validateThrows("\"u\n", "Unterminated string.");
        validateThrows("\"\\x\"", "Illegal escape.");
        validateThrows("\"\\u000", "Illegal escape.");
        validateThrows("\"\\uzzzz", "Illegal escape.");

        JsonValue v = JsonParser.parse((char[])null);
        assertEquals(JsonValue.NULL, v);

        v = parse("{\"foo\":1,}");
        assertNotNull(v);
        assertNotNull(v.map);
        assertEquals(1, v.map.size());
        assertTrue(v.map.containsKey("foo"));
        assertEquals(1, v.map.get("foo").i);

        v = parse("INFO{\"foo\":1,}", 4);
        assertNotNull(v);
        assertNotNull(v.map);
        assertEquals(1, v.map.size());
        assertTrue(v.map.containsKey("foo"));
        assertEquals(1, v.map.get("foo").i);

        v = parse("[\"foo\",]"); // handles dangling commas fine
        assertNotNull(v);
        assertNotNull(v.array);
        assertEquals(1, v.array.size());
        assertEquals("foo", v.array.get(0).string);

        String s = "foo \b \t \n \f \r \" \\ /";
        String j = "\"" + jsonEncode(s) + "\"";
        v = parse(j);
        assertNotNull(v.string);
        assertEquals(s, v.string);

        // every constructor
        String json = "{}";
        new JsonParser(json.toCharArray());
        new JsonParser(json.toCharArray(), KEEP_NULLS);
        parse(json.toCharArray());
        parse(json.toCharArray(), 0);
        parse(json.toCharArray(), KEEP_NULLS);
        parse(json.toCharArray(), 0, KEEP_NULLS);
        parse(json);
        parse(json, 0);
        parse(json, KEEP_NULLS);
        parse(json, 0, KEEP_NULLS);
        parse(json.getBytes());
        parse(json.getBytes(), KEEP_NULLS);
        parse(json.getBytes(), 0, KEEP_NULLS);
        parseUnchecked(json.toCharArray());
        parseUnchecked(json.toCharArray(), 0);
        parseUnchecked(json.toCharArray(), KEEP_NULLS);
        parseUnchecked(json.toCharArray(), 0, KEEP_NULLS);
        parseUnchecked(json);
        parseUnchecked(json, 0);
        parseUnchecked(json, KEEP_NULLS);
        parseUnchecked(json, 0, KEEP_NULLS);
        parseUnchecked(json.getBytes());
        parseUnchecked(json.getBytes(), KEEP_NULLS);
        parseUnchecked(json.getBytes(), 0, KEEP_NULLS);

        // misc
        json = ResourceUtils.resourceAsString("stream_info.json");
        JsonValue jv = JsonParser.parseUnchecked(json);
        printFormatted(jv);
        printFormatted(json);
        printFormatted((Object)json);
        assertEquals("\"JsonParsingTests\":", toKey(this.getClass()));
        JsonParser.parseUnchecked(json, KEEP_NULLS);
        JsonParser.parseUnchecked(json, (JsonParser.Option) null);
        JsonParser.parseUnchecked(json, (JsonParser.Option[]) null);

        printFormatted(",[,]zzz"); // BRANCH COVERAGE
    }

    private void validateThrows(String json, String errorText) {
        // also provides coverage for every constructor
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json.toCharArray())));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json.toCharArray(), 0)));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json.toCharArray(), KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json.toCharArray(), 0, KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json)));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json, 0)));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json, KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json, 0, KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json.getBytes())));
        validateThrowError(errorText, assertThrows(JsonParseException.class, () -> parse(json.getBytes(), KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json.toCharArray())));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json.toCharArray(), 0)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json.toCharArray(), KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json.toCharArray(), 0, KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json, 0)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json, KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json, 0, KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json.getBytes())));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json.getBytes(), 0)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json.getBytes(), KEEP_NULLS)));
        validateThrowError(errorText, assertThrows(RuntimeException.class, () -> parseUnchecked(json.getBytes(), 0, KEEP_NULLS)));
    }

    private static void validateThrowError(String errorText, Exception e) {
        assertTrue(e.getMessage().contains(errorText));
    }

    @Test
    public void testNumberParsing() throws JsonParseException {
        assertEquals(JsonValueType.INTEGER, parse("1").type);
        assertEquals(JsonValueType.INTEGER, parse(Integer.toString(Integer.MAX_VALUE)).type);
        assertEquals(JsonValueType.INTEGER, parse(Integer.toString(Integer.MIN_VALUE)).type);
        assertEquals(JsonValueType.LONG, parse(Long.toString((long)Integer.MAX_VALUE + 1)).type);
        assertEquals(JsonValueType.LONG, parse(Long.toString((long)Integer.MIN_VALUE - 1)).type);
        assertEquals(JsonValueType.DOUBLE, parse("-0", DECIMALS).type);
        assertEquals(JsonValueType.DOUBLE, parse("-0.0", DECIMALS).type);
        assertEquals(JsonValueType.DOUBLE, parse("0.1d", DECIMALS).type);
        assertEquals(JsonValueType.DOUBLE, parse("0.f", DECIMALS).type);
        assertEquals(JsonValueType.DOUBLE, parse("0.1f", DECIMALS).type);
        assertEquals(JsonValueType.DOUBLE, parse("-0x1.fffp1", DECIMALS).type);
        assertEquals(JsonValueType.DOUBLE, parse("0x1.0P-1074", DECIMALS).type);
        assertEquals(JsonValueType.BIG_DECIMAL, parse("0.2", DECIMALS).type);
        assertEquals(JsonValueType.BIG_DECIMAL, parse("244273.456789012345", DECIMALS).type);
        assertEquals(JsonValueType.BIG_DECIMAL, parse("244273.456789012345", DECIMALS).type);
        assertEquals(JsonValueType.BIG_DECIMAL, parse("0.1234567890123456789", DECIMALS).type);
        assertEquals(JsonValueType.BIG_DECIMAL, parse("-24.42e7345", DECIMALS).type);
        assertEquals(JsonValueType.BIG_DECIMAL, parse("-24.42E7345", DECIMALS).type);
        assertEquals(JsonValueType.BIG_DECIMAL, parse("-.01", DECIMALS).type);
        assertEquals(JsonValueType.BIG_DECIMAL, parse("00.001", DECIMALS).type);
        assertEquals(JsonValueType.BIG_INTEGER, parse("12345678901234567890").type);

        assertTrue(JsonParser.isDecimalNotation("-0"));
        assertTrue(JsonParser.isDecimalNotation("1.1"));
        assertTrue(JsonParser.isDecimalNotation("-24e7345"));
        assertTrue(JsonParser.isDecimalNotation("-24E7345"));
        assertFalse(JsonParser.isDecimalNotation("12345"));

        String str = new BigInteger( Long.toString(Long.MAX_VALUE) ).add( BigInteger.ONE ).toString();
        assertEquals(JsonValueType.BIG_INTEGER, parse(str).type);

        validateThrows("-0x123", INVALID_VALUE);
        JsonParseException e;

        e = assertThrows(JsonParseException.class, () -> parse("-"));
        assertTrue(e.getMessage().contains(INVALID_VALUE));

        e = assertThrows(JsonParseException.class, () -> parse("00"));
        assertTrue(e.getMessage().contains(INVALID_VALUE));

        e = assertThrows(JsonParseException.class, () -> parse("NaN"));
        assertTrue(e.getMessage().contains(INVALID_VALUE));

        e = assertThrows(JsonParseException.class, () -> parse("-NaN"));
        assertTrue(e.getMessage().contains(INVALID_VALUE));

        e = assertThrows(JsonParseException.class, () -> parse("Infinity"));
        assertTrue(e.getMessage().contains(INVALID_VALUE));

        e = assertThrows(JsonParseException.class, () -> parse("-Infinity"));
        assertTrue(e.getMessage().contains(INVALID_VALUE));
    }

    @Test
    public void testValueUtilsInstanceDuration() {
        JsonValue v = JsonValue.instance(Duration.ofSeconds(1));
        assertNotNull(v.l);
        assertEquals(1000000000L, v.l);
    }

    static class TestSerializableMap implements JsonSerializable {
        @Override
        @NonNull
        public String toJson() {
            JsonValue v = new JsonValue(new HashMap<>());
            //noinspection DataFlowIssue // NO ISSUE, WE KNOW jv.map is NOT NULL
            v.map.put("a", new JsonValue("A"));
            v.map.put("b", new JsonValue("B"));
            v.map.put("c", new JsonValue("C"));
            return v.toJson();
        }
    }

    static class TestSerializableList implements JsonSerializable {
        @Override
        @NonNull
        public String toJson() {
            JsonValue v = new JsonValue(new ArrayList<>());
            //noinspection DataFlowIssue // NO ISSUE, WE KNOW jv.array is NOT NULL
            v.array.add(new JsonValue("X"));
            v.array.add(new JsonValue("Y"));
            v.array.add(new JsonValue("Z"));
            return v.toJson();
        }
    }

    @Test
    public void testValueUtilsInstanceList() {
        List<Object> list = new ArrayList<>();
        list.add("Hello");
        list.add("");
        list.add(" ");
        list.add('c');
        list.add(1);
        list.add(1L);
        list.add(1D);
        list.add(1F);
        list.add(new BigDecimal("1.0"));
        list.add(new BigInteger("1"));
        list.add(true);
        list.add(new HashMap<>());
        list.add(new ArrayList<>());
        list.add(new HashSet<>());
        list.add(new TestSerializableMap());
        list.add(new TestSerializableList());
        list.add(null);
        JsonValue v = JsonValue.instance(list);
        assertNotNull(v.array);
        assertEquals(17, v.array.size());
        int ix = 0;
        assertEquals(JsonValueType.STRING, v.array.get(ix).type);
        assertEquals(JsonValueType.STRING, v.array.get(++ix).type);
        assertEquals(JsonValueType.STRING, v.array.get(++ix).type);
        assertEquals(JsonValueType.STRING, v.array.get(++ix).type);
        assertEquals(JsonValueType.INTEGER, v.array.get(++ix).type);
        assertEquals(JsonValueType.LONG, v.array.get(++ix).type);
        assertEquals(JsonValueType.DOUBLE, v.array.get(++ix).type);
        assertEquals(JsonValueType.FLOAT, v.array.get(++ix).type);
        assertEquals(JsonValueType.BIG_DECIMAL, v.array.get(++ix).type);
        assertEquals(JsonValueType.BIG_INTEGER, v.array.get(++ix).type);
        assertEquals(JsonValueType.BOOL, v.array.get(++ix).type);
        assertEquals(JsonValueType.MAP, v.array.get(++ix).type);
        assertEquals(JsonValueType.ARRAY, v.array.get(++ix).type);
        assertEquals(JsonValueType.ARRAY, v.array.get(++ix).type);
        assertEquals(JsonValueType.MAP, v.array.get(++ix).type);
        assertEquals(JsonValueType.ARRAY, v.array.get(++ix).type);
    }

    @Test
    public void testValueUtilsInstanceMap() {
        validateMap(instance(getValuesMap()), false, false, true);
    }

    private static @NonNull Map<String, Object> getValuesMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("string", "Hello");
        map.put("empty_is_string", "");
        map.put("space_is_string", " ");
        map.put("char", 'c');
        map.put("int", 1);
        map.put("long", Long.MAX_VALUE);
        map.put("double", 1D);
        map.put("float", 1F);
        map.put("bd", new BigDecimal("1.0"));
        map.put("bi", new BigInteger(Long.toString(Long.MAX_VALUE)));
        map.put("bool", true);
        map.put("map", new HashMap<>());
        map.put("list", new ArrayList<>());
        map.put("set", new HashSet<>());
        map.put("smap", new TestSerializableMap());
        map.put("slist", new TestSerializableList());
        map.put("jv", JsonValue.EMPTY_MAP);
        map.put("null", null);
        map.put("jvNull", JsonValue.NULL);
        return map;
    }

    @Test
    public void testValueUtilsMapBuilder() {
        Map<String, Object> map = getValuesMap();

        MapBuilder builder = MapBuilder.instance(true);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }

        builder.jv.toJson(); // COVERAGE
        validateMap(builder.toJsonValue(), false, false, true);
        validateMap(JsonParser.parseUnchecked(builder.toJson(), DECIMALS), true, false, true);
        validateMap(JsonParser.parseUnchecked(builder.toJson(), KEEP_NULLS, DECIMALS), true, true, true);

        //noinspection DataFlowIssue // NO ISSUE, WE KNOW jv.map is NOT NULL
        builder.jv.map.put("jvNull", JsonValue.NULL); // because the original map builder does not put nulls
        MapBuilder builder2 = MapBuilder.instance(true)
            .put("map", builder.jv.map);
        //noinspection DataFlowIssue // NO ISSUE, WE KNOW jv.map is NOT NULL
        validateMap(builder2.jv.map.get("map"), false, false, true);

        builder2 = MapBuilder.instance(true).putEntries(builder.jv.map);
        validateMap(builder2.jv, false, false, true);

        builder2 = MapBuilder.instance(true).putEntries(null);
        assertNotNull(builder2.jv.map);
        assertTrue(builder2.jv.map.isEmpty());

        builder = MapBuilder.instance(false);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }

        validateMap(builder.toJsonValue(), false, false, false);
        validateMap(JsonParser.parseUnchecked(builder.toJson(), DECIMALS), true, false, false);
        validateMap(JsonParser.parseUnchecked(builder.toJson(), KEEP_NULLS, DECIMALS), true, true, false);
    }

    private static void validateMap(JsonValue v, boolean parsed, boolean parsedKeepNulls, boolean putNulls) {
        assertNotNull(v.map);
        assertEquals(JsonValueType.STRING, v.map.get("string").type);
        assertEquals(JsonValueType.STRING, v.map.get("empty_is_string").type);
        assertEquals(JsonValueType.STRING, v.map.get("space_is_string").type);
        assertEquals(JsonValueType.STRING, v.map.get("char").type);
        assertEquals(JsonValueType.INTEGER, v.map.get("int").type);
        assertEquals(JsonValueType.LONG, v.map.get("long").type);
        if (parsed) {
            assertEquals(JsonValueType.BIG_DECIMAL, v.map.get("double").type);
            assertEquals(JsonValueType.BIG_DECIMAL, v.map.get("float").type);
            assertEquals(JsonValueType.LONG, v.map.get("bi").type);
        }
        else {
            assertEquals(JsonValueType.DOUBLE, v.map.get("double").type);
            assertEquals(JsonValueType.FLOAT, v.map.get("float").type);
            assertEquals(JsonValueType.BIG_INTEGER, v.map.get("bi").type);
            if (putNulls) {
                assertEquals(JsonValueType.NULL, v.map.get("null").type);
                assertEquals(JsonValueType.NULL, v.map.get("jvNull").type);
            }
        }
        int size = 19;
        if ((parsed && !parsedKeepNulls) || !putNulls) {
            size = 17;
        }
        assertEquals(size, v.map.size());
        assertEquals(JsonValueType.BIG_DECIMAL, v.map.get("bd").type);
        assertEquals(JsonValueType.BOOL, v.map.get("bool").type);
        assertEquals(JsonValueType.MAP, v.map.get("map").type);
        assertEquals(JsonValueType.ARRAY, v.map.get("list").type);
        assertEquals(JsonValueType.ARRAY, v.map.get("set").type);
        assertEquals(JsonValueType.MAP, v.map.get("smap").type);
        assertEquals(JsonValueType.ARRAY, v.map.get("slist").type);
        assertEquals(JsonValueType.MAP, v.map.get("jv").type);
    }

    @Test
    public void testValueUtilsArrayBuilder() {
        ArrayBuilder builder1 = makeArrayBuilder(true);
        validateArray(false, builder1.toJsonValue(), true);
        validateArray(true, JsonParser.parseUnchecked(builder1.toJson(), DECIMALS), true);

        ArrayBuilder builder2 = makeArrayBuilder(false);
        validateArray(false, builder2.toJsonValue(), false);
        validateArray(true, JsonParser.parseUnchecked(builder2.toJson(), DECIMALS), false);

        ArrayBuilder builder3 = ArrayBuilder.instance(true).addItems(builder1.jv.array);
        validateArray(false, builder3.jv, true);

        ArrayBuilder builder4 = ArrayBuilder.instance().addItems(null);
        assertNotNull(builder4.jv.array);
        assertTrue(builder4.jv.array.isEmpty());

        ArrayBuilder builder5 = ArrayBuilder.instance(false).addItems(builder1.jv.array);
        validateArray(false, builder5.jv, false);
    }

    private static ArrayBuilder makeArrayBuilder(boolean addNulls) {
        List<Object> list = getValuesList();
        ArrayBuilder builder = ArrayBuilder.instance(addNulls);
        for (Object value : list) {
            builder.add(value);
        }
        return builder;
    }

    @Test
    public void testValueUtilsInstanceArray() {
        validateArray(false, JsonValue.instance(getValuesList()), true);
    }

    private static @NonNull List<Object> getValuesList() {
        List<Object> list = new ArrayList<>();
        list.add("Hello");
        list.add('c');
        list.add(1);
        list.add(Long.MAX_VALUE);
        list.add(1D);
        list.add(1F);
        list.add(new BigDecimal("1.0"));
        list.add(new BigInteger(Long.toString(Long.MAX_VALUE)));
        list.add(true);
        list.add(new HashMap<>());
        list.add(new ArrayList<>());
        list.add(new TestSerializableMap());
        list.add(new TestSerializableList());
        list.add(JsonValue.EMPTY_MAP);
        list.add(null);
        list.add(JsonValue.NULL);
        return list;
    }

    private static void validateArray(boolean parsed, JsonValue v, boolean addNulls) {
        assertNotNull(v.array);
        assertEquals(JsonValueType.STRING, v.array.get(0).type);
        assertEquals(JsonValueType.STRING, v.array.get(1).type);
        assertEquals(JsonValueType.INTEGER, v.array.get(2).type);
        assertEquals(JsonValueType.LONG, v.array.get(3).type);
        if (parsed) {
            assertEquals(JsonValueType.BIG_DECIMAL, v.array.get(4).type);
            assertEquals(JsonValueType.BIG_DECIMAL, v.array.get(5).type);
            assertEquals(JsonValueType.LONG, v.array.get(7).type);
        }
        else {
            assertEquals(JsonValueType.DOUBLE, v.array.get(4).type);
            assertEquals(JsonValueType.FLOAT, v.array.get(5).type);
            assertEquals(JsonValueType.BIG_INTEGER, v.array.get(7).type);
        }
        assertEquals(JsonValueType.BIG_DECIMAL, v.array.get(6).type);
        assertEquals(JsonValueType.BOOL, v.array.get(8).type);
        assertEquals(JsonValueType.MAP, v.array.get(9).type);
        assertEquals(JsonValueType.ARRAY, v.array.get(10).type);
        assertEquals(JsonValueType.MAP, v.array.get(11).type);
        assertEquals(JsonValueType.ARRAY, v.array.get(12).type);
        assertEquals(JsonValueType.MAP, v.array.get(13).type);

        if (addNulls) {
            assertEquals(16, v.array.size());
            assertEquals(JsonValueType.NULL, v.array.get(14).type);
            assertEquals(JsonValueType.NULL, v.array.get(15).type);
        }
        else {
            assertEquals(14, v.array.size());
        }
    }

    @Test
    public void testJsonParseException() {
        Exception cause = new Exception("cause");
        JsonParseException e1 = new JsonParseException("e1");
        JsonParseException e2 = new JsonParseException("e2", cause);
        JsonParseException e3 = new JsonParseException(cause);
        assertEquals("e1", e1.getMessage());
        assertNull(e1.getCause());
        assertEquals("e2", e2.getMessage());
        assertNotNull(e2.getCause());
        assertEquals("java.lang.Exception: cause", e3.getMessage());
        assertNotNull(e3.getCause());
    }

    @Test
    public void testCoverageAndEdges() {
        ArrayBuilder arrayBuilder = ArrayBuilder.instance();
        for (String u : UTF_STRINGS) {
            arrayBuilder.add(u);
        }
        arrayBuilder.add("hasU\0U");

        String json = arrayBuilder.toJson();
        JsonValue jv = JsonParser.parseUnchecked(json);
        String json2 = jv.toJson();
        assertEquals(json, json2);
    }

    @Test
    public void testBasicsEqualsAndHashcode() {
        Map<String, JsonValue> map1 = new HashMap<>();
        map1.put("key1", new JsonValue("value1"));
        Map<String, JsonValue> map2 = new HashMap<>();
        map2.put("key2", new JsonValue("value2"));
        List<JsonValue> list1 = new ArrayList<>();
        list1.add(new JsonValue("item1"));
        List<JsonValue> list2 = new ArrayList<>();
        list2.add(new JsonValue("item2"));
        JsonValue[] array1 = {new JsonValue("item1"), new JsonValue(41)};
        JsonValue[] array2 = {new JsonValue("item2"), new JsonValue(42)};

        JsonValue[] jvs = new JsonValue[]{
            new JsonValue("test string"),
            new JsonValue("another string"),
            new JsonValue(true),
            new JsonValue(false),
            new JsonValue(42),
            new JsonValue(43),
            new JsonValue(72L),
            new JsonValue(73L),
            new JsonValue(3.14),
            new JsonValue(3.15),
            new JsonValue(3.14f),
            new JsonValue(3.15f),
            new JsonValue(new BigDecimal("123.456")),
            new JsonValue(new BigDecimal("321.456")),
            new JsonValue(new BigInteger("123456")),
            new JsonValue(new BigInteger("123457")),
            new JsonValue(map1),
            new JsonValue(map2),
            new JsonValue(list1),
            new JsonValue(list2),
            new JsonValue(array1),
            new JsonValue(array2),
            JsonValue.NULL
        };
        for (int i = 0; i < jvs.length; i++) {
            for (int j = 0; j < jvs.length; j++) {
                if (i == j) {
                    assertEquals(jvs[i], jvs[j]);
                    assertEquals(jvs[i].hashCode(), jvs[j].hashCode());
                }
                else {
                    assertNotEquals(jvs[i], jvs[j]);
                    assertNotEquals(jvs[i].hashCode(), jvs[j].hashCode());
                }
            }
        }

        //noinspection ConstantValue,SimplifiableAssertion
        assertFalse(jvs[0].equals(null));

        //noinspection SimplifiableAssertion,EqualsBetweenInconvertibleTypes
        assertFalse(jvs[0].equals("string"));
    }

    @Test
    public void testMapOrder() {
        Map<String, JsonValue> map = new HashMap<>();
        map.put("key1", new JsonValue("value1"));
        map.put("key2", new JsonValue("value2"));
        map.put("key3", new JsonValue("value3"));
        JsonValue jv = new JsonValue(map);

        jv.addMapOrder("key1");
        jv.addMapOrder("key2");
        jv.addMapOrder("key3");

        String json = jv.toJson();
        int x1 = json.indexOf("key1");
        int x2 = json.indexOf("key2");
        int x3 = json.indexOf("key3");
        assertTrue(x1 < x2);
        assertTrue(x1 < x3);
        assertTrue(x2 < x3);

        jv.setMapOrder("key2", "key3", "key1");
        json = jv.toJson();
        x1 = json.indexOf("key2");
        x2 = json.indexOf("key3");
        x3 = json.indexOf("key1");
        assertTrue(x1 < x2);
        assertTrue(x1 < x3);
        assertTrue(x2 < x3);

        jv.setMapOrder(Arrays.asList("key3", "key1", "key2"));
        json = jv.toJson();
        x1 = json.indexOf("key3");
        x2 = json.indexOf("key1");
        x3 = json.indexOf("key2");
        assertTrue(x1 < x2);
        assertTrue(x1 < x3);
        assertTrue(x2 < x3);

        assertThrows(IllegalStateException.class, () -> new JsonValue("not a map").addMapOrder("key"));
        assertThrows(IllegalStateException.class, () -> new JsonValue("not a map").setMapOrder("key"));
        assertThrows(IllegalStateException.class, () -> new JsonValue("not a map").setMapOrder(Arrays.asList("key", "another")));
    }
}
