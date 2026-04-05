# JNATS JSON Examples

This subproject shows how to convert a plain POJO into a `JsonSerializable` class
using each of the three parser implementations.

## The Original POJOs

[`ServerConfig.java`](src/main/java/io/nats/json/examples/ServerConfig.java) is a
standard Java POJO with getters, setters, and no JSON awareness. It includes nested
object fields ([`Placement`](src/main/java/io/nats/json/examples/Placement.java) and
a list of [`Endpoint`](src/main/java/io/nats/json/examples/Endpoint.java)) to demonstrate
how nesting is handled by each parser:

```java
public class ServerConfig {
    private String name;
    private String host;
    private int port;
    private long maxConnections;
    private boolean tlsEnabled;
    private Duration timeout;
    private List<String> tags;
    private Map<String, String> metadata;
    private Placement placement;         // nested object
    private List<Endpoint> endpoints;    // list of nested objects
    // ... getters and setters
}
```

## Converted Versions

### Eager: [`ServerConfigEager.java`](src/main/java/io/nats/json/examples/ServerConfigEager.java)

Uses `JsonParser` / `JsonValue` / `JsonValueUtils`. All fields are read eagerly in the
constructor and stored as `final` fields.

**When to use:** You need every field and want the simplest, most straightforward code.

**How to convert:**

1. Implement `JsonSerializable`
2. Make all fields `private final`
3. Remove setters
4. Add a `JsonValue` constructor that reads all fields using `JsonValueUtils.readXxx()` methods
5. Add a `String` constructor that parses with `JsonParser.parse(json)` and delegates
6. Add `toJson()` using `JsonWriteUtils.addField()` / `addStrings()` / `addFieldAsNanos()` etc.

```java
public class ServerConfigEager implements JsonSerializable {
    private final String name;
    private final PlacementEager placement;
    private final List<EndpointEager> endpoints;
    // ...

    public ServerConfigEager(JsonValue v) {
        this.name = readString(v, "name");
        // ... read scalar fields

        // Nested object: read as JsonValue, construct if present
        JsonValue pv = readValue(v, "placement");
        this.placement = pv == null ? null : new PlacementEager(pv);

        // List of nested objects: read array, construct each
        List<JsonValue> evs = readArrayOrNull(v, "endpoints");
        // ... loop and construct each EndpointEager
    }

    @Override
    public String toJson() {
        StringBuilder sb = beginJson();
        addField(sb, "name", name);
        addField(sb, "placement", placement);    // uses JsonSerializable.toJson()
        addJsons(sb, "endpoints", endpoints);    // each element is JsonSerializable
        return endJson(sb).toString();
    }

    // Nested classes also implement JsonSerializable
    public static class PlacementEager implements JsonSerializable { ... }
    public static class EndpointEager implements JsonSerializable { ... }
}
```

### Indexed: [`ServerConfigIndexed.java`](src/main/java/io/nats/json/examples/ServerConfigIndexed.java)

Uses `IndexedJsonParser` / `IndexedJsonValue` / `IndexedJsonValueUtils`. The parsed value
is stored and fields are read on demand through getters.

**When to use:** General-purpose upgrade over eager -- faster across the board with no
behavioral surprises.

**How to convert:**

1. Implement `JsonSerializable`
2. Store `IndexedJsonValue source` as the only field
3. Add constructors for `String` and `IndexedJsonValue`
4. Getters call `IndexedJsonValueUtils.readXxx(source, "key")` -- data is copied/parsed
   only when the getter is called
5. Add `toJson()` that calls getters and writes with `JsonWriteUtils`

```java
public class ServerConfigIndexed implements JsonSerializable {
    private final IndexedJsonValue source;

    public ServerConfigIndexed(String json) throws JsonParseException {
        this.source = IndexedJsonParser.parse(json);
    }

    public String getName() { return readString(source, "name"); }

    // Nested object: wrap on access
    public PlacementIndexed getPlacement() {
        IndexedJsonValue pv = readValue(source, "placement");
        return pv == null ? null : new PlacementIndexed(pv);
    }

    // List of nested: wrap each element on access
    public List<EndpointIndexed> getEndpoints() {
        List<IndexedJsonValue> evs = readArrayOrNull(source, "endpoints");
        // ... loop and wrap each
    }

    // Nested classes store their own IndexedJsonValue source
    public static class PlacementIndexed implements JsonSerializable { ... }
    public static class EndpointIndexed implements JsonSerializable { ... }
}
```

