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

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Value-based equality / hashCode tests across all three JSON value representations:
 * {@link JsonValue} (eager), {@link IndexedJsonValue} (lazy leaves, eager tree) and
 * {@link LazyJsonValue} (lazy leaves and tree). Every value-equality invariant is
 * checked in all 6 pairwise directions plus hashCode parity.
 */
public final class JsonValueEqualityTests {

    // ---- helpers ----------------------------------------------------------

    private static JsonValue jv(String json) {
        return JsonParser.parseUnchecked(json);
    }

    private static JsonValue jv(String json, JsonParser.Option opt) {
        return JsonParser.parseUnchecked(json, opt);
    }

    private static LazyJsonValue lj(String json) {
        return LazyJsonParser.parseUnchecked(json);
    }

    private static LazyJsonValue lj(String json, JsonParser.Option opt) {
        return LazyJsonParser.parseUnchecked(json, opt);
    }

    private static IndexedJsonValue ij(String json) {
        return IndexedJsonParser.parseUnchecked(json);
    }

    private static IndexedJsonValue ij(String json, JsonParser.Option opt) {
        return IndexedJsonParser.parseUnchecked(json, opt);
    }

    /**
     * Assert that the three given values (one of each representation) are all equal
     * to each other (in both directions) and share the same hashCode.
     */
    private static void assertAllEqual(JsonValue a, LazyJsonValue b, IndexedJsonValue c) {
        // 6 directed equality checks
        //noinspection AssertBetweenInconvertibleTypes
        assertEquals(a, b, "JsonValue.equals(LazyJsonValue) failed");
        //noinspection AssertBetweenInconvertibleTypes
        assertEquals(b, a, "LazyJsonValue.equals(JsonValue) failed");
        //noinspection AssertBetweenInconvertibleTypes
        assertEquals(a, c, "JsonValue.equals(IndexedJsonValue) failed");
        //noinspection AssertBetweenInconvertibleTypes
        assertEquals(c, a, "IndexedJsonValue.equals(JsonValue) failed");
        //noinspection AssertBetweenInconvertibleTypes
        assertEquals(b, c, "LazyJsonValue.equals(IndexedJsonValue) failed");
        //noinspection AssertBetweenInconvertibleTypes
        assertEquals(c, b, "IndexedJsonValue.equals(LazyJsonValue) failed");

        // hashCode parity across all three
        assertEquals(a.hashCode(), b.hashCode(), "hashCode differs JsonValue vs LazyJsonValue");
        assertEquals(a.hashCode(), c.hashCode(), "hashCode differs JsonValue vs IndexedJsonValue");
        assertEquals(b.hashCode(), c.hashCode(), "hashCode differs LazyJsonValue vs IndexedJsonValue");
    }

    /**
     * Assert that no value in triple (a,b,c) equals any value in triple (x,y,z).
     * 9 pairwise inequality checks.
     */
    private static void assertNonePairwiseEqual(JsonValue a, LazyJsonValue b, IndexedJsonValue c,
                                                JsonValue x, LazyJsonValue y, IndexedJsonValue z) {
        assertNotEquals(a, x); assertNotEquals(a, y); assertNotEquals(a, z);
        assertNotEquals(b, x); assertNotEquals(b, y); assertNotEquals(b, z);
        assertNotEquals(c, x); assertNotEquals(c, y); assertNotEquals(c, z);
    }

    // ---- STRING ----------------------------------------------------------

    @Test
    public void testString() {
        assertAllEqual(new JsonValue("hello"), lj("\"hello\""), ij("\"hello\""));
    }

    @Test
    public void testStringInequality() {
        assertNonePairwiseEqual(
            new JsonValue("hello"), lj("\"hello\""), ij("\"hello\""),
            new JsonValue("world"), lj("\"world\""), ij("\"world\""));
    }

    @Test
    public void testStringEscapesAreNormalized() {
        // "AB" via direct, "AB" via escapes — same logical string
        JsonValue a = new JsonValue("AB");
        LazyJsonValue b = lj("\"\\u0041\\u0042\"");
        IndexedJsonValue c = ij("\"\\u0041\\u0042\"");
        assertAllEqual(a, b, c);
    }

    // ---- BOOL / NULL -----------------------------------------------------

    @Test
    public void testBoolTrue() {
        assertAllEqual(JsonValue.TRUE, lj("true"), ij("true"));
    }

    @Test
    public void testBoolFalse() {
        assertAllEqual(JsonValue.FALSE, lj("false"), ij("false"));
    }

