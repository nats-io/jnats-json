---
name: json-lazy
description: Convert a POJO to use the lazy parser (LazyJsonValue / LazyJsonValueUtils / JsonSerializable)
---

# Convert POJO to Lazy JsonSerializable

Convert the given Java POJO class to implement `io.nats.json.JsonSerializable` using the **lazy** parser (`LazyJsonParser` / `LazyJsonValue` / `LazyJsonValueUtils`).

The lazy parser defers everything: strings, numbers, AND nested objects/arrays. Nested structures are not parsed until accessed.

## Rules

1. The class must implement `JsonSerializable`.
2. Store the `LazyJsonValue source` as a field — do NOT read fields in the constructor.
3. Add a constructor that takes a `String json` parameter and parses with `LazyJsonParser.parse(json)`.
4. Add a constructor that takes a `LazyJsonValue v` parameter and stores it.
5. Getters read from `source` on demand using `LazyJsonValueUtils` methods.
6. Add a `toJson()` method that calls the getters and serializes using `JsonWriteUtils`.
7. Use the correct read method for each field type:

| Java Type | Getter Body | Write Method |
|---|---|---|
| `String` | `readString(source, "key")` | `addField(sb, "key", getName())` |
| `int` | `readInteger(source, "key", default)` | `addField(sb, "key", getPort())` |
| `long` | `readLong(source, "key", default)` | `addField(sb, "key", getMax())` |
| `boolean` | `readBoolean(source, "key", default)` | `addField(sb, "key", isEnabled())` |
| `Duration` (nanos) | `readNanosAsDuration(source, "key")` | `addFieldAsNanos(sb, "key", getTimeout())` |
| `ZonedDateTime` | `readDate(source, "key")` | `addField(sb, "key", getCreated())` |
| `List<String>` | `readStringListOrEmpty(source, "key")` | `addStrings(sb, "key", getTags())` |
| `Map<String, String>` | `readStringMapOrNull(source, "key")` | `addField(sb, "key", getMetadata())` |
| Nested object | `readValue(source, "key")` then wrap | `addField(sb, "key", getNested())` |

8. Import statics: `import static io.nats.json.LazyJsonValueUtils.*;` and `import static io.nats.json.JsonWriteUtils.*;`
9. JSON field names should be snake_case.
10. For a nested object field:
    - Read with `readValue(source, "key")` to get the child `LazyJsonValue`
    - The child was skip-scanned during the initial parse -- it is only parsed when accessed
    - Wrap in the nested class: `new NestedLazy(pv)`
    - The nested class stores its own `LazyJsonValue source` and has lazy getters
    - Write with `addField(sb, "key", getNested())`
11. For a list of nested objects:
    - Read with `readArrayOrNull(source, "key")` to get `List<LazyJsonValue>`
    - The array was skip-scanned; calling `readArrayOrNull` triggers one-level parse
    - Each element that is an object is itself still an unresolved offset range
    - Loop and wrap each: `new NestedLazy(ev)`
    - Write with `addJsons(sb, "key", getNestedList())`

## Template

```java
import io.nats.json.*;
import static io.nats.json.LazyJsonValueUtils.*;
import static io.nats.json.JsonWriteUtils.*;

public class MyClass implements JsonSerializable {
    private final LazyJsonValue source;

    public MyClass(String json) throws JsonParseException {
        this.source = LazyJsonParser.parse(json);
    }

    public MyClass(LazyJsonValue v) {
        this.source = v;
    }

    // Lazy getters -- nothing parsed until called
    public String getName() { return readString(source, "name"); }
    public int getCount() { return readInteger(source, "count", 0); }
    // ... all getters

    @Override
    public String toJson() {
        StringBuilder sb = beginJson();
        addField(sb, "name", getName());
        addField(sb, "count", getCount());
        // ... write all fields
        return endJson(sb).toString();
    }
}
```

    // Nested object -- skip-scanned during parse, only parsed on access
    public Nested getNested() {
        LazyJsonValue pv = readValue(source, "nested");
        return pv == null ? null : new Nested(pv);
    }

    public static class Nested implements JsonSerializable {
        private final LazyJsonValue source;
        public Nested(LazyJsonValue v) { this.source = v; }
        public String getValue() { return readString(source, "value"); }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            addField(sb, "value", getValue());
            return endJson(sb).toString();
        }
    }
}
```

## Key Difference from Indexed

The indexed parser builds the full HashMap/ArrayList tree during parse but defers leaf values.
The lazy parser also defers nested objects and arrays — they are skip-scanned and only parsed
when `getMap()` or `getArray()` is called on them. This makes the lazy parser fastest when
large sibling subtrees are never accessed (e.g., parsing a StreamInfo response but only
reading the `config` section while `cluster`, `state`, `sources` are skipped entirely).

For flat objects with no nesting, the lazy and indexed parsers behave similarly.
