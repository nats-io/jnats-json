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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An indexed JSON parser that builds a tree of {@link IndexedJsonValue} nodes
 * referencing the original source {@code char[]} instead of copying strings
 * and parsing numbers eagerly.
 * <p>
 * Strings and numbers are stored as offset ranges into the source array and
 * are only materialized (copied / parsed) when their accessor methods are called.
 * Map keys are extracted during parsing (required for HashMap lookup), but map
 * values and all other leaf values remain lazy.
 * <p>
 * The parser validates structure and string escapes identically to {@link JsonParser}.
 */
public class IndexedJsonParser {

    private static final boolean[] IS_DELIMITER = new boolean[128];

    static {
        for (char c : ",:]}/\\\"[{;=#".toCharArray()) {
            IS_DELIMITER[c] = true;
        }
    }

    // ---- static entry points ----

    @NonNull
    public static IndexedJsonValue parse(char @Nullable [] json) throws JsonParseException {
        return new IndexedJsonParser(json, 0, false, true).parse();
    }

    @NonNull
    public static IndexedJsonValue parse(char @Nullable [] json, int startIndex) throws JsonParseException {
        return new IndexedJsonParser(json, startIndex, false, true).parse();
    }

    @NonNull
    public static IndexedJsonValue parse(char @Nullable [] json, JsonParser.@Nullable Option... options) throws JsonParseException {
        return new IndexedJsonParser(json, 0, options).parse();
    }

    @NonNull
    public static IndexedJsonValue parse(String json) throws JsonParseException {
        return new IndexedJsonParser(json.toCharArray(), 0, false, true).parse();
    }

    @NonNull
    public static IndexedJsonValue parse(String json, int startIndex) throws JsonParseException {
        return new IndexedJsonParser(json.toCharArray(), startIndex, false, true).parse();
    }

    @NonNull
    public static IndexedJsonValue parse(String json, JsonParser.@Nullable Option... options) throws JsonParseException {
        return new IndexedJsonParser(json.toCharArray(), 0, options).parse();
    }

    @NonNull
    public static IndexedJsonValue parse(byte[] json) throws JsonParseException {
        return new IndexedJsonParser(bytesToChars(json), 0, false, true).parse();
    }

    @NonNull
    public static IndexedJsonValue parse(byte[] json, JsonParser.@Nullable Option... options) throws JsonParseException {
        return new IndexedJsonParser(bytesToChars(json), 0, options).parse();
    }

    @NonNull
    public static IndexedJsonValue parseUnchecked(char @Nullable [] json) {
        try { return parse(json); }
        catch (JsonParseException j) { throw new RuntimeException(j); }
    }

    @NonNull
    public static IndexedJsonValue parseUnchecked(String json) {
        try { return parse(json); }
        catch (JsonParseException j) { throw new RuntimeException(j); }
    }

    @NonNull
    public static IndexedJsonValue parseUnchecked(byte[] json) {
        try { return parse(json); }
        catch (JsonParseException j) { throw new RuntimeException(j); }
    }

    @NonNull
    public static IndexedJsonValue parseUnchecked(String json, JsonParser.@Nullable Option... options) {
        try { return parse(json, options); }
        catch (JsonParseException j) { throw new RuntimeException(j); }
    }

    // ---- instance fields ----

    private final char @NonNull [] json;
    private final boolean keepNulls;
    private final boolean integersOnly;
    private final int len;
    private int idx;
    private int nextIdx;
    private char previous;
    private char current;
    private char next;

    // Reusable buffer for extracting map keys (keys must be materialized for lookup)
    private final StringBuilder keyBuffer = new StringBuilder(64);

