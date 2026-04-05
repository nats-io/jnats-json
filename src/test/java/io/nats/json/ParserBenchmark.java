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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Benchmark comparing JsonParser (eager), IndexedJsonParser (indexed), and
 * LazyJsonParser (lazy) for realistic workloads modeled after StreamFields
 * and ConsumerFields parsing.
 * <p>
 * Tagged "benchmark" and excluded from CI/CD. Run manually with:
 * <pre>gradle test --tests "*ParserBenchmark*"</pre>
 */
@Tag("benchmark")
public final class ParserBenchmark {

    // Warm-up and measurement parameters
    private static final int WARMUP_ITERATIONS = 5_000;
    private static final int MEASURE_ITERATIONS = 100_000;

    // JSON payloads: both pretty (as stored on disk) and compact (as sent by NATS server)
    private static final String STREAM_PRETTY;
    private static final String CONSUMER_PRETTY;
    private static final String STREAM_MIN_PRETTY;
    private static final String CONSUMER_MIN_PRETTY;
    private static final String STREAM_COMPACT;
    private static final String CONSUMER_COMPACT;
    private static final String STREAM_MIN_COMPACT;
    private static final String CONSUMER_MIN_COMPACT;

    static {
        STREAM_PRETTY = ResourceUtils.resourceAsString("stream_info.json");
        CONSUMER_PRETTY = ResourceUtils.resourceAsString("consumer_info.json");
        STREAM_MIN_PRETTY = ResourceUtils.resourceAsString("stream_info_minimal.json");
        CONSUMER_MIN_PRETTY = ResourceUtils.resourceAsString("consumer_info_minimal.json");
        STREAM_COMPACT = compact(STREAM_PRETTY);
        CONSUMER_COMPACT = compact(CONSUMER_PRETTY);
        STREAM_MIN_COMPACT = compact(STREAM_MIN_PRETTY);
        CONSUMER_MIN_COMPACT = compact(CONSUMER_MIN_PRETTY);
    }

    private static String compact(String json) {
        return JsonParser.parseUnchecked(json, JsonParser.Option.DECIMALS).toJson();
    }

    // -----------------------------------------------------------------------
    // StreamFields read methods - modeled directly from StreamFields.java
    // reading from the "config" sub-object, same keys and types
    // -----------------------------------------------------------------------

    /** Read ~10% of stream config fields: just name and storage type */
    private static void readStreamEager10(JsonValue config) {
        JsonValueUtils.readString(config, "name");
        JsonValueUtils.readString(config, "storage");
        JsonValueUtils.readInteger(config, "num_replicas", 1);
    }

    private static void readStreamIndexed10(IndexedJsonValue config) {
        IndexedJsonValueUtils.readString(config, "name");
        IndexedJsonValueUtils.readString(config, "storage");
        IndexedJsonValueUtils.readInteger(config, "num_replicas", 1);
    }

    /** Read ~25% of stream config fields */
    private static void readStreamEager25(JsonValue config) {
        readStreamEager10(config);
        JsonValueUtils.readString(config, "retention");
        JsonValueUtils.readString(config, "discard");
        JsonValueUtils.readLong(config, "max_consumers", -1);
        JsonValueUtils.readLong(config, "max_msgs", -1);
        JsonValueUtils.readLong(config, "max_bytes", -1);
    }

    private static void readStreamIndexed25(IndexedJsonValue config) {
        readStreamIndexed10(config);
        IndexedJsonValueUtils.readString(config, "retention");
        IndexedJsonValueUtils.readString(config, "discard");
        IndexedJsonValueUtils.readLong(config, "max_consumers", -1);
        IndexedJsonValueUtils.readLong(config, "max_msgs", -1);
        IndexedJsonValueUtils.readLong(config, "max_bytes", -1);
    }

    /** Read ~50% of stream config fields */
    private static void readStreamEager50(JsonValue config) {
        readStreamEager25(config);
        JsonValueUtils.readLong(config, "max_msgs_per_sub", -1);
        JsonValueUtils.readNanosAsDuration(config, "max_age");
        JsonValueUtils.readInteger(config, "max_msg_size", -1);
        JsonValueUtils.readNanosAsDuration(config, "duplicate_window");
        JsonValueUtils.readStringListOrEmpty(config, "subjects");
        JsonValueUtils.readLong(config, "first_seq", 1);
    }

