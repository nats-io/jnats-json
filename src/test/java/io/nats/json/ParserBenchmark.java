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

/**
 * Benchmark comparing JsonParser (eager/copy) vs IndexedJsonParser (lazy/indexed)
 * for realistic workloads modeled after StreamFields and ConsumerFields parsing.
 *
 * Tests various read percentages: how much of the parsed data is actually accessed.
 * The indexed parser's advantage grows as less data is accessed, since it defers
 * string copying and number parsing until read time.
 */
public final class ParserBenchmark {

    // Warm-up and measurement parameters
    private static final int WARMUP_ITERATIONS = 5_000;
    private static final int MEASURE_ITERATIONS = 20_000;

    // JSON payloads loaded once
    private static final String STREAM_JSON;
    private static final String CONSUMER_JSON;

    static {
        STREAM_JSON = ResourceUtils.resourceAsString("StreamInfo-v3.json");
        CONSUMER_JSON = ResourceUtils.resourceAsString("ConsumerInfo-v3.json");
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
                                     IndexedJsonParser.Option... options) throws Exception {
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

    private static void runScenario(String label, String json, String configKey,
                                    EagerReader eagerReader, IndexedReader indexedReader) throws Exception {
        long eagerOps = benchEager(json, configKey, eagerReader, MEASURE_ITERATIONS);
        long indexedOps = benchIndexed(json, configKey, indexedReader, MEASURE_ITERATIONS);
        double ratio = (double) indexedOps / eagerOps;
        String marker = ratio > 1.0 ? "indexed wins" : "eager wins";
        System.out.printf("  %-14s  eager: %,8d ops/s  indexed: %,8d ops/s  ratio: %.2fx  (%s)%n",
            label, eagerOps, indexedOps, ratio, marker);
    }

    private static void runThreeWayScenario(String label, String json, String configKey,
                                            EagerReader eagerReader, IndexedReader indexedReader) throws Exception {
        long eagerOps = benchEager(json, configKey, eagerReader, MEASURE_ITERATIONS);
        // Default mode (integers-only)
        long intOnlyOps = benchIndexed(json, configKey, indexedReader, MEASURE_ITERATIONS);
        // DECIMALS mode (full decimal support)
        long decimalsOps = benchIndexed(json, configKey, indexedReader, MEASURE_ITERATIONS,
            IndexedJsonParser.Option.DECIMALS);
        double ratioInt = (double) intOnlyOps / eagerOps;
        double ratioDec = (double) decimalsOps / eagerOps;
        System.out.printf("  %-14s  eager: %,8d  default: %,8d (%.2fx)  decimals: %,8d (%.2fx)%n",
            label, eagerOps, intOnlyOps, ratioInt, decimalsOps, ratioDec);
    }

    // -----------------------------------------------------------------------
    // Test entry point (run with: gradle test --tests "*ParserBenchmark*")
    // -----------------------------------------------------------------------

    @Test
    public void benchmarkStreamFields() throws Exception {
        System.out.println();
        System.out.println("=== StreamInfo Benchmark (parse + read config fields) ===");
        System.out.println("  JSON size: " + STREAM_JSON.length() + " chars, " +
            "warmup: " + WARMUP_ITERATIONS + ", measure: " + MEASURE_ITERATIONS);
        System.out.println();

        runScenario("0% (parse only)", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager0, ParserBenchmark::readStreamIndexed0);
        runScenario("10% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager10, ParserBenchmark::readStreamIndexed10);
        runScenario("25% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager25, ParserBenchmark::readStreamIndexed25);
        runScenario("50% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager50, ParserBenchmark::readStreamIndexed50);
        runScenario("75% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager75, ParserBenchmark::readStreamIndexed75);
        runScenario("100% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager100, ParserBenchmark::readStreamIndexed100);

        System.out.println();
    }

    @Test
    public void benchmarkConsumerFields() throws Exception {
        System.out.println();
        System.out.println("=== ConsumerInfo Benchmark (parse + read config fields) ===");
        System.out.println("  JSON size: " + CONSUMER_JSON.length() + " chars, " +
            "warmup: " + WARMUP_ITERATIONS + ", measure: " + MEASURE_ITERATIONS);
        System.out.println();

        runScenario("0% (parse only)", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager0, ParserBenchmark::readConsumerIndexed0);
        runScenario("10% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager10, ParserBenchmark::readConsumerIndexed10);
        runScenario("25% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager25, ParserBenchmark::readConsumerIndexed25);
        runScenario("50% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager50, ParserBenchmark::readConsumerIndexed50);
        runScenario("75% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager75, ParserBenchmark::readConsumerIndexed75);
        runScenario("100% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager100, ParserBenchmark::readConsumerIndexed100);

        System.out.println();
    }

    @Test
    public void benchmarkParseOnly() throws Exception {
        System.out.println();
        System.out.println("=== Parse-Only Benchmark (no field reads at all) ===");
        System.out.println();

        // Stream
        long eStream = benchEager(STREAM_JSON, null, v -> {}, MEASURE_ITERATIONS);
        long iStream = benchIndexed(STREAM_JSON, null, v -> {}, MEASURE_ITERATIONS);
        System.out.printf("  StreamInfo     eager: %,8d ops/s  indexed: %,8d ops/s  ratio: %.2fx%n",
            eStream, iStream, (double) iStream / eStream);

        // Consumer
        long eConsumer = benchEager(CONSUMER_JSON, null, v -> {}, MEASURE_ITERATIONS);
        long iConsumer = benchIndexed(CONSUMER_JSON, null, v -> {}, MEASURE_ITERATIONS);
        System.out.printf("  ConsumerInfo   eager: %,8d ops/s  indexed: %,8d ops/s  ratio: %.2fx%n",
            eConsumer, iConsumer, (double) iConsumer / iConsumer);

        System.out.println();
    }

    @Test
    public void benchmarkFullRoundTrip() throws Exception {
        System.out.println();
        System.out.println("=== Full Round-Trip: parse + read ALL + also read top-level fields ===");
        System.out.println("  (Simulates reading the entire StreamInfo/ConsumerInfo response)");
        System.out.println();

        // StreamInfo: read top-level fields (type, created, ts, state, cluster, etc.) + config
        EagerReader streamFullEager = root -> {
            JsonValueUtils.readString(root, "type");
            JsonValueUtils.readDate(root, "created");
            JsonValueUtils.readDate(root, "ts");
            JsonValue config = JsonValueUtils.readValue(root, "config");
            readStreamEager100(config);
            // state sub-object
            JsonValue state = JsonValueUtils.readValue(root, "state");
            if (state != null) {
                JsonValueUtils.readLong(state, "messages", 0);
                JsonValueUtils.readLong(state, "bytes", 0);
                JsonValueUtils.readLong(state, "first_seq", 0);
                JsonValueUtils.readLong(state, "last_seq", 0);
                JsonValueUtils.readLong(state, "consumer_count", 0);
                JsonValueUtils.readLong(state, "num_subjects", 0);
                JsonValueUtils.readLong(state, "num_deleted", 0);
            }
        };

        IndexedReader streamFullIndexed = root -> {
            IndexedJsonValueUtils.readString(root, "type");
            IndexedJsonValueUtils.readDate(root, "created");
            IndexedJsonValueUtils.readDate(root, "ts");
            IndexedJsonValue config = IndexedJsonValueUtils.readValue(root, "config");
            readStreamIndexed100(config);
            IndexedJsonValue state = IndexedJsonValueUtils.readValue(root, "state");
            if (state != null) {
                IndexedJsonValueUtils.readLong(state, "messages", 0);
                IndexedJsonValueUtils.readLong(state, "bytes", 0);
                IndexedJsonValueUtils.readLong(state, "first_seq", 0);
                IndexedJsonValueUtils.readLong(state, "last_seq", 0);
                IndexedJsonValueUtils.readLong(state, "consumer_count", 0);
                IndexedJsonValueUtils.readLong(state, "num_subjects", 0);
                IndexedJsonValueUtils.readLong(state, "num_deleted", 0);
            }
        };

        runScenario("StreamInfo", STREAM_JSON, null, streamFullEager, streamFullIndexed);

        // ConsumerInfo: read top-level fields + config
        EagerReader consumerFullEager = root -> {
            JsonValueUtils.readString(root, "type");
            JsonValueUtils.readString(root, "stream_name");
            JsonValueUtils.readString(root, "name");
            JsonValueUtils.readDate(root, "created");
            JsonValueUtils.readDate(root, "ts");
            JsonValueUtils.readLong(root, "num_pending", 0);
            JsonValueUtils.readLong(root, "num_ack_pending", 0);
            JsonValueUtils.readLong(root, "num_redelivered", 0);
            JsonValueUtils.readBoolean(root, "paused", false);
            JsonValueUtils.readNanosAsDuration(root, "pause_remaining");
            JsonValue config = JsonValueUtils.readValue(root, "config");
            readConsumerEager100(config);
        };

        IndexedReader consumerFullIndexed = root -> {
            IndexedJsonValueUtils.readString(root, "type");
            IndexedJsonValueUtils.readString(root, "stream_name");
            IndexedJsonValueUtils.readString(root, "name");
            IndexedJsonValueUtils.readDate(root, "created");
            IndexedJsonValueUtils.readDate(root, "ts");
            IndexedJsonValueUtils.readLong(root, "num_pending", 0);
            IndexedJsonValueUtils.readLong(root, "num_ack_pending", 0);
            IndexedJsonValueUtils.readLong(root, "num_redelivered", 0);
            IndexedJsonValueUtils.readBoolean(root, "paused", false);
            IndexedJsonValueUtils.readNanosAsDuration(root, "pause_remaining");
            IndexedJsonValue config = IndexedJsonValueUtils.readValue(root, "config");
            readConsumerIndexed100(config);
        };

        runScenario("ConsumerInfo", CONSUMER_JSON, null, consumerFullEager, consumerFullIndexed);

        System.out.println();
    }

    @Test
    public void benchmarkIntegersOnly() throws Exception {
        System.out.println();
        System.out.println("=== Default (integers-only) vs DECIMALS mode (eager vs default vs decimals) ===");
        System.out.println("  All numbers are ops/s. Ratios are vs eager.");
        System.out.println();

        System.out.println("  -- StreamInfo (config fields) --");
        runThreeWayScenario("0% (parse)", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager0, ParserBenchmark::readStreamIndexed0);
        runThreeWayScenario("10% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager10, ParserBenchmark::readStreamIndexed10);
        runThreeWayScenario("25% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager25, ParserBenchmark::readStreamIndexed25);
        runThreeWayScenario("50% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager50, ParserBenchmark::readStreamIndexed50);
        runThreeWayScenario("75% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager75, ParserBenchmark::readStreamIndexed75);
        runThreeWayScenario("100% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager100, ParserBenchmark::readStreamIndexed100);

        System.out.println();
        System.out.println("  -- ConsumerInfo (config fields) --");
        runThreeWayScenario("0% (parse)", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager0, ParserBenchmark::readConsumerIndexed0);
        runThreeWayScenario("10% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager10, ParserBenchmark::readConsumerIndexed10);
        runThreeWayScenario("25% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager25, ParserBenchmark::readConsumerIndexed25);
        runThreeWayScenario("50% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager50, ParserBenchmark::readConsumerIndexed50);
        runThreeWayScenario("75% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager75, ParserBenchmark::readConsumerIndexed75);
        runThreeWayScenario("100% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager100, ParserBenchmark::readConsumerIndexed100);

        System.out.println();
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

    @Test
    public void benchmarkDeepIndexed() throws Exception {
        System.out.println();
        System.out.println("=== Deep Indexed: eager vs indexed vs deep (all ops/s, ratios vs eager) ===");
        System.out.println();

        System.out.println("  -- StreamInfo (parse + read config fields only) --");
        runDeepScenario("0% (parse)", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager0, ParserBenchmark::readStreamIndexed0, c -> {});
        runDeepScenario("10% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager10, ParserBenchmark::readStreamIndexed10,
            ParserBenchmark::readStreamDeep10);
        runDeepScenario("25% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager25, ParserBenchmark::readStreamIndexed25,
            ParserBenchmark::readStreamDeep25);
        runDeepScenario("50% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager50, ParserBenchmark::readStreamIndexed50,
            ParserBenchmark::readStreamDeep50);
        runDeepScenario("100% fields", STREAM_JSON, "config",
            ParserBenchmark::readStreamEager100, ParserBenchmark::readStreamIndexed100,
            ParserBenchmark::readStreamDeep100);

        System.out.println();
        System.out.println("  -- ConsumerInfo (parse + read config fields only) --");
        runDeepScenario("0% (parse)", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager0, ParserBenchmark::readConsumerIndexed0, c -> {});
        runDeepScenario("10% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager10, ParserBenchmark::readConsumerIndexed10,
            ParserBenchmark::readConsumerDeep10);
        runDeepScenario("25% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager25, ParserBenchmark::readConsumerIndexed25,
            ParserBenchmark::readConsumerDeep25);
        runDeepScenario("50% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager50, ParserBenchmark::readConsumerIndexed50,
            ParserBenchmark::readConsumerDeep50);
        runDeepScenario("100% fields", CONSUMER_JSON, "config",
            ParserBenchmark::readConsumerEager100, ParserBenchmark::readConsumerIndexed100,
            ParserBenchmark::readConsumerDeep100);

        System.out.println();
    }
}
