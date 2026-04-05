---
name: json-eager
description: Convert a POJO to use the eager JsonParser (JsonValue / JsonValueUtils / JsonSerializable)
---

# Convert POJO to Eager JsonSerializable

Convert the given Java POJO class to implement `io.nats.json.JsonSerializable` using the **eager** parser (`JsonParser` / `JsonValue` / `JsonValueUtils`).

## Rules

1. The class must implement `JsonSerializable`.
2. Make all fields `private final` and read them in the constructor.
3. Add a constructor that takes a `String json` parameter, parses with `JsonParser.parse(json)`, and delegates to a `JsonValue` constructor.
4. Add a constructor that takes a `JsonValue v` parameter and reads every field from it.
5. Add a `toJson()` method that serializes all fields using `JsonWriteUtils`.
6. Keep only getters (no setters — the object is immutable after construction).
7. Use the correct read method for each field type:

| Java Type | Read Method | Write Method |
|---|---|---|
| `String` | `readString(v, "key")` | `addField(sb, "key", value)` |
| `int` | `readInteger(v, "key", default)` | `addField(sb, "key", value)` |
| `long` | `readLong(v, "key", default)` | `addField(sb, "key", value)` |
| `boolean` | `readBoolean(v, "key", default)` | `addField(sb, "key", value)` |
| `Duration` (nanos) | `readNanosAsDuration(v, "key")` | `addFieldAsNanos(sb, "key", value)` |
| `ZonedDateTime` | `readDate(v, "key")` | `addField(sb, "key", value)` |
| `List<String>` | `readStringListOrEmpty(v, "key")` | `addStrings(sb, "key", value)` |
| `Map<String, String>` | `readStringMapOrNull(v, "key")` | `addField(sb, "key", value)` |
| `byte[]` (base64) | `readBase64Basic(v, "key")` | use `Encoding.base64BasicEncode` + `addField` |
| Nested object | `readValue(v, "key")` then pass to nested constructor | `addField(sb, "key", nestedSerializable)` |

8. Import statics: `import static io.nats.json.JsonValueUtils.*;` and `import static io.nats.json.JsonWriteUtils.*;`
9. JSON field names should be snake_case. Map Java camelCase field names to snake_case keys.
10. For a nested object field:
    - Read with `readValue(v, "key")` to get the child `JsonValue`
    - Pass it to the nested class constructor: `new NestedEager(pv)`
    - The nested class should also implement `JsonSerializable` with the same pattern
    - Write with `addField(sb, "key", nestedObj)` (uses `JsonSerializable.toJson()`)
11. For a list of nested objects:
    - Read with `readArrayOrNull(v, "key")` to get `List<JsonValue>`
    - Loop and construct each: `new NestedEager(ev)`
    - Write with `addJsons(sb, "key", list)` (each element implements `JsonSerializable`)

## Template

```java
import io.nats.json.*;
import static io.nats.json.JsonValueUtils.*;
import static io.nats.json.JsonWriteUtils.*;

public class MyClass implements JsonSerializable {
    private final String name;
    private final int count;
    // ... all fields

    public MyClass(String json) throws JsonParseException {
        this(JsonParser.parse(json));
    }

    public MyClass(JsonValue v) {
        this.name = readString(v, "name");
        this.count = readInteger(v, "count", 0);
        // ... read all fields
    }

    @Override
    public String toJson() {
        StringBuilder sb = beginJson();
        addField(sb, "name", name);
        addField(sb, "count", count);
        // ... write all fields
        return endJson(sb).toString();
    }

    public String getName() { return name; }
    public int getCount() { return count; }
    // ... getters only

    // Nested object as static inner class
    public static class Nested implements JsonSerializable {
        private final String value;

        public Nested(JsonValue v) {
            this.value = readString(v, "value");
        }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            addField(sb, "value", value);
            return endJson(sb).toString();
        }

        public String getValue() { return value; }
    }
}
```
