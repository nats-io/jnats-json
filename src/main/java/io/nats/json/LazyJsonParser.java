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
 * A deeply indexed JSON parser that defers both leaf materialization AND
 * structural parsing of nested objects and arrays.
 * <p>
 * When parsing, nested {@code {...}} and {@code [...]} are not recursed into.
 * Instead, a brace/bracket-counting skip scan records start/end offsets.
 * The children are parsed one level deep on demand when
 * {@link LazyJsonValue#getMap()} or {@link LazyJsonValue#getArray()}
 * is called.
 * <p>
 * The top-level {@code parse()} call shallow-parses the outermost container
 * (one level), so the returned value's {@code getMap()}/{@code getArray()}
 * works immediately.
 */
public class LazyJsonParser {

    private static final boolean[] IS_DELIMITER = new boolean[128];

    static {
        for (char c : ",:]}/\\\"[{;=#".toCharArray()) {
            IS_DELIMITER[c] = true;
        }
    }

    // ---- static entry points ----

    @NonNull
    public static LazyJsonValue parse(char @Nullable [] json) throws JsonParseException {
        return new LazyJsonParser(json, 0, json == null ? 0 : json.length, false, true).parse();
    }

    @NonNull
    public static LazyJsonValue parse(char @Nullable [] json, int startIndex) throws JsonParseException {
        return new LazyJsonParser(json, startIndex, json == null ? 0 : json.length, false, true).parse();
    }

    @NonNull
    public static LazyJsonValue parse(char @Nullable [] json, JsonParser.@Nullable Option... options) throws JsonParseException {
        boolean[] flags = parseOptions(options);
        int len = json == null ? 0 : json.length;
        return new LazyJsonParser(json, 0, len, flags[0], flags[1]).parse();
    }

    @NonNull
    public static LazyJsonValue parse(String json) throws JsonParseException {
        char[] chars = json.toCharArray();
        return new LazyJsonParser(chars, 0, chars.length, false, true).parse();
    }

    @NonNull
    public static LazyJsonValue parse(String json, int startIndex) throws JsonParseException {
        char[] chars = json.toCharArray();
        return new LazyJsonParser(chars, startIndex, chars.length, false, true).parse();
    }

    @NonNull
    public static LazyJsonValue parse(String json, JsonParser.@Nullable Option... options) throws JsonParseException {
        boolean[] flags = parseOptions(options);
        char[] chars = json.toCharArray();
        return new LazyJsonParser(chars, 0, chars.length, flags[0], flags[1]).parse();
    }

    @NonNull
    public static LazyJsonValue parse(byte[] json) throws JsonParseException {
        char[] chars = bytesToChars(json);
        return new LazyJsonParser(chars, 0, chars.length, false, true).parse();
    }

    @NonNull
    public static LazyJsonValue parse(byte[] json, JsonParser.@Nullable Option... options) throws JsonParseException {
        boolean[] flags = parseOptions(options);
        char[] chars = bytesToChars(json);
        return new LazyJsonParser(chars, 0, chars.length, flags[0], flags[1]).parse();
    }

    @NonNull
    public static LazyJsonValue parseUnchecked(String json) {
        try { return parse(json); }
        catch (JsonParseException j) { throw new RuntimeException(j); }
    }

    @NonNull
    public static LazyJsonValue parseUnchecked(String json, JsonParser.@Nullable Option... options) {
        try { return parse(json, options); }
        catch (JsonParseException j) { throw new RuntimeException(j); }
    }

    @NonNull
    public static LazyJsonValue parseUnchecked(char @Nullable [] json) {
        try { return parse(json); }
        catch (JsonParseException j) { throw new RuntimeException(j); }
    }

    @NonNull
    public static LazyJsonValue parseUnchecked(byte[] json) {
        try { return parse(json); }
        catch (JsonParseException j) { throw new RuntimeException(j); }
    }

    // ---- package-private: used by LazyJsonValue for lazy resolution ----

    static Map<String, LazyJsonValue> shallowParseObject(
        char[] json, int start, int end, boolean keepNulls, boolean integersOnly) {
        try {
            LazyJsonParser p = new LazyJsonParser(json, start, end, keepNulls, integersOnly);
            return p.nextObject();
        }
        catch (JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    static List<LazyJsonValue> shallowParseArray(
        char[] json, int start, int end, boolean keepNulls, boolean integersOnly) {
        try {
            LazyJsonParser p = new LazyJsonParser(json, start, end, keepNulls, integersOnly);
            return p.nextArray();
        }
        catch (JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    // ---- instance fields ----

    private final char @NonNull [] json;
    private final boolean keepNulls;
    private final boolean integersOnly;
    private final int endBound; // exclusive upper bound for scanning
    private int idx;
    private int nextIdx;
    private char previous;
    private char current;
    private char next;
    private final StringBuilder keyBuffer = new StringBuilder(64);

    private LazyJsonParser(char @Nullable [] json, int startIndex, int endBound,
                                  boolean keepNulls, boolean integersOnly) {
        this.keepNulls = keepNulls;
        this.integersOnly = integersOnly;
        if (json == null) {
            this.json = new char[0];
            this.endBound = 0;
        }
        else {
            this.json = json;
            this.endBound = endBound;
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

    // ---- top-level parse ----

    @NonNull
    private LazyJsonValue parse() throws JsonParseException {
        char c = peekToken();
        if (c == 0) return LazyJsonValue.NULL;
        if (c == '"') {
            nextToken();
            return nextStringValue();
        }
        if (c == '{') {
            nextToken();
            Map<String, LazyJsonValue> map = nextObject();
            return map.isEmpty() ? LazyJsonValue.EMPTY_MAP : new LazyJsonValue(map);
        }
        if (c == '[') {
            nextToken();
            List<LazyJsonValue> list = nextArray();
            return list.isEmpty() ? LazyJsonValue.EMPTY_ARRAY : new LazyJsonValue(list);
        }
        return nextPrimitiveValue();
    }

    // ---- one-level parse of object/array (child containers are SKIPPED) ----

    /**
     * Parse one level of a JSON object. Called after the opening '{' has been
     * consumed. For each value, if it is a nested container ({...} or [...]),
     * it is skip-scanned and stored as an unresolved LazyJsonValue.
     */
    private Map<String, LazyJsonValue> nextObject() throws JsonParseException {
        Map<String, LazyJsonValue> map = new HashMap<>(8);
        String key;
        while (true) {
            char c = nextToken();
            switch (c) {
                case 0:
                    throw new JsonParseException("Text must end with '}'");
                case '}':
                    return map;
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
            if (c != ':') throw new JsonParseException("Expected a ':' after a key.");

            LazyJsonValue value = nextChildValue();
            if (value != LazyJsonValue.NULL || keepNulls) {
                map.put(key, value);
            }

            switch (nextToken()) {
                case ',':
                    if (peekToken() == '}') return map;
                    break;
                case '}':
                    return map;
                default:
                    throw new JsonParseException("Expected a ',' or '}'.");
            }
        }
    }

    private List<LazyJsonValue> nextArray() throws JsonParseException {
        List<LazyJsonValue> list = new ArrayList<>(8);
        char p = peekToken();
        while (p != ']') {
            if (p == ',') {
                nextToken();
            }
            else {
                list.add(nextChildValue());
            }
            p = peekToken();
        }
        nextToken(); // consume ']'
        return list;
    }

    /**
     * Parse the next value as a child of a container. Leaf values (strings,
     * numbers, booleans, null) are handled normally. Nested containers are
     * SKIPPED — only their byte-range is recorded.
     */
    private LazyJsonValue nextChildValue() throws JsonParseException {
        char c = peekToken();
        if (c == 0) throw new JsonParseException("Unexpected end of data.");
        if (c == '"') {
            nextToken();
            return nextStringValue();
        }
        if (c == '{') {
            nextToken();
            return skipObjectValue();
        }
        if (c == '[') {
            nextToken();
            return skipArrayValue();
        }
        return nextPrimitiveValue();
    }

    /**
     * Skip a nested object value. The opening '{' has been consumed.
     * Records the byte-range and returns an unresolved LazyJsonValue.
     */
    private LazyJsonValue skipObjectValue() throws JsonParseException {
        int contentStart = idx; // right after the '{'
        skipContainerContent();
        int contentEnd = idx; // idx is right after the closing '}' — include '}' in the range
        // Reset tokenizer state so the caller's nextToken/peekToken works
        current = '}';
        previous = 0;
        next = 0;
        nextIdx = -1;
        if (contentStart == contentEnd - 1) {
            // Only the '}' — empty object
            return LazyJsonValue.EMPTY_MAP;
        }
        return new LazyJsonValue(JsonValueType.MAP, json, contentStart, contentEnd,
            integersOnly, keepNulls, true);
    }

    private LazyJsonValue skipArrayValue() throws JsonParseException {
        int contentStart = idx; // right after the '['
        skipContainerContent();
        int contentEnd = idx; // idx is right after the closing ']' — include ']' in the range
        current = ']';
        previous = 0;
        next = 0;
        nextIdx = -1;
        if (contentStart == contentEnd - 1) {
            return LazyJsonValue.EMPTY_ARRAY;
        }
        return new LazyJsonValue(JsonValueType.ARRAY, json, contentStart, contentEnd,
            integersOnly, keepNulls, true);
    }

    /**
     * Skip over the content of a container. Called right after the opening
     * brace/bracket has been consumed. Scans forward counting depth, handling
     * strings. Exits with idx pointing right after the closing brace/bracket.
     */
    private void skipContainerContent() throws JsonParseException {
        int depth = 1;
        while (idx < endBound) {
            char c = json[idx++];
            switch (c) {
                case '{':
                case '[':
                    depth++;
                    break;
                case '}':
                case ']':
                    depth--;
                    if (depth == 0) return;
                    break;
                case '"':
                    // Skip string content — just need to handle \" correctly
                    while (idx < endBound) {
                        char sc = json[idx++];
                        if (sc == '"') break;
                        if (sc == '\\') {
                            if (idx < endBound) idx++; // skip escaped char
                        }
                    }
                    break;
                // All other chars: skip
            }
        }
        throw new JsonParseException("Unterminated container.");
    }

    // ---- string value (offset recording, same as IndexedJsonParser) ----

    private LazyJsonValue nextStringValue() throws JsonParseException {
        int stringStart = idx;
        boolean hasEscapes = false;
        while (true) {
            if (idx >= endBound) throw new JsonParseException("Unterminated string.");
            char c = json[idx++];
            switch (c) {
                case '\n':
                case '\r':
                    throw new JsonParseException("Unterminated string.");
                case '\\':
                    hasEscapes = true;
                    if (idx >= endBound) throw new JsonParseException("Unterminated string.");
                    char esc = json[idx++];
                    if (esc == 'u') {
                        for (int i = 0; i < 4; i++) {
                            if (idx >= endBound) throw new JsonParseException("Illegal escape.");
                            char h = json[idx++];
                            if (!((h >= '0' && h <= '9') || (h >= 'A' && h <= 'F') || (h >= 'a' && h <= 'f')))
                                throw new JsonParseException("Illegal escape.");
                        }
                    }
                    else {
                        switch (esc) {
                            case 'b': case 't': case 'n': case 'f': case 'r':
                            case '"': case '\'': case '\\': case '/': break;
                            default: throw new JsonParseException("Illegal escape.");
                        }
                    }
                    break;
                case '"':
                    int stringEnd = idx - 1;
                    current = '"';
                    previous = 0;
                    next = 0;
                    nextIdx = -1;
                    return new LazyJsonValue(JsonValueType.STRING, json, stringStart, stringEnd,
                        hasEscapes, integersOnly);
            }
        }
    }

    // ---- key string (must copy for HashMap lookup) ----

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
                        case '"': case '\'': case '\\': case '/':
                            keyBuffer.append(c); break;
                        default: throw new JsonParseException("Illegal escape.");
                    }
                    break;
                default:
                    if (c == '"') return keyBuffer.toString();
                    keyBuffer.append(c);
            }
        }
    }

    private char[] parseU() throws JsonParseException {
        int code = 0;
        for (int i = 0; i < 4; i++) {
            char c = nextToken();
            if (c == 0) throw new JsonParseException("Illegal escape.");
            int digit;
            if (c >= '0' && c <= '9') digit = c - '0';
            else if (c >= 'A' && c <= 'F') digit = c - 'A' + 10;
            else if (c >= 'a' && c <= 'f') digit = c - 'a' + 10;
            else throw new JsonParseException("Illegal escape.");
            code = (code << 4) | digit;
        }
        return Character.toChars(code);
    }

    // ---- primitive value ----

    private LazyJsonValue nextPrimitiveValue() throws JsonParseException {
        int primStart = -1;
        int primEnd = 0;
        char c = peekToken();
        while (c >= ' ' && isNotDelimiter(c)) {
            if (primStart == -1) primStart = nextIdx - 1;
            nextToken();
            primEnd = idx;
            c = peekToken();
        }
        if (primStart == -1) throw new JsonParseException();

        int primLen = primEnd - primStart;
        if (primLen == 4) {
            if (json[primStart] == 't' && json[primStart+1] == 'r' && json[primStart+2] == 'u' && json[primStart+3] == 'e')
                return LazyJsonValue.TRUE;
            if (json[primStart] == 'n' && json[primStart+1] == 'u' && json[primStart+2] == 'l' && json[primStart+3] == 'l')
                return LazyJsonValue.NULL;
        }
        else if (primLen == 5) {
            if (json[primStart] == 'f' && json[primStart+1] == 'a' && json[primStart+2] == 'l'
                && json[primStart+3] == 's' && json[primStart+4] == 'e')
                return LazyJsonValue.FALSE;
        }

        char initial = json[primStart];
        if ((initial >= '0' && initial <= '9') || initial == '-') {
            validateNumberStructure(json, primStart, primLen, primEnd);
            JsonValueType numType = integersOnly ? JsonValueType.INTEGER
                : (hasDecimalIndicator(json, primStart, primEnd) ? JsonValueType.BIG_DECIMAL : JsonValueType.INTEGER);
            return new LazyJsonValue(numType, json, primStart, primEnd, false, integersOnly);
        }
        throw new JsonParseException();
    }

    // ---- tokenizer (same as IndexedJsonParser, using endBound) ----

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
        if (idx == endBound) {
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
            while (nextIdx < endBound) {
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

    // ---- number validation (structural only) ----

    private static void validateNumberStructure(char[] json, int start, int len, int end) throws JsonParseException {
        if (hasDecimalIndicator(json, start, end)) return;

        char initial = json[start];
        if (initial == '0' && len > 1) {
            char at1 = json[start + 1];
            if (at1 >= '0' && at1 <= '9') throw new JsonParseException();
        }
        else if (initial == '-' && len > 2) {
            char at1 = json[start + 1];
            char at2 = json[start + 2];
            if (at1 == '0' && at2 >= '0' && at2 <= '9') throw new JsonParseException();
        }
    }

    private static boolean hasDecimalIndicator(char[] json, int start, int end) {
        if (end - start == 2 && json[start] == '-' && json[start + 1] == '0') return true;
        for (int i = start; i < end; i++) {
            char c = json[i];
            if (c == '.' || c == 'e' || c == 'E') return true;
        }
        return false;
    }

    // ---- options parsing ----

    private static boolean[] parseOptions(JsonParser.@Nullable Option[] options) {
        boolean keepNulls = false;
        boolean integersOnly = true; // default
        if (options != null) {
            for (JsonParser.Option opt : options) {
                if (opt == JsonParser.Option.KEEP_NULLS) keepNulls = true;
                else if (opt == JsonParser.Option.DECIMALS) integersOnly = false;
            }
        }
        return new boolean[]{keepNulls, integersOnly};
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
