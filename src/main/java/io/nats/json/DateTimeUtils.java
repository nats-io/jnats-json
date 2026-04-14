// Copyright 2020-2026 The NATS Authors
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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Helper utilities for ZoneDateTime
 */
@NullMarked
public abstract class DateTimeUtils {
    private DateTimeUtils() {}  /* ensures cannot be constructed */

    private static final long NANO_FACTOR = 1_000_000_000;

    /**
     * The ZoneId for GMT
     */
    public static final ZoneId ZONE_ID_GMT = ZoneId.of("GMT");

    /**
     * The ZoneDateTime uses as a default, can be used instead of null
     */
    public static final ZonedDateTime DEFAULT_TIME = ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZONE_ID_GMT);

    /**
     * The formatter to crate RFC 3339 strings from dates.
     */
    public static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnn'Z'");

    /**
     * Convert a ZoneDateTime to a GMT version of the ZoneDateTime
     * @param zonedDateTime the input
     * @return the output
     */
    public static ZonedDateTime toGmt(ZonedDateTime zonedDateTime) {
        return zonedDateTime.withZoneSameInstant(ZONE_ID_GMT);
    }

    /**
     * Get an instance of ZonedDateTime.now(), but for the GMT timezone
     * @return the current date-time using the system clock in GMT
     */
    public static ZonedDateTime gmtNow() {
        return ZonedDateTime.now().withZoneSameInstant(ZONE_ID_GMT);
    }

    /**
     * Compare two ZonedDateTime after converting them to the GMT timezone
     * @param zdt1 the first ZonedDateTime
     * @param zdt2 the second ZonedDateTime
     * @return true if they are equal
     */
    public static boolean equals(@Nullable ZonedDateTime zdt1, @Nullable ZonedDateTime zdt2) {
        if (zdt1 == zdt2) return true;
        if (zdt1 == null || zdt2 == null) return false;
        return zdt1.withZoneSameInstant(ZONE_ID_GMT).equals(zdt2.withZoneSameInstant(ZONE_ID_GMT));
    }

    /**
     * Format the ZonedDateTime to RFC 3339, most commonly used for input to the serer
     * @param zonedDateTime the input
     * @return the formatted string
     */
    public static String toRfc3339(ZonedDateTime zonedDateTime) {
        return RFC3339_FORMATTER.format(toGmt(zonedDateTime));
    }

    /**
     * Parses a date time from the server.
     * @param dateTime - date time from the server.
     * @return a Zoned Date time.
     */
    public static ZonedDateTime parseDateTime(String dateTime) {
        return parseDateTime(dateTime, DEFAULT_TIME);
    }

    /**
     * Parses a date time from the server.
     * @param dateTime - date time from the server.
     * @param dflt - the default ZoneDateTime to use if the input string exceptions while parsing
     * @return a ZonedDateTime.
     */
    public static ZonedDateTime parseDateTime(String dateTime, ZonedDateTime dflt) {
        try {
            return toGmt(ZonedDateTime.parse(dateTime));
        }
        catch (DateTimeParseException s) {
            return dflt;
        }
    }

    /**
     * Parses a date time from the server which throws if there is an error parsing.
     * @param dateTime - date time from the server.
     * @return a ZonedDateTime.
     */
    public static ZonedDateTime parseDateTimeThrowParseError(String dateTime) {
        return toGmt(ZonedDateTime.parse(dateTime));
    }

    /**
     * Parses a long timestamp with nano precision in epoch UTC to the system
     * default time-zone date time
     *
     * @param timestampNanos String timestamp
     * @return a local Zoned Date time.
     */
    public static ZonedDateTime parseDateTimeNanos(String timestampNanos) {
        return parseDateTimeNanos(timestampNanos, ZoneId.systemDefault());
    }

    /**
     * Parses a long timestamp with nano precision in epoch UTC to a Zoned date
     * time
     *
     * @param timestampNanos String timestamp
     * @param zoneId ZoneId
     * @return a Zoned Date time.
     * @throws NumberFormatException if the {@code String} does not contain a parsable {@code long}.
     */
    public static ZonedDateTime parseDateTimeNanos(String timestampNanos, ZoneId zoneId) {
        long ts = Long.parseLong(timestampNanos);
        long seconds = ts / NANO_FACTOR;
        long nanos = ts % NANO_FACTOR;
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanos), zoneId);
    }

    /**
     * Get a ZonedDateTime representing millis from now
     * @param millis the millis from now value
     * @return a ZonedDateTime.
     */
    public static ZonedDateTime fromNow(long millis) {
        return ZonedDateTime.ofInstant(Instant.now().plusMillis(millis), ZONE_ID_GMT);
    }

    /**
     * Get a ZonedDateTime representing millis from now
     * @param dur the duration to use the millis for from now
     * @return a ZonedDateTime.
     */
    public static ZonedDateTime fromNow(Duration dur) {
        return ZonedDateTime.ofInstant(Instant.now().plusMillis(dur.toMillis()), ZONE_ID_GMT);
    }
}
