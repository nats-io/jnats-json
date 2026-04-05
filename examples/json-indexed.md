---
name: json-indexed
description: Convert a POJO to use the indexed parser (IndexedJsonValue / IndexedJsonValueUtils / JsonSerializable)
---

# Convert POJO to Indexed JsonSerializable

Convert the given Java POJO class to implement `io.nats.json.JsonSerializable` using the **indexed** parser (`IndexedJsonParser` / `IndexedJsonValue` / `IndexedJsonValueUtils`).

The indexed parser defers string copying and number parsing until accessor methods are called. The getters read from the parsed value on demand.

## Rules

1. The class must implement `JsonSerializable`.
2. Store the `IndexedJsonValue source` as a field — do NOT read fields in the constructor.
3. Add a constructor that takes a `String json` parameter and parses with `IndexedJsonParser.parse(json)`.
4. Add a constructor that takes an `IndexedJsonValue v` parameter and stores it.
5. Getters read from `source` on demand using `IndexedJsonValueUtils` methods. Data is only copied/parsed when the getter is called.
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

8. Import statics: `import static io.nats.json.IndexedJsonValueUtils.*;` and `import static io.nats.json.JsonWriteUtils.*;`
9. JSON field names should be snake_case.
10. For a nested object field:
    - Read with `readValue(source, "key")` to get the child `IndexedJsonValue`
    - Wrap in the nested class: `new NestedIndexed(pv)`
    - The nested class stores its own `IndexedJsonValue source` and has lazy getters
    - Write with `addField(sb, "key", getNested())` 
11. For a list of nested objects:
    - Read with `readArrayOrNull(source, "key")` to get `List<IndexedJsonValue>`
    - Loop and wrap each: `new NestedIndexed(ev)`
    - Write with `addJsons(sb, "key", getNestedList())`

## Template

```java
import io.nats.json.*;
import static io.nats.json.IndexedJsonValueUtils.*;
import static io.nats.json.JsonWriteUtils.*;

public class MyClass implements JsonSerializable {
    private final IndexedJsonValue source;

    public MyClass(String json) throws JsonParseException {
        this.source = IndexedJsonParser.parse(json);
    }

    public MyClass(IndexedJsonValue v) {
        this.source = v;
    }

    // Lazy getters -- data copied/parsed on first call
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

    // Nested object -- wraps on access
    public Nested getNested() {
        IndexedJsonValue pv = readValue(source, "nested");
        return pv == null ? null : new Nested(pv);
    }

    public static class Nested implements JsonSerializable {
        private final IndexedJsonValue source;
        public Nested(IndexedJsonValue v) { this.source = v; }
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

## Key Difference from Eager

In the eager version, fields are read once in the constructor and stored as Java fields.
In the indexed version, the parsed `IndexedJsonValue` is stored and fields are read on demand.
If you only call `getName()`, the other fields are never copied or parsed.