    @Test
    public void testBoolInequality() {
        assertNonePairwiseEqual(
            JsonValue.TRUE, lj("true"), ij("true"),
            JsonValue.FALSE, lj("false"), ij("false"));
    }

    @Test
    public void testNull() {
        assertAllEqual(JsonValue.NULL, lj("null"), ij("null"));
    }

    // ---- NUMBERS ---------------------------------------------------------

    @Test
    public void testInteger() {
        assertAllEqual(new JsonValue(42), lj("42"), ij("42"));
    }

    @Test
    public void testIntegerInequality() {
        assertNonePairwiseEqual(
            new JsonValue(42), lj("42"), ij("42"),
            new JsonValue(43), lj("43"), ij("43"));
    }

    @Test
    public void testLong() {
        long big = 3_000_000_000L;
        assertAllEqual(new JsonValue(big), lj("3000000000"), ij("3000000000"));
    }

    @Test
    public void testBigDecimal() {
        BigDecimal bd = new BigDecimal("3.14");
        assertAllEqual(
            new JsonValue(bd),
            lj("3.14", JsonParser.Option.DECIMALS),
            ij("3.14", JsonParser.Option.DECIMALS));
    }

    @Test
    public void testBigInteger() {
        BigInteger huge = new BigInteger("99999999999999999999999999");
        assertAllEqual(new JsonValue(huge), lj(huge.toString()), ij(huge.toString()));
    }

    @Test
    public void testNumberTypeMismatch() {
        // 42 (INTEGER) vs 42.0 (BIG_DECIMAL under DECIMALS) are different types — not equal.
        assertNonePairwiseEqual(
            new JsonValue(42), lj("42"), ij("42"),
            new JsonValue(new BigDecimal("42.0")),
            lj("42.0", JsonParser.Option.DECIMALS),
            ij("42.0", JsonParser.Option.DECIMALS));
    }

    // ---- ARRAY -----------------------------------------------------------

    @Test
    public void testArray() {
        List<JsonValue> arr = Arrays.asList(new JsonValue(1), new JsonValue(2), new JsonValue(3));
        assertAllEqual(new JsonValue(arr), lj("[1,2,3]"), ij("[1,2,3]"));
    }

    @Test
    public void testArrayOrderMatters() {
        List<JsonValue> arr123 = Arrays.asList(new JsonValue(1), new JsonValue(2), new JsonValue(3));
        List<JsonValue> arr321 = Arrays.asList(new JsonValue(3), new JsonValue(2), new JsonValue(1));
        assertNonePairwiseEqual(
            new JsonValue(arr123), lj("[1,2,3]"), ij("[1,2,3]"),
            new JsonValue(arr321), lj("[3,2,1]"), ij("[3,2,1]"));
    }

    @Test
    public void testEmptyArray() {
        assertAllEqual(JsonValue.EMPTY_ARRAY, lj("[]"), ij("[]"));
    }

    // ---- MAP -------------------------------------------------------------

    @Test
    public void testMap() {
        Map<String, JsonValue> m = new LinkedHashMap<>();
        m.put("a", new JsonValue(1));
        m.put("b", new JsonValue("x"));
        assertAllEqual(
            new JsonValue(m),
            lj("{\"a\":1,\"b\":\"x\"}"),
            ij("{\"a\":1,\"b\":\"x\"}"));
    }

    @Test
    public void testMapOrderDoesNotMatter() {
        // HashMap equality is order-independent — same content, different ordering still equal.
        Map<String, JsonValue> m = new LinkedHashMap<>();
        m.put("a", new JsonValue(1));
        m.put("b", new JsonValue("x"));
        assertAllEqual(
            new JsonValue(m),
            lj("{\"b\":\"x\",\"a\":1}"),
            ij("{\"b\":\"x\",\"a\":1}"));
    }

    @Test
    public void testMapInequality() {
        Map<String, JsonValue> m1 = new LinkedHashMap<>();
        m1.put("a", new JsonValue(1));
        Map<String, JsonValue> m2 = new LinkedHashMap<>();
        m2.put("a", new JsonValue(2));
        assertNonePairwiseEqual(
            new JsonValue(m1), lj("{\"a\":1}"), ij("{\"a\":1}"),
            new JsonValue(m2), lj("{\"a\":2}"), ij("{\"a\":2}"));
    }