    private static void readStreamIndexed50(IndexedJsonValue config) {
        readStreamIndexed25(config);
        IndexedJsonValueUtils.readLong(config, "max_msgs_per_sub", -1);
        IndexedJsonValueUtils.readNanosAsDuration(config, "max_age");
        IndexedJsonValueUtils.readInteger(config, "max_msg_size", -1);
        IndexedJsonValueUtils.readNanosAsDuration(config, "duplicate_window");
        IndexedJsonValueUtils.readStringListOrEmpty(config, "subjects");
        IndexedJsonValueUtils.readLong(config, "first_seq", 1);
    }

    /** Read ~75% of stream config fields */
    private static void readStreamEager75(JsonValue config) {
        readStreamEager50(config);
        JsonValueUtils.readBoolean(config, "no_ack", false);
        JsonValueUtils.readString(config, "template_owner");
        JsonValueUtils.readString(config, "description");
        JsonValueUtils.readBoolean(config, "sealed", false);
        JsonValueUtils.readBoolean(config, "allow_rollup_hdrs", false);
        JsonValueUtils.readBoolean(config, "allow_direct", false);
        JsonValueUtils.readBoolean(config, "mirror_direct", false);
        JsonValueUtils.readBoolean(config, "deny_delete", false);
        JsonValueUtils.readBoolean(config, "deny_purge", false);
    }

    private static void readStreamIndexed75(IndexedJsonValue config) {
        readStreamIndexed50(config);
        IndexedJsonValueUtils.readBoolean(config, "no_ack", false);
        IndexedJsonValueUtils.readString(config, "template_owner");
        IndexedJsonValueUtils.readString(config, "description");
        IndexedJsonValueUtils.readBoolean(config, "sealed", false);
        IndexedJsonValueUtils.readBoolean(config, "allow_rollup_hdrs", false);
        IndexedJsonValueUtils.readBoolean(config, "allow_direct", false);
        IndexedJsonValueUtils.readBoolean(config, "mirror_direct", false);
        IndexedJsonValueUtils.readBoolean(config, "deny_delete", false);
        IndexedJsonValueUtils.readBoolean(config, "deny_purge", false);
    }

    /** Read 100% of stream config fields */
    private static void readStreamEager100(JsonValue config) {
        readStreamEager75(config);
        JsonValueUtils.readBoolean(config, "discard_new_per_subject", false);
        JsonValueUtils.readStringMapOrNull(config, "metadata");
        JsonValueUtils.readBoolean(config, "allow_msg_ttl", false);
        JsonValueUtils.readBoolean(config, "allow_msg_schedules", false);
        JsonValueUtils.readBoolean(config, "allow_msg_counter", false);
        JsonValueUtils.readBoolean(config, "allow_atomic", false);
        JsonValueUtils.readString(config, "persist_mode");
        JsonValueUtils.readString(config, "compression");
        JsonValueUtils.readNanosAsDuration(config, "subject_delete_marker_ttl");
        // nested objects
        JsonValueUtils.readValue(config, "placement");
        JsonValueUtils.readValue(config, "republish");
        JsonValueUtils.readValue(config, "subject_transform");
        JsonValueUtils.readValue(config, "consumer_limits");
        JsonValueUtils.readValue(config, "mirror");
        JsonValueUtils.readValue(config, "sources");
    }

    private static void readStreamIndexed100(IndexedJsonValue config) {
        readStreamIndexed75(config);
        IndexedJsonValueUtils.readBoolean(config, "discard_new_per_subject", false);
        IndexedJsonValueUtils.readStringMapOrNull(config, "metadata");
        IndexedJsonValueUtils.readBoolean(config, "allow_msg_ttl", false);
        IndexedJsonValueUtils.readBoolean(config, "allow_msg_schedules", false);
        IndexedJsonValueUtils.readBoolean(config, "allow_msg_counter", false);
        IndexedJsonValueUtils.readBoolean(config, "allow_atomic", false);
        IndexedJsonValueUtils.readString(config, "persist_mode");
        IndexedJsonValueUtils.readString(config, "compression");
        IndexedJsonValueUtils.readNanosAsDuration(config, "subject_delete_marker_ttl");
        // nested objects
        IndexedJsonValueUtils.readValue(config, "placement");
        IndexedJsonValueUtils.readValue(config, "republish");
        IndexedJsonValueUtils.readValue(config, "subject_transform");
        IndexedJsonValueUtils.readValue(config, "consumer_limits");
        IndexedJsonValueUtils.readValue(config, "mirror");
        IndexedJsonValueUtils.readValue(config, "sources");
    }

