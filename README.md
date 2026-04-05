![NATS](src/main/javadoc/images/large-logo.png)

# JNATS JSON

This library is a JSON Parser built specifically for JNATS to avoid a 3rd party library dependency.

![3.0.7](https://img.shields.io/badge/Current_Release-3.0.7-27AAE0?style=for-the-badge)
![3.0.8](https://img.shields.io/badge/Current_Snapshot-3.0.8--SNAPSHOT-27AAE0?style=for-the-badge)

[![Build Main Badge](https://github.com/nats-io/jnats-json/actions/workflows/build-main.yml/badge.svg?event=push)](https://github.com/nats-io/jnats-json/actions/workflows/build-main.yml)
[![Coverage Status](https://coveralls.io/repos/github/nats-io/jnats-json/badge?branch=main)](https://coveralls.io/github/nats-io/jnats-json?branch=main)
[![Javadoc](http://javadoc.io/badge/io.nats/jnats-json.svg?branch=main)](http://javadoc.io/doc/io.nats/jnats-json?branch=main)
[![License Apache 2](https://img.shields.io/badge/License-Apache2-blue)](https://www.apache.org/licenses/LICENSE-2.0)

### IMPORTANT

Until the minor version reaches 1, the api and behavior is subject to change.

### JDK Version

This project uses Java 8 Language Level api, but builds jars compiled with and targeted for Java 8, 17, 21 and 25.
It creates different artifacts for each. All have the same group id `io.nats` and the same version but have different artifact names. 

| Java Target Level | Artifact Id        |                                                                     Maven Central                                                                      |
|:-----------------:|--------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------:|
|        1.8        | `jnats-json`       |   [![Maven JDK 1_8](https://img.shields.io/maven-central/v/io.nats/jnats-json-jdk17?label=)](https://mvnrepository.com/artifact/io.nats/jnats-json)    |
|        17         | `jnats-json-jdk17` | [![Maven JDK 17](https://img.shields.io/maven-central/v/io.nats/jnats-json-jdk17?label=)](https://mvnrepository.com/artifact/io.nats/jnats-json-jdk17) |
|        21         | `jnats-json-jdk21` | [![Maven JDK 21](https://img.shields.io/maven-central/v/io.nats/jnats-json-jdk21?label=)](https://mvnrepository.com/artifact/io.nats/jnats-json-jdk21) |
|        25         | `jnats-json-jdk25` | [![Maven JDK 25](https://img.shields.io/maven-central/v/io.nats/jnats-json-jdk25?label=)](https://mvnrepository.com/artifact/io.nats/jnats-json-jdk25) |

### Dependency Management

The NATS client is available in the Maven central repository, 
and can be imported as a standard dependency in your `build.gradle` or `pom.xml` file,
The examples shown use the Jdk 8 version. To use other versions, change the artifact id. 

#### Gradle

```groovy
dependencies {
    implementation 'io.nats:jnats-json:3.0.7'
}
```

If you need the latest and greatest before Maven central updates, you can use:

```groovy
repositories {
    mavenCentral()
    maven {
        url "https://repo1.maven.org/maven2/"
    }
}
```

If you need a snapshot version, you must add the url for the snapshots and change your dependency.

```groovy
repositories {
    mavenCentral()
    maven {
        url "https://central.sonatype.com/repository/maven-snapshots"
    }
}

dependencies {
   implementation 'io.nats:jnats-json:3.0.8-SNAPSHOT'
}
```

#### Maven

```xml
<dependency>
    <groupId>io.nats</groupId>
    <artifactId>jnats-json</artifactId>
    <version>3.0.7</version>
</dependency>
```

If you need the absolute latest, before it propagates to maven central, you can use the repository:

```xml
<repositories>
    <repository>
        <id>sonatype releases</id>
        <url>https://repo1.maven.org/maven2/</url>
        <releases>
           <enabled>true</enabled>
        </releases>
    </repository>
</repositories>
```

If you need a snapshot version, you must enable snapshots and change your dependency.

```xml
<repositories>
    <repository>
        <id>sonatype snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>io.nats</groupId>
    <artifactId>jnats-json</artifactId>
    <version>3.0.8-SNAPSHOT</version>
</dependency>
```

## Implementations

The library provides three JSON parser implementations with increasing levels of laziness.
All three share the same `JsonParser.Option` enum for configuration:

- `KEEP_NULLS` -- retain null values in maps (normally filtered out)
- `DECIMALS` -- enable decimal/floating-point number support (BigDecimal, Double, etc.)

By default, all parsers assume integers only, which is appropriate for NATS protocol messages
where numbers are always integers (sequence numbers, byte counts, timestamps in nanoseconds, etc.).

### JsonParser / JsonValue / JsonValueUtils (Eager)

The original parser. Parses the entire JSON document eagerly -- all strings are copied,
all numbers are parsed, and the full tree of HashMaps and ArrayLists is built during the
`parse()` call.

```java
JsonValue v = JsonParser.parse(json);
String name = JsonValueUtils.readString(v, "name");
```

Best when you need every field in the document and want the simplest API.
`JsonValue` exposes all typed fields directly as public final fields (`string`, `i`, `l`, `map`, `array`, etc.).

### IndexedJsonParser / IndexedJsonValue / IndexedJsonValueUtils (Indexed)

Defers leaf materialization. During parsing, strings and numbers are stored as offset ranges
into the source `char[]` rather than being copied or parsed. The full tree structure (HashMaps,
ArrayLists) is still built eagerly. Data is only copied/parsed when accessor methods are called
(`getString()`, `getInteger()`, `getLong()`, etc.), and results are cached after first access.

```java
IndexedJsonValue v = IndexedJsonParser.parse(json);
String name = IndexedJsonValueUtils.readString(v, "name");
```

In integers-only mode (the default), numbers are parsed directly from the `char[]` to `long`
with no intermediate `String` allocation.

Best as a general-purpose upgrade over the eager parser -- faster across the board with no
behavioral surprises.

### LazyJsonParser / LazyJsonValue / LazyJsonValueUtils (Lazy)

Defers everything. In addition to lazy leaf materialization, nested objects (`{...}`) and
arrays (`[...]`) are not parsed during the initial `parse()` call. Instead, a brace/bracket-counting
skip scan records their byte-range offsets. Children are parsed one level deep on demand when
`getMap()` or `getArray()` is first called.

```java
LazyJsonValue v = LazyJsonParser.parse(json);
String name = LazyJsonValueUtils.readString(v, "name");
// The "cluster", "state", "sources" subtrees were never parsed
```

The initial `parse()` call shallow-parses only the outermost container. Nested containers
within that are unresolved offset ranges until accessed. This means parsing cost is proportional
to what you actually read, not the total document size.

Trade-offs vs the indexed parser:
- Skipped regions are not validated until accessed. Malformed JSON inside a nested object you never
  read will not produce an error.
- On-demand resolution has a small per-access overhead. For small flat documents where you read
  every field, the lazy parser can be marginally slower than the indexed parser.

Best when parsing large responses where you only access a subset of the top-level fields
(e.g., reading `config` from a StreamInfo response while ignoring `cluster`, `state`, `sources`).

### Comparison

Benchmarks were run parsing real NATS JSON payloads (StreamInfo and ConsumerInfo) in both
prettified and compacted (no whitespace) form. Compacted JSON matches what the NATS server
actually sends on the wire. All numbers are operations per second. Ratios are relative to
the eager parser.

**StreamInfo** (has nested `cluster`, `state`, `mirror`, `sources`, `alternates`):

| Scenario | | Eager | Indexed | Lazy |
|---|---|---|---|---|
| Parse only | pretty (3,313 chars) | 74K | 96K (1.29x) | **190K (2.56x)** |
| | compact (2,161 chars) | 87K | 124K (1.42x) | **373K (4.27x)** |
| 10% config fields | pretty | 82K | 112K (1.38x) | **163K (2.00x)** |
| | compact | 87K | 124K (1.42x) | **288K (3.30x)** |
| 25% config fields | pretty | 83K | 117K (1.40x) | **159K (1.91x)** |
| | compact | 90K | 132K (1.47x) | **296K (3.29x)** |
| 50% config fields | pretty | 82K | 113K (1.37x) | **148K (1.80x)** |
| | compact | 92K | 127K (1.38x) | **261K (2.84x)** |
| 100% config fields | pretty | 73K | 113K (1.55x) | **143K (1.98x)** |
| | compact | 84K | 120K (1.43x) | **250K (2.96x)** |

**ConsumerInfo** (has nested `cluster`, `delivered`, `ack_floor`):

| Scenario | | Eager | Indexed | Lazy |
|---|---|---|---|---|
| Parse only | pretty (2,551 chars) | 109K | 158K (1.45x) | **251K (2.31x)** |
| | compact (1,907 chars) | 116K | 165K (1.42x) | **400K (3.44x)** |
| 10% config fields | pretty | 116K | 160K (1.39x) | 169K (1.46x) |
| | compact | 118K | 168K (1.42x) | **217K (1.84x)** |
| 25% config fields | pretty | 112K | 162K (1.45x) | 166K (1.48x) |
| | compact | 115K | 167K (1.45x) | **206K (1.79x)** |
| 50% config fields | pretty | 98K | 133K (1.37x) | 136K (1.39x) |
| | compact | 105K | 143K (1.37x) | **176K (1.68x)** |
| 100% config fields | pretty | 90K | 116K (1.28x) | 109K (1.20x) |
| | compact | 96K | 122K (1.28x) | **136K (1.42x)** |

**Minimal StreamInfo** (flat config, no nested objects to skip):

| Scenario | | Eager | Indexed | Lazy |
|---|---|---|---|---|
| Parse only | pretty (338 chars) | 763K | 1,055K (1.38x) | **1,295K (1.70x)** |
| | compact (250 chars) | 810K | 1,100K (1.36x) | **1,516K (1.87x)** |
| Name only | pretty | 818K | 983K (1.20x) | **1,290K (1.58x)** |
| | compact | 816K | 1,043K (1.28x) | **1,650K (2.02x)** |
| All fields | pretty | 710K | **853K (1.20x)** | 750K (1.06x) |
| | compact | 717K | **937K (1.31x)** | 876K (1.22x) |

**Minimal ConsumerInfo** (flat config):

| Scenario | | Eager | Indexed | Lazy |
|---|---|---|---|---|
| Parse only | pretty (360 chars) | 665K | 951K (1.43x) | **1,106K (1.66x)** |
| | compact (283 chars) | 805K | 1,104K (1.37x) | **1,385K (1.72x)** |
| Name only | pretty | 763K | 1,071K (1.40x) | **1,151K (1.51x)** |
| | compact | 769K | 996K (1.30x) | **1,371K (1.78x)** |
| All fields | pretty | 738K | **875K (1.18x)** | 812K (1.10x) |
| | compact | 800K | **937K (1.17x)** | 860K (1.07x) |

**Summary:**

- **Compact JSON dramatically amplifies the lazy parser's advantage.** The skip scan
  flies through dense compacted JSON. For StreamInfo parse-only: pretty=2.56x, compact=**4.27x**.
  Even reading 100% of config fields: pretty=1.98x, compact=**2.96x**. This matters because
  the NATS server always sends compact JSON.
- **Lazy** dominates when nested structures go untouched. The sibling subtrees (`cluster`,
  `state`, `sources`, etc.) are skipped entirely regardless of how many config fields you read.
- **Indexed** is the reliable all-rounder at **1.2-1.5x** faster than eager across the board.
  It never underperforms and wins on small flat documents where every field is read.
- For **flat minimal JSON reading all fields**, indexed edges out lazy (1.31x vs 1.22x compact)
  because lazy's on-demand resolution overhead exceeds skip savings when there is nothing to skip.
  Both are well above baseline.
- **Integers-only mode** (the default) provides a small but consistent additional speedup
  over DECIMALS mode by skipping decimal-indicator scanning and using direct `char[]`-to-`long`
  parsing with no `String` allocation.

## Examples

The [examples](examples/) subproject demonstrates how to convert a plain POJO into a
`JsonSerializable` class for each parser implementation, including handling of nested
objects and lists of nested objects. See the [examples README](examples/README.md) for
full details, code samples, and a field type mapping reference.

## License

Unless otherwise noted, the NATS source files are distributed
under the Apache Version 2.0 license found in the LICENSE file.