    @Test
    public void testMapExtraKey() {
        Map<String, JsonValue> m1 = new LinkedHashMap<>();
        m1.put("a", new JsonValue(1));
        Map<String, JsonValue> m2 = new LinkedHashMap<>();
        m2.put("a", new JsonValue(1));
        m2.put("b", new JsonValue(2));
        assertNonePairwiseEqual(
            new JsonValue(m1), lj("{\"a\":1}"), ij("{\"a\":1}"),
            new JsonValue(m2), lj("{\"a\":1,\"b\":2}"), ij("{\"a\":1,\"b\":2}"));
    }

    @Test
    public void testNestedMapAndArray() {
        // {"outer":{"inner":[1,2,3]}}
        JsonValue inner = new JsonValue(Arrays.asList(new JsonValue(1), new JsonValue(2), new JsonValue(3)));
        Map<String, JsonValue> innerMap = new LinkedHashMap<>();
        innerMap.put("inner", inner);
        Map<String, JsonValue> outerMap = new LinkedHashMap<>();
        outerMap.put("outer", new JsonValue(innerMap));

        assertAllEqual(
            new JsonValue(outerMap),
            lj("{\"outer\":{\"inner\":[1,2,3]}}"),
            ij("{\"outer\":{\"inner\":[1,2,3]}}"));
    }

    @Test
    public void testEmptyMap() {
        assertAllEqual(JsonValue.EMPTY_MAP, lj("{}"), ij("{}"));
    }

    // ---- TYPE MISMATCH ---------------------------------------------------

    @Test
    public void testStringVersusNumber() {
        // "42" (STRING) vs 42 (INTEGER) — different types
        assertNonePairwiseEqual(
            new JsonValue("42"), lj("\"42\""), ij("\"42\""),
            new JsonValue(42), lj("42"), ij("42"));
    }

    @Test
    public void testMapVersusArray() {
        assertNonePairwiseEqual(
            JsonValue.EMPTY_MAP, lj("{}"), ij("{}"),
            JsonValue.EMPTY_ARRAY, lj("[]"), ij("[]"));
    }

    @Test
    public void testBoolVersusNumber() {
        assertNonePairwiseEqual(
            JsonValue.TRUE, lj("true"), ij("true"),
            new JsonValue(1), lj("1"), ij("1"));
    }

    // ---- IDENTITY / NULL / OTHER OBJECT ----------------------------------

    @Test
    public void testReflexive() {
        JsonValue a = new JsonValue("hello");
        LazyJsonValue b = lj("\"hello\"");
        IndexedJsonValue c = ij("\"hello\"");
        assertEquals(a, a);
        assertEquals(b, b);
        assertEquals(c, c);
    }

    @Test
    public void testNullParameter() {
        assertNotEquals(new JsonValue("x"), null);
        assertNotEquals(lj("\"x\""), null);
        assertNotEquals(ij("\"x\""), null);
    }

    @Test
    public void testForeignObject() {
        assertNotEquals(new JsonValue("x"), "x");
        assertNotEquals(lj("\"x\""), "x");
        assertNotEquals(ij("\"x\""), "x");

        assertNotEquals(new JsonValue(42), 42);
        assertNotEquals(lj("42"), 42);
        assertNotEquals(ij("42"), 42);
    }

    // ---- hashCode contract -----------------------------------------------

    @Test
    public void testHashCodeIsStable() {
        // Calling hashCode multiple times on the same instance returns the same value.
        JsonValue a = new JsonValue("hello");
        LazyJsonValue b = lj("{\"a\":1,\"b\":[2,3]}");
        IndexedJsonValue c = ij("[1,\"x\",true,null]");

        assertEquals(a.hashCode(), a.hashCode());
        assertEquals(b.hashCode(), b.hashCode());
        assertEquals(c.hashCode(), c.hashCode());
    }

    // ---- TRANSITIVITY ----------------------------------------------------

    @Test
    public void testTransitivityForAllLeafTypes() {
        // Verify a == b, b == c, a == c for each leaf type.
        assertAllEqual(new JsonValue("hello"), lj("\"hello\""), ij("\"hello\""));
        assertAllEqual(JsonValue.TRUE, lj("true"), ij("true"));
        assertAllEqual(JsonValue.FALSE, lj("false"), ij("false"));
        assertAllEqual(JsonValue.NULL, lj("null"), ij("null"));
        assertAllEqual(new JsonValue(42), lj("42"), ij("42"));
        assertAllEqual(new JsonValue(3_000_000_000L), lj("3000000000"), ij("3000000000"));
    }
}