    // -----------------------------------------------------------------------
    // ConsumerFields read methods - modeled directly from ConsumerFields.java
    // reading from the "config" sub-object
    // -----------------------------------------------------------------------

    /** Read ~10% of consumer config fields */
    private static void readConsumerEager10(JsonValue config) {
        JsonValueUtils.readString(config, "name");
        JsonValueUtils.readString(config, "durable_name");
        JsonValueUtils.readString(config, "deliver_policy");
    }

    private static void readConsumerIndexed10(IndexedJsonValue config) {
        IndexedJsonValueUtils.readString(config, "name");
        IndexedJsonValueUtils.readString(config, "durable_name");
        IndexedJsonValueUtils.readString(config, "deliver_policy");
    }

    /** Read ~25% of consumer config fields */
    private static void readConsumerEager25(JsonValue config) {
        readConsumerEager10(config);
        JsonValueUtils.readString(config, "ack_policy");
        JsonValueUtils.readString(config, "replay_policy");
        JsonValueUtils.readString(config, "description");
        JsonValueUtils.readString(config, "deliver_subject");
        JsonValueUtils.readString(config, "deliver_group");
        JsonValueUtils.readLong(config, "max_deliver", -1);
    }

    private static void readConsumerIndexed25(IndexedJsonValue config) {
        readConsumerIndexed10(config);
        IndexedJsonValueUtils.readString(config, "ack_policy");
        IndexedJsonValueUtils.readString(config, "replay_policy");
        IndexedJsonValueUtils.readString(config, "description");
        IndexedJsonValueUtils.readString(config, "deliver_subject");
        IndexedJsonValueUtils.readString(config, "deliver_group");
        IndexedJsonValueUtils.readLong(config, "max_deliver", -1);
    }

    /** Read ~50% of consumer config fields */
    private static void readConsumerEager50(JsonValue config) {
        readConsumerEager25(config);
        JsonValueUtils.readString(config, "sample_freq");
        JsonValueUtils.readDate(config, "opt_start_time");
        JsonValueUtils.readNanosAsDuration(config, "ack_wait");
        JsonValueUtils.readNanosAsDuration(config, "max_expires");
        JsonValueUtils.readNanosAsDuration(config, "inactive_threshold");
        JsonValueUtils.readLong(config, "opt_start_seq", -1);
        JsonValueUtils.readLong(config, "rate_limit_bps", -1);
        JsonValueUtils.readLong(config, "max_ack_pending", -1);
        JsonValueUtils.readLong(config, "max_waiting", -1);
    }

    private static void readConsumerIndexed50(IndexedJsonValue config) {
        readConsumerIndexed25(config);
        IndexedJsonValueUtils.readString(config, "sample_freq");
        IndexedJsonValueUtils.readDate(config, "opt_start_time");
        IndexedJsonValueUtils.readNanosAsDuration(config, "ack_wait");
        IndexedJsonValueUtils.readNanosAsDuration(config, "max_expires");
        IndexedJsonValueUtils.readNanosAsDuration(config, "inactive_threshold");
        IndexedJsonValueUtils.readLong(config, "opt_start_seq", -1);
        IndexedJsonValueUtils.readLong(config, "rate_limit_bps", -1);
        IndexedJsonValueUtils.readLong(config, "max_ack_pending", -1);
        IndexedJsonValueUtils.readLong(config, "max_waiting", -1);
    }

    /** Read ~75% of consumer config fields */
    private static void readConsumerEager75(JsonValue config) {
        readConsumerEager50(config);
        JsonValueUtils.readLong(config, "max_batch", -1);
        JsonValueUtils.readLong(config, "max_bytes", -1);
        JsonValueUtils.readInteger(config, "num_replicas", 0);
        JsonValueUtils.readDate(config, "pause_until");
        JsonValueUtils.readNanosAsDuration(config, "idle_heartbeat");
        JsonValueUtils.readBoolean(config, "flow_control", false);
        JsonValueUtils.readBoolean(config, "headers_only", false);
        JsonValueUtils.readBoolean(config, "mem_storage", false);
        JsonValueUtils.readString(config, "filter_subject");
    }