### Lazy: [`ServerConfigLazy.java`](src/main/java/io/nats/json/examples/ServerConfigLazy.java)

Uses `LazyJsonParser` / `LazyJsonValue` / `LazyJsonValueUtils`. Nested objects and arrays
are not even parsed until accessed.

**When to use:** Large JSON responses where you only read a subset of fields. Nested
structures you never access are never parsed at all.

**How to convert:**

Same pattern as indexed, but with `LazyJsonParser` / `LazyJsonValue` / `LazyJsonValueUtils`:

```java
public class ServerConfigLazy implements JsonSerializable {
    private final LazyJsonValue source;

    public ServerConfigLazy(String json) throws JsonParseException {
        this.source = LazyJsonParser.parse(json);
    }

    public String getName() { return readString(source, "name"); }

    // Nested object: skip-scanned during parse, only parsed on access
    public PlacementLazy getPlacement() {
        LazyJsonValue pv = readValue(source, "placement");
        return pv == null ? null : new PlacementLazy(pv);
    }

    // List of nested: array was skip-scanned, parsed one level on access.
    // Each element object is itself still unresolved until its getters are called.
    public List<EndpointLazy> getEndpoints() {
        List<LazyJsonValue> evs = readArrayOrNull(source, "endpoints");
        // ... loop and wrap each
    }

    public static class PlacementLazy implements JsonSerializable { ... }
    public static class EndpointLazy implements JsonSerializable { ... }
}
```

## Claude Code Skills

Three Claude Code skills are provided in this directory to automate the conversion:

| Skill | Command | Description |
|---|---|---|
| `json-eager` | `/json-eager` | Convert POJO using eager parser |
| `json-indexed` | `/json-indexed` | Convert POJO using indexed parser |
| `json-lazy` | `/json-lazy` | Convert POJO using lazy parser |

To use a skill, open the POJO file in Claude Code and run the slash command.
Claude will rewrite the class following the patterns shown in the examples above.

## Field Type Mapping

| Java Type | JSON Key Style | Read Method | Write Method |
|---|---|---|---|
| `String` | `"name"` | `readString(v, "name")` | `addField(sb, "name", name)` |
| `int` | `"port"` | `readInteger(v, "port", 0)` | `addField(sb, "port", port)` |
| `long` | `"max_connections"` | `readLong(v, "max_connections", -1)` | `addField(sb, "max_connections", max)` |
| `boolean` | `"tls_enabled"` | `readBoolean(v, "tls_enabled", false)` | `addField(sb, "tls_enabled", tls)` |
| `Duration` | `"timeout"` (nanos) | `readNanosAsDuration(v, "timeout")` | `addFieldAsNanos(sb, "timeout", dur)` |
| `ZonedDateTime` | `"created"` | `readDate(v, "created")` | `addField(sb, "created", zdt)` |
| `List<String>` | `"tags"` | `readStringListOrEmpty(v, "tags")` | `addStrings(sb, "tags", tags)` |
| `Map<String, String>` | `"metadata"` | `readStringMapOrNull(v, "metadata")` | `addField(sb, "metadata", meta)` |
| Nested object | `"placement"` | `readValue(v, "placement")` then wrap | `addField(sb, "placement", obj)` |
| `List<Nested>` | `"endpoints"` | `readArrayOrNull(v, "endpoints")` then wrap each | `addJsons(sb, "endpoints", list)` |

The `readXxx` methods come from the `*ValueUtils` class matching the parser you chose.
The `addXxx` methods always come from `JsonWriteUtils` (shared by all three).
Nested classes should also implement `JsonSerializable` following the same pattern.
