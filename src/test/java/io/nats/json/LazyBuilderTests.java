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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class LazyBuilderTests {

    // ---- a JsonSerializable to convert from ----

    static final class Sample implements JsonSerializable {
        final String name;
        final int count;

        Sample(String name, int count) {
            this.name = name;
            this.count = count;
        }

        @Override
        public String toJson() {
            return toJsonValue().toJson();
        }

        @Override
        public JsonValue toJsonValue() {
            return MapBuilder.instance().put("name", name).put("count", count).toJsonValue();
        }
    }

    // ---- builder: leaf types ----

    @Test
    public void testLeafTypes() {
        LazyJsonValue v = LazyMapBuilder.instance()
            .put("s", "hello")
            .put("i", 42)
            .put("l", 3000000000L)
            .put("d", 3.5d)
            .put("bt", true)
            .put("bf", false)
            .build();

        Map<String, LazyJsonValue> m = v.getMap();
        assertNotNull(m);
        assertEquals("hello", m.get("s").getString());
        assertEquals(Integer.valueOf(42), m.get("i").getInteger());
        assertEquals(Long.valueOf(3000000000L), m.get("l").getLong());
        assertEquals(Double.valueOf(3.5d), m.get("d").getDouble());
        assertEquals(Boolean.TRUE, m.get("bt").getBoolean());
        assertEquals(Boolean.FALSE, m.get("bf").getBoolean());
        assertSame(LazyJsonValue.TRUE, m.get("bt"));
        assertSame(LazyJsonValue.FALSE, m.get("bf"));
    }

    // ---- builder: nested map and array ----

    @Test
    public void testNestedMapAndArray() {
        LazyJsonValue nested = LazyMapBuilder.instance().put("a", 1).build();
        LazyJsonValue v = LazyMapBuilder.instance()
            .put("nested", nested)
            .put("tags", Arrays.asList("x", "y"))
            .build();

        Map<String, LazyJsonValue> m = v.getMap();
        assertNotNull(m);
        assertSame(nested, m.get("nested"));
        assertEquals(Integer.valueOf(1), m.get("nested").getMap().get("a").getInteger());

        List<LazyJsonValue> tags = m.get("tags").getArray();
        assertNotNull(tags);
        assertEquals(2, tags.size());
        assertEquals("x", tags.get(0).getString());
        assertEquals("y", tags.get(1).getString());
    }

    // ---- builder: nulls ----

    @Test
    public void testPutNullsFalse() {
        LazyJsonValue v = LazyMapBuilder.instance()
            .put("keep", "v")
            .put("drop", null)
            .build();
        Map<String, LazyJsonValue> m = v.getMap();
        assertNotNull(m);
        assertTrue(m.containsKey("keep"));
        assertFalse(m.containsKey("drop"));
    }

    @Test
    public void testPutNullsTrue() {
        LazyJsonValue v = LazyMapBuilder.instance(true)
            .put("keep", "v")
            .put("nul", null)
            .build();
        Map<String, LazyJsonValue> m = v.getMap();
        assertNotNull(m);
        assertTrue(m.containsKey("nul"));
        assertSame(LazyJsonValue.NULL, m.get("nul"));
    }

    // ---- builder: putEntries ----

    @Test
    public void testPutEntries() {
        Map<String, Object> src = new LinkedHashMap<>();
        src.put("a", 1);
        src.put("b", "two");
        LazyJsonValue v = LazyMapBuilder.instance().putEntries(src).build();
        Map<String, LazyJsonValue> m = v.getMap();
        assertNotNull(m);
        assertEquals(Integer.valueOf(1), m.get("a").getInteger());
        assertEquals("two", m.get("b").getString());
    }

    // ---- builder: insertion order preserved through toJson ----

    @Test
    public void testOrderPreserved() {
        String json = LazyMapBuilder.instance()
            .put("one", 1)
            .put("two", 2)
            .put("three", 3)
            .toJson();
        assertEquals("{\"one\":1,\"two\":2,\"three\":3}", json);
    }

    // ---- converter: from(JsonValue) ----

    @Test
    public void testFromJsonValue() {
        JsonValue src = MapBuilder.instance()
            .put("name", "n")
            .put("count", 5)
            .toJsonValue();
        LazyJsonValue v = LazyJsonParser.from(src);
        assertEquals(src, v);
        assertEquals(src.toJson(), v.toJson());
    }

    // ---- converter: from(JsonSerializable) ----

    @Test
    public void testFromJsonSerializable() {
        LazyJsonValue v = LazyJsonParser.from(new Sample("n", 5));
        Map<String, LazyJsonValue> m = v.getMap();
        assertNotNull(m);
        assertEquals("n", m.get("name").getString());
        assertEquals(Integer.valueOf(5), m.get("count").getInteger());
    }

    // ---- parity: built value matches the same JSON parsed ----

    @Test
    public void testParityWithParser() throws Exception {
        LazyJsonValue built = LazyMapBuilder.instance()
            .put("s", "hello")
            .put("i", 42)
            .put("l", 3000000000L)
            .put("nested", LazyMapBuilder.instance().put("a", 1).build())
            .put("tags", Arrays.asList("x", "y"))
            .build();

        LazyJsonValue parsed = LazyJsonParser.parse(built.toJson());
        // value-based equality (order-independent); the parser is HashMap-backed
        // and does not preserve key order, while the builder does (see testOrderPreserved).
        assertEquals(parsed, built);
        assertEquals(built, parsed);
    }

    // ---- empties ----

    @Test
    public void testEmpty() {
        LazyJsonValue v = LazyMapBuilder.instance().build();
        Map<String, LazyJsonValue> m = v.getMap();
        assertNotNull(m);
        assertTrue(m.isEmpty());
        assertEquals("{}", v.toJson());
    }

    // ---- array builder ----

    @Test
    public void testArrayBuilder() {
        LazyJsonValue nested = LazyMapBuilder.instance().put("a", 1).build();
        LazyJsonValue v = LazyArrayBuilder.instance()
            .add("a")
            .add(1)
            .add(true)
            .add(nested)
            .build();

        List<LazyJsonValue> a = v.getArray();
        assertNotNull(a);
        assertEquals(4, a.size());
        assertEquals("a", a.get(0).getString());
        assertEquals(Integer.valueOf(1), a.get(1).getInteger());
        assertEquals(Boolean.TRUE, a.get(2).getBoolean());
        assertSame(nested, a.get(3));
        assertEquals("[\"a\",1,true,{\"a\":1}]", v.toJson());
    }

    @Test
    public void testArrayBuilderAddItems() {
        LazyJsonValue v = LazyArrayBuilder.instance()
            .addItems(Arrays.asList("x", "y", "z"))
            .build();
        List<LazyJsonValue> a = v.getArray();
        assertNotNull(a);
        assertEquals(3, a.size());
        assertEquals("z", a.get(2).getString());
    }

    @Test
    public void testArrayBuilderNulls() {
        assertEquals(1, LazyArrayBuilder.instance().add("x").add(null).build().getArray().size());
        List<LazyJsonValue> withNulls = LazyArrayBuilder.instance(true).add("x").add(null).build().getArray();
        assertNotNull(withNulls);
        assertEquals(2, withNulls.size());
        assertSame(LazyJsonValue.NULL, withNulls.get(1));
    }

    @Test
    public void testArrayBuilderParity() throws Exception {
        LazyJsonValue built = LazyArrayBuilder.instance()
            .add("a")
            .add(3000000000L)
            .add(LazyMapBuilder.instance().put("a", 1).build())
            .build();
        LazyJsonValue parsed = LazyJsonParser.parse(built.toJson());
        assertEquals(parsed, built);
        assertEquals(built, parsed);
        assertEquals(parsed.toJson(), built.toJson());
    }

    @Test
    public void testArrayBuilderEmpty() {
        LazyJsonValue v = LazyArrayBuilder.instance().build();
        List<LazyJsonValue> a = v.getArray();
        assertNotNull(a);
        assertTrue(a.isEmpty());
        assertEquals("[]", v.toJson());
    }
}