    private IndexedJsonParser(char @Nullable [] json, int startIndex, boolean keepNulls, boolean integersOnly) {
        this.keepNulls = keepNulls;
        this.integersOnly = integersOnly;
        if (json == null) {
            this.json = new char[0];
            len = 0;
        }
        else {
            this.json = json;
            len = json.length;
        }
        idx = startIndex;
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index.");
        }
        nextIdx = -1;
        previous = 0;
        current = 0;
        next = 0;
    }

    private IndexedJsonParser(char @Nullable [] json, int startIndex, JsonParser.@Nullable Option... options) {
        boolean kn = false;
        boolean decimals = false;
        if (options != null) {
            for (JsonParser.Option opt : options) {
                if (opt == JsonParser.Option.KEEP_NULLS) {
                    kn = true;
                }
                else if (opt == JsonParser.Option.DECIMALS) {
                    decimals = true;
                }
            }
        }
        this.keepNulls = kn;
        this.integersOnly = !decimals;
        if (json == null) {
            this.json = new char[0];
            len = 0;
        }
        else {
            this.json = json;
            len = json.length;
        }
        idx = startIndex;
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index.");
        }
        nextIdx = -1;
        previous = 0;
        current = 0;
        next = 0;
    }

    @NonNull
    private IndexedJsonValue parse() throws JsonParseException {
        char c = peekToken();
        if (c == 0) {
            return IndexedJsonValue.NULL;
        }
        return nextValue();
    }

    private IndexedJsonValue nextValue() throws JsonParseException {
        char c = peekToken();
        if (c == 0) {
            throw new JsonParseException("Unexpected end of data.");
        }
        if (c == '"') {
            nextToken();
            return nextStringValue();
        }
        if (c == '{') {
            nextToken();
            return nextObject();
        }
        if (c == '[') {
            nextToken();
            return nextArray();
        }
        return nextPrimitiveValue();
    }

    /**
     * Index a string value: record the start/end offsets and whether the string
     * contains escape sequences, but do NOT copy the characters.
     */
    private IndexedJsonValue nextStringValue() throws JsonParseException {
        int stringStart = idx; // idx is right after the opening quote
        boolean hasEscapes = false;
        while (true) {
            if (idx >= len) {
                throw new JsonParseException("Unterminated string.");
            }
            char c = json[idx++];
            switch (c) {
                case '\n':
                case '\r':
                    throw new JsonParseException("Unterminated string.");
                case '\\':
                    hasEscapes = true;
                    if (idx >= len) {
                        throw new JsonParseException("Unterminated string.");
                    }
                    char esc = json[idx++];
                    if (esc == 'u') {
                        // validate 4 hex digits
                        for (int i = 0; i < 4; i++) {
                            if (idx >= len) {
                                throw new JsonParseException(JsonParseException.ILLEGAL_ESCAPE);
                            }
                            char h = json[idx++];
                            if (!((h >= '0' && h <= '9') || (h >= 'A' && h <= 'F') || (h >= 'a' && h <= 'f'))) {
                                throw new JsonParseException(JsonParseException.ILLEGAL_ESCAPE);
                            }
                        }
                    }
                    else {
                        switch (esc) {
                            case 'b': case 't': case 'n': case 'f': case 'r':
                            case '"': case '\'': case '\\': case '/':
                                break;
                            default:
                                throw new JsonParseException(JsonParseException.ILLEGAL_ESCAPE);
                        }
                    }
                    break;
                case '"':
                    int stringEnd = idx - 1; // exclude the closing quote
                    // Reset tokenizer state so subsequent peekToken/nextToken work correctly
                    current = '"';
                    previous = 0;
                    next = 0;
                    nextIdx = -1;
                    return new IndexedJsonValue(JsonValueType.STRING, json, stringStart, stringEnd, hasEscapes);
            }
        }
    }

    /**
     * Extract a map key string. Keys must be materialized for HashMap lookup.
     * This uses the same validation logic as nextStringValue but copies into keyBuffer.
     */
    private String nextKeyString() throws JsonParseException {
        keyBuffer.setLength(0);
        while (true) {
            char c = nextChar();
            switch (c) {
                case 0:
                case '\n':
                case '\r':
                    throw new JsonParseException("Unterminated string.");
                case '\\':
                    c = nextChar();
                    switch (c) {
                        case 'b':  keyBuffer.append('\b'); break;
                        case 't':  keyBuffer.append('\t'); break;
                        case 'n':  keyBuffer.append('\n'); break;
                        case 'f':  keyBuffer.append('\f'); break;
                        case 'r':  keyBuffer.append('\r'); break;
                        case 'u':  keyBuffer.append(parseU()); break;
                        case '"':
                        case '\'':
                        case '\\':
                        case '/':
                            keyBuffer.append(c);
                            break;
                        default:
                            throw new JsonParseException(JsonParseException.ILLEGAL_ESCAPE);
                    }
                    break;
                default:
                    if (c == '"') {
                        return keyBuffer.toString();
                    }
                    keyBuffer.append(c);
            }
        }
    }

    private char[] parseU() throws JsonParseException {
        int code = 0;
        for (int i = 0; i < 4; i++) {
            char c = nextToken();
            if (c == 0) {
                throw new JsonParseException(JsonParseException.ILLEGAL_ESCAPE);
            }
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            }
            else if (c >= 'A' && c <= 'F') {
                digit = c - 'A' + 10;
            }
            else if (c >= 'a' && c <= 'f') {
                digit = c - 'a' + 10;
            }
            else  {
                throw new JsonParseException(JsonParseException.ILLEGAL_ESCAPE);
            }
            code = (code << 4) | digit;
        }
        return Character.toChars(code);
    }

    private IndexedJsonValue nextArray() throws JsonParseException {
        List<IndexedJsonValue> list = new ArrayList<>(8);
        char p = peekToken();
        while (p != ']') {
            if (p == ',') {
                nextToken();
            }
            else {
                list.add(nextValue());
            }
            p = peekToken();
        }
        nextToken(); // consume ']'
        return list.isEmpty() ? IndexedJsonValue.EMPTY_ARRAY : new IndexedJsonValue(list);
    }

    private IndexedJsonValue nextPrimitiveValue() throws JsonParseException {
        // Record the start/end offsets of the primitive token.
        // Primitive chars (true/false/null/numbers) are always contiguous
        // (no internal whitespace), so idx after each nextToken() is one
        // past the consumed char.
        int primStart = -1;
        int primEnd = 0;
        char c = peekToken();
        while (c >= ' ' && isNotDelimiter(c)) {
            if (primStart == -1) {
                // The peeked char is at json[nextIdx - 1]
                primStart = nextIdx - 1;
            }
            nextToken();
            primEnd = idx; // exclusive end: one past the last consumed char
            c = peekToken();
        }

        if (primStart == -1) {
            throw new JsonParseException();
        }

        // Check for true/false/null
        int primLen = primEnd - primStart;
        if (primLen == 4) {
            if (json[primStart] == 't' && json[primStart + 1] == 'r' && json[primStart + 2] == 'u' && json[primStart + 3] == 'e') {
                return IndexedJsonValue.TRUE;
            }
            if (json[primStart] == 'n' && json[primStart + 1] == 'u' && json[primStart + 2] == 'l' && json[primStart + 3] == 'l') {
                return IndexedJsonValue.NULL;
            }
        }
        else if (primLen == 5) {
            if (json[primStart] == 'f' && json[primStart + 1] == 'a' && json[primStart + 2] == 'l'
                && json[primStart + 3] == 's' && json[primStart + 4] == 'e') {
                return IndexedJsonValue.FALSE;
            }
        }

        // Validate it looks like a number
        char initial = json[primStart];
        if ((initial >= '0' && initial <= '9') || initial == '-') {
            // Cheap structural validation only — no String allocation.
            // Full number parsing is deferred to IndexedJsonValue.ensureNumber().
            validateNumberStructure(json, primStart, primLen, primEnd);
            // In INTEGERS_ONLY mode, skip the decimal indicator scan entirely
            JsonValueType numType = integersOnly ? JsonValueType.INTEGER
                : (hasDecimalIndicator(json, primStart, primEnd) ? JsonValueType.BIG_DECIMAL : JsonValueType.INTEGER);
            return new IndexedJsonValue(numType, json, primStart, primEnd, false, integersOnly);
        }

        throw new JsonParseException();
    }

    private IndexedJsonValue nextObject() throws JsonParseException {
        Map<String, IndexedJsonValue> map = new HashMap<>(8);
        String key;
        while (true) {
            char c = nextToken();
            switch (c) {
                case 0:
                    throw new JsonParseException("Text must end with '}'");
                case '}':
                    return map.isEmpty() ? IndexedJsonValue.EMPTY_MAP : new IndexedJsonValue(map);
                case '{':
                case '[':
                    if (previous == '{') {
                        throw new JsonParseException("Cannot directly nest another Object or Array.");
                    }
                    // fall through
                default:
                    key = nextKeyString();
            }

            c = nextToken();
            if (c != ':') {
                throw new JsonParseException("Expected a ':' after a key.");
            }

            IndexedJsonValue value = nextValue();
            if (value != IndexedJsonValue.NULL || keepNulls) {
                map.put(key, value);
            }

            switch (nextToken()) {
                case ',':
                    if (peekToken() == '}') {
                        return map.isEmpty() ? IndexedJsonValue.EMPTY_MAP : new IndexedJsonValue(map);
                    }
                    break;
                case '}':
                    return map.isEmpty() ? IndexedJsonValue.EMPTY_MAP : new IndexedJsonValue(map);
                default:
                    throw new JsonParseException("Expected a ',' or '}'.");
            }
        }
    }

    // ---- number validation (cheap structural check, no String allocation) ----

    /**
     * Validates number structure without allocating a String or doing full parsing.
     * Only checks for leading-zero/octal violations that the original parser rejects.
     * Full parse validation happens lazily in IndexedJsonValue.ensureNumber().
     */
    private static void validateNumberStructure(char[] json, int start, int len, int end) throws JsonParseException {
        // The original JsonParser only rejects leading zeros for non-decimal numbers.
        // Skip this check if the number contains a decimal indicator (., e, E).
        if (hasDecimalIndicator(json, start, end)) {
            return;
        }

        char initial = json[start];
        // block items like 00 01 etc. Java number parsers treat these as Octal.
        if (initial == '0' && len > 1) {
            char at1 = json[start + 1];
            if (at1 >= '0' && at1 <= '9') {
                throw new JsonParseException();
            }
        }
        else if (initial == '-' && len > 2) {
            char at1 = json[start + 1];
            char at2 = json[start + 2];
            if (at1 == '0' && at2 >= '0' && at2 <= '9') {
                throw new JsonParseException();
            }
        }
    }

    /**
     * Check for decimal notation indicators directly on the char array.
     * Equivalent to isDecimalNotation but avoids String allocation.
     */
    private static boolean hasDecimalIndicator(char[] json, int start, int end) {
        // Also treat "-0" as decimal (same as original parser)
        if (end - start == 2 && json[start] == '-' && json[start + 1] == '0') {
            return true;
        }
        for (int i = start; i < end; i++) {
            char c = json[i];
            if (c == '.' || c == 'e' || c == 'E') {
                return true;
            }
        }
        return false;
    }

    // ---- tokenizer (identical to JsonParser) ----

    private char nextToken() {
        peekToken();
        idx = nextIdx;
        nextIdx = -1;
        previous = current;
        current = next;
        next = 0;
        return current;
    }

    private char nextChar() {
        previous = current;
        if (idx == len) {
            current = 0;
        }
        else {
            current = json[idx++];
        }
        next = 0;
        nextIdx = -1;
        return current;
    }

    private char peekToken() {
        if (nextIdx == -1) {
            nextIdx = idx;
            next = 0;
            while (nextIdx < len) {
                char c = json[nextIdx++];
                switch (c) {
                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                        continue;
                }
                return next = c;
            }
        }
        return next;
    }

    private boolean isNotDelimiter(char c) {
        return c < 128 && !IS_DELIMITER[c];
    }

    private static char[] bytesToChars(byte[] json) throws JsonParseException {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(json)).array();
        }
        catch (CharacterCodingException e) {
            throw new JsonParseException(e);
        }
    }
}