    private static void readConsumerIndexed75(IndexedJsonValue config) {
        readConsumerIndexed50(config);
        IndexedJsonValueUtils.readLong(config, "max_batch", -1);
        IndexedJsonValueUtils.readLong(config, "max_bytes", -1);
        IndexedJsonValueUtils.readInteger(config, "num_replicas", 0);
        IndexedJsonValueUtils.readDate(config, "pause_until");
        IndexedJsonValueUtils.readNanosAsDuration(config, "idle_heartbeat");
        IndexedJsonValueUtils.readBoolean(config, "flow_control", false);
        IndexedJsonValueUtils.readBoolean(config, "headers_only", false);
        IndexedJsonValueUtils.readBoolean(config, "mem_storage", false);
        IndexedJsonValueUtils.readString(config, "filter_subject");
    }

    /** Read 100% of consumer config fields */
    private static void readConsumerEager100(JsonValue config) {
        readConsumerEager75(config);
        JsonValueUtils.readStringListOrNull(config, "filter_subjects");
        JsonValueUtils.readStringMapOrNull(config, "metadata");
        JsonValueUtils.readNanosAsDurationListOrNull(config, "backoff");
        JsonValueUtils.readStringListOrNull(config, "priority_groups");
        JsonValueUtils.readString(config, "priority_policy");
        JsonValueUtils.readNanosAsDuration(config, "priority_timeout");
    }

    private static void readConsumerIndexed100(IndexedJsonValue config) {
        readConsumerIndexed75(config);
        IndexedJsonValueUtils.readStringListOrNull(config, "filter_subjects");
        IndexedJsonValueUtils.readStringMapOrNull(config, "metadata");
        IndexedJsonValueUtils.readNanosAsDurationListOrNull(config, "backoff");
        IndexedJsonValueUtils.readStringListOrNull(config, "priority_groups");
        IndexedJsonValueUtils.readString(config, "priority_policy");
        IndexedJsonValueUtils.readNanosAsDuration(config, "priority_timeout");
    }

    // -----------------------------------------------------------------------
    // "Parse only" - measures just parse cost with zero field reads
    // -----------------------------------------------------------------------

    /** Read 0% - parse only, read nothing */
    @SuppressWarnings("unused")
    private static void readStreamEager0(JsonValue config) { }

    @SuppressWarnings("unused")
    private static void readStreamIndexed0(IndexedJsonValue config) { }

    @SuppressWarnings("unused")
    private static void readConsumerEager0(JsonValue config) { }

    @SuppressWarnings("unused")
    private static void readConsumerIndexed0(IndexedJsonValue config) { }

    // -----------------------------------------------------------------------
    // Benchmark harness
    // -----------------------------------------------------------------------

    @FunctionalInterface
    interface EagerReader { void read(JsonValue config); }

    @FunctionalInterface
    interface IndexedReader { void read(IndexedJsonValue config); }

    /**
     * Benchmark: parse JSON + read fields with the eager (JsonParser) approach.
     * Returns ops/sec.
     */
    private static long benchEager(String json, String configKey, EagerReader reader, int iterations) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            JsonValue root = JsonParser.parse(json);
            JsonValue config = configKey == null ? root : JsonValueUtils.readValue(root, configKey);
            reader.read(config);
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            JsonValue root = JsonParser.parse(json);
            JsonValue config = configKey == null ? root : JsonValueUtils.readValue(root, configKey);
            reader.read(config);
        }
        long elapsed = System.nanoTime() - start;
        return (long) ((double) iterations / elapsed * 1_000_000_000L);
    }

    /**
     * Benchmark: parse JSON + read fields with the indexed (IndexedJsonParser) approach.
     * Returns ops/sec.
     */
    private static long benchIndexed(String json, String configKey, IndexedReader reader, int iterations,
                                     JsonParser.Option... options) throws Exception {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            IndexedJsonValue root = IndexedJsonParser.parse(json, options);
            IndexedJsonValue config = configKey == null ? root : IndexedJsonValueUtils.readValue(root, configKey);
            reader.read(config);
        }

        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            IndexedJsonValue root = IndexedJsonParser.parse(json, options);
            IndexedJsonValue config = configKey == null ? root : IndexedJsonValueUtils.readValue(root, configKey);
            reader.read(config);
        }
        long elapsed = System.nanoTime() - start;
        return (long) ((double) iterations / elapsed * 1_000_000_000L);
    }

    // -----------------------------------------------------------------------
    // Benchmark entry points. Disabled so CI/CD skips them.
    // Run manually: gradle test --tests "*ParserBenchmark*"
    // -----------------------------------------------------------------------

    @Test
    public void benchmarkAll() throws Exception {
        benchStreamConfig(STREAM_PRETTY, "pretty", STREAM_PRETTY.length());
        benchStreamConfig(STREAM_COMPACT, "compact", STREAM_COMPACT.length());
        benchConsumerConfig(CONSUMER_PRETTY, "pretty", CONSUMER_PRETTY.length());
        benchConsumerConfig(CONSUMER_COMPACT, "compact", CONSUMER_COMPACT.length());
        benchMinimal(STREAM_MIN_PRETTY, STREAM_MIN_COMPACT, "stream_info_minimal");
        benchMinimal(CONSUMER_MIN_PRETTY, CONSUMER_MIN_COMPACT, "consumer_info_minimal");
    }

    private void benchStreamConfig(String json, String format, int len) throws Exception {
        System.out.println();
        System.out.printf("=== StreamInfo %s (%d chars) -- eager vs indexed vs lazy ===%n", format, len);
        System.out.println();
        runDeepScenario("parse only", json, "config",
            ParserBenchmark::readStreamEager0, ParserBenchmark::readStreamIndexed0, c -> {});
        runDeepScenario("10% fields", json, "config",
            ParserBenchmark::readStreamEager10, ParserBenchmark::readStreamIndexed10,
            ParserBenchmark::readStreamDeep10);
        runDeepScenario("25% fields", json, "config",
            ParserBenchmark::readStreamEager25, ParserBenchmark::readStreamIndexed25,
            ParserBenchmark::readStreamDeep25);
        runDeepScenario("50% fields", json, "config",
            ParserBenchmark::readStreamEager50, ParserBenchmark::readStreamIndexed50,
            ParserBenchmark::readStreamDeep50);
        runDeepScenario("100% fields", json, "config",
            ParserBenchmark::readStreamEager100, ParserBenchmark::readStreamIndexed100,
            ParserBenchmark::readStreamDeep100);
    }

    private void benchConsumerConfig(String json, String format, int len) throws Exception {
        System.out.println();
        System.out.printf("=== ConsumerInfo %s (%d chars) -- eager vs indexed vs lazy ===%n", format, len);
        System.out.println();
        runDeepScenario("parse only", json, "config",
            ParserBenchmark::readConsumerEager0, ParserBenchmark::readConsumerIndexed0, c -> {});
        runDeepScenario("10% fields", json, "config",
            ParserBenchmark::readConsumerEager10, ParserBenchmark::readConsumerIndexed10,
            ParserBenchmark::readConsumerDeep10);
        runDeepScenario("25% fields", json, "config",
            ParserBenchmark::readConsumerEager25, ParserBenchmark::readConsumerIndexed25,
            ParserBenchmark::readConsumerDeep25);
        runDeepScenario("50% fields", json, "config",
            ParserBenchmark::readConsumerEager50, ParserBenchmark::readConsumerIndexed50,
            ParserBenchmark::readConsumerDeep50);
        runDeepScenario("100% fields", json, "config",
            ParserBenchmark::readConsumerEager100, ParserBenchmark::readConsumerIndexed100,
            ParserBenchmark::readConsumerDeep100);
    }

    private void benchMinimal(String pretty, String compact, String name) throws Exception {
        for (String[] entry : new String[][]{{"pretty", pretty}, {"compact", compact}}) {
            String format = entry[0];
            String json = entry[1];
            System.out.println();
            System.out.printf("=== %s %s (%d chars) -- eager vs indexed vs lazy ===%n", name, format, json.length());
            System.out.println();
            runDeepScenario("parse only", json, null, v -> {}, v -> {}, v -> {});
            runDeepScenario("name only", json, null,
                v -> JsonValueUtils.readString(v, "name"),
                v -> IndexedJsonValueUtils.readString(v, "name"),
                v -> LazyJsonValueUtils.readString(v, "name"));
            runDeepScenario("all fields", json, null,
                name.contains("stream") ? ParserBenchmark::readStreamMinEagerAll : ParserBenchmark::readConsumerMinEagerAll,
                name.contains("stream") ? ParserBenchmark::readStreamMinIndexedAll : ParserBenchmark::readConsumerMinIndexedAll,
                name.contains("stream") ? ParserBenchmark::readStreamMinDeepAll : ParserBenchmark::readConsumerMinDeepAll);
        }
    }

    // -----------------------------------------------------------------------
    // Deep indexed reader functions (parallel to the indexed ones)
    // -----------------------------------------------------------------------

    @FunctionalInterface
    interface DeepReader { void read(LazyJsonValue config); }

    private static void readStreamDeep10(LazyJsonValue config) {
        LazyJsonValueUtils.readString(config, "name");
        LazyJsonValueUtils.readString(config, "storage");
        LazyJsonValueUtils.readInteger(config, "num_replicas", 1);
    }

    private static void readStreamDeep25(LazyJsonValue config) {
        readStreamDeep10(config);
        LazyJsonValueUtils.readString(config, "retention");
        LazyJsonValueUtils.readString(config, "discard");
        LazyJsonValueUtils.readLong(config, "max_consumers", -1);
        LazyJsonValueUtils.readLong(config, "max_msgs", -1);
        LazyJsonValueUtils.readLong(config, "max_bytes", -1);
    }

    private static void readStreamDeep50(LazyJsonValue config) {
        readStreamDeep25(config);
        LazyJsonValueUtils.readLong(config, "max_msgs_per_sub", -1);
        LazyJsonValueUtils.readNanosAsDuration(config, "max_age");
        LazyJsonValueUtils.readInteger(config, "max_msg_size", -1);
        LazyJsonValueUtils.readNanosAsDuration(config, "duplicate_window");
        LazyJsonValueUtils.readStringListOrEmpty(config, "subjects");
        LazyJsonValueUtils.readLong(config, "first_seq", 1);
    }

    private static void readStreamDeep100(LazyJsonValue config) {
        readStreamDeep50(config);
        LazyJsonValueUtils.readBoolean(config, "no_ack", false);
        LazyJsonValueUtils.readString(config, "template_owner");
        LazyJsonValueUtils.readString(config, "description");
        LazyJsonValueUtils.readBoolean(config, "sealed", false);
        LazyJsonValueUtils.readBoolean(config, "allow_rollup_hdrs", false);
        LazyJsonValueUtils.readBoolean(config, "allow_direct", false);
        LazyJsonValueUtils.readBoolean(config, "mirror_direct", false);
        LazyJsonValueUtils.readBoolean(config, "deny_delete", false);
        LazyJsonValueUtils.readBoolean(config, "deny_purge", false);
        LazyJsonValueUtils.readBoolean(config, "discard_new_per_subject", false);
        LazyJsonValueUtils.readStringMapOrNull(config, "metadata");
        LazyJsonValueUtils.readBoolean(config, "allow_msg_ttl", false);
        LazyJsonValueUtils.readBoolean(config, "allow_msg_schedules", false);
        LazyJsonValueUtils.readBoolean(config, "allow_msg_counter", false);
        LazyJsonValueUtils.readBoolean(config, "allow_atomic", false);
        LazyJsonValueUtils.readString(config, "persist_mode");
        LazyJsonValueUtils.readString(config, "compression");
        LazyJsonValueUtils.readNanosAsDuration(config, "subject_delete_marker_ttl");
        LazyJsonValueUtils.readValue(config, "placement");
        LazyJsonValueUtils.readValue(config, "republish");
        LazyJsonValueUtils.readValue(config, "subject_transform");
        LazyJsonValueUtils.readValue(config, "consumer_limits");
        LazyJsonValueUtils.readValue(config, "mirror");
        LazyJsonValueUtils.readValue(config, "sources");
    }

    private static void readConsumerDeep10(LazyJsonValue config) {
        LazyJsonValueUtils.readString(config, "name");
        LazyJsonValueUtils.readString(config, "durable_name");
        LazyJsonValueUtils.readString(config, "deliver_policy");
    }

    private static void readConsumerDeep25(LazyJsonValue config) {
        readConsumerDeep10(config);
        LazyJsonValueUtils.readString(config, "ack_policy");
        LazyJsonValueUtils.readString(config, "replay_policy");
        LazyJsonValueUtils.readString(config, "description");
        LazyJsonValueUtils.readString(config, "deliver_subject");
        LazyJsonValueUtils.readString(config, "deliver_group");
        LazyJsonValueUtils.readLong(config, "max_deliver", -1);
    }

    private static void readConsumerDeep50(LazyJsonValue config) {
        readConsumerDeep25(config);
        LazyJsonValueUtils.readString(config, "sample_freq");
        LazyJsonValueUtils.readDate(config, "opt_start_time");
        LazyJsonValueUtils.readNanosAsDuration(config, "ack_wait");
        LazyJsonValueUtils.readNanosAsDuration(config, "max_expires");
        LazyJsonValueUtils.readNanosAsDuration(config, "inactive_threshold");
        LazyJsonValueUtils.readLong(config, "opt_start_seq", -1);
        LazyJsonValueUtils.readLong(config, "rate_limit_bps", -1);
        LazyJsonValueUtils.readLong(config, "max_ack_pending", -1);
        LazyJsonValueUtils.readLong(config, "max_waiting", -1);
    }

    private static void readConsumerDeep100(LazyJsonValue config) {
        readConsumerDeep50(config);
        LazyJsonValueUtils.readLong(config, "max_batch", -1);
        LazyJsonValueUtils.readLong(config, "max_bytes", -1);
        LazyJsonValueUtils.readInteger(config, "num_replicas", 0);
        LazyJsonValueUtils.readDate(config, "pause_until");
        LazyJsonValueUtils.readNanosAsDuration(config, "idle_heartbeat");
        LazyJsonValueUtils.readBoolean(config, "flow_control", false);
        LazyJsonValueUtils.readBoolean(config, "headers_only", false);
        LazyJsonValueUtils.readBoolean(config, "mem_storage", false);
        LazyJsonValueUtils.readString(config, "filter_subject");
        LazyJsonValueUtils.readStringListOrNull(config, "filter_subjects");
        LazyJsonValueUtils.readStringMapOrNull(config, "metadata");
        LazyJsonValueUtils.readNanosAsDurationListOrNull(config, "backoff");
        LazyJsonValueUtils.readStringListOrNull(config, "priority_groups");
        LazyJsonValueUtils.readString(config, "priority_policy");
        LazyJsonValueUtils.readNanosAsDuration(config, "priority_timeout");
    }

    private static long benchDeep(String json, String configKey, DeepReader reader, int iterations) throws Exception {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            LazyJsonValue root = LazyJsonParser.parse(json);
            LazyJsonValue config = configKey == null ? root : LazyJsonValueUtils.readValue(root, configKey);
            reader.read(config);
        }
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            LazyJsonValue root = LazyJsonParser.parse(json);
            LazyJsonValue config = configKey == null ? root : LazyJsonValueUtils.readValue(root, configKey);
            reader.read(config);
        }
        long elapsed = System.nanoTime() - start;
        return (long) ((double) iterations / elapsed * 1_000_000_000L);
    }

    private static void runDeepScenario(String label, String json, String configKey,
                                        EagerReader eagerReader, IndexedReader indexedReader,
                                        DeepReader deepReader) throws Exception {
        long eagerOps = benchEager(json, configKey, eagerReader, MEASURE_ITERATIONS);
        long indexedOps = benchIndexed(json, configKey, indexedReader, MEASURE_ITERATIONS);
        long deepOps = benchDeep(json, configKey, deepReader, MEASURE_ITERATIONS);
        System.out.printf("  %-14s  eager: %,8d  indexed: %,8d (%.2fx)  deep: %,8d (%.2fx)%n",
            label, eagerOps, indexedOps, (double) indexedOps / eagerOps,
            deepOps, (double) deepOps / eagerOps);
    }

    // -----------------------------------------------------------------------
    // Minimal JSON reader functions -- flat configs, no nesting
    // -----------------------------------------------------------------------

    // Stream minimal: all fields
    private static void readStreamMinEagerAll(JsonValue v) {
        JsonValueUtils.readString(v, "name");
        JsonValueUtils.readStringListOrEmpty(v, "subjects");
        JsonValueUtils.readString(v, "retention");
        JsonValueUtils.readString(v, "storage");
        JsonValueUtils.readInteger(v, "num_replicas", 1);
        JsonValueUtils.readString(v, "discard");
        JsonValueUtils.readNanosAsDuration(v, "duplicate_window");
        JsonValueUtils.readValue(v, "consumer_limits");
        JsonValueUtils.readStringMapOrNull(v, "metadata");
    }

    private static void readStreamMinIndexedAll(IndexedJsonValue v) {
        IndexedJsonValueUtils.readString(v, "name");
        IndexedJsonValueUtils.readStringListOrEmpty(v, "subjects");
        IndexedJsonValueUtils.readString(v, "retention");
        IndexedJsonValueUtils.readString(v, "storage");
        IndexedJsonValueUtils.readInteger(v, "num_replicas", 1);
        IndexedJsonValueUtils.readString(v, "discard");
        IndexedJsonValueUtils.readNanosAsDuration(v, "duplicate_window");
        IndexedJsonValueUtils.readValue(v, "consumer_limits");
        IndexedJsonValueUtils.readStringMapOrNull(v, "metadata");
    }

    private static void readStreamMinDeepAll(LazyJsonValue v) {
        LazyJsonValueUtils.readString(v, "name");
        LazyJsonValueUtils.readStringListOrEmpty(v, "subjects");
        LazyJsonValueUtils.readString(v, "retention");
        LazyJsonValueUtils.readString(v, "storage");
        LazyJsonValueUtils.readInteger(v, "num_replicas", 1);
        LazyJsonValueUtils.readString(v, "discard");
        LazyJsonValueUtils.readNanosAsDuration(v, "duplicate_window");
        LazyJsonValueUtils.readValue(v, "consumer_limits");
        LazyJsonValueUtils.readStringMapOrNull(v, "metadata");
    }

    // Consumer minimal: all fields
    private static void readConsumerMinEagerAll(JsonValue v) {
        JsonValueUtils.readString(v, "name");
        JsonValueUtils.readString(v, "deliver_policy");
        JsonValueUtils.readString(v, "ack_policy");
        JsonValueUtils.readNanosAsDuration(v, "ack_wait");
        JsonValueUtils.readLong(v, "max_ack_pending", -1);
        JsonValueUtils.readString(v, "replay_policy");
        JsonValueUtils.readLong(v, "max_waiting", -1);
        JsonValueUtils.readNanosAsDuration(v, "inactive_threshold");
        JsonValueUtils.readInteger(v, "num_replicas", 0);
        JsonValueUtils.readStringMapOrNull(v, "metadata");
    }

    private static void readConsumerMinIndexedAll(IndexedJsonValue v) {
        IndexedJsonValueUtils.readString(v, "name");
        IndexedJsonValueUtils.readString(v, "deliver_policy");
        IndexedJsonValueUtils.readString(v, "ack_policy");
        IndexedJsonValueUtils.readNanosAsDuration(v, "ack_wait");
        IndexedJsonValueUtils.readLong(v, "max_ack_pending", -1);
        IndexedJsonValueUtils.readString(v, "replay_policy");
        IndexedJsonValueUtils.readLong(v, "max_waiting", -1);
        IndexedJsonValueUtils.readNanosAsDuration(v, "inactive_threshold");
        IndexedJsonValueUtils.readInteger(v, "num_replicas", 0);
        IndexedJsonValueUtils.readStringMapOrNull(v, "metadata");
    }

    private static void readConsumerMinDeepAll(LazyJsonValue v) {
        LazyJsonValueUtils.readString(v, "name");
        LazyJsonValueUtils.readString(v, "deliver_policy");
        LazyJsonValueUtils.readString(v, "ack_policy");
        LazyJsonValueUtils.readNanosAsDuration(v, "ack_wait");
        LazyJsonValueUtils.readLong(v, "max_ack_pending", -1);
        LazyJsonValueUtils.readString(v, "replay_policy");
        LazyJsonValueUtils.readLong(v, "max_waiting", -1);
        LazyJsonValueUtils.readNanosAsDuration(v, "inactive_threshold");
        LazyJsonValueUtils.readInteger(v, "num_replicas", 0);
        LazyJsonValueUtils.readStringMapOrNull(v, "metadata");
    }

}
