// Copyright 2025-2026 The NATS Authors
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

import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.nats.json.Encoding.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class EncodingTests {
    @Test
    public void testJsonEncodeDecode() {
        _testJsonEncodeDecode("b4\\\\after", "b4\\after", null); // a single slash with a meaningless letter after it
        _testJsonEncodeDecode("b4\\\\tafter", "b4\\tafter", null); // a single slash with a char that can be part of an escape

        _testJsonEncodeDecode("b4\\bafter", "b4\bafter", null);
        _testJsonEncodeDecode("b4\\fafter", "b4\fafter", null);
        _testJsonEncodeDecode("b4\\nafter", "b4\nafter", null);
        _testJsonEncodeDecode("b4\\rafter", "b4\rafter", null);
        _testJsonEncodeDecode("b4\\tafter", "b4\tafter", null);

        _testJsonEncodeDecode("b4\\u0000after", "b4" + (char) 0 + "after", null);
        _testJsonEncodeDecode("b4\\u001fafter", "b4" + (char) 0x1f + "after", "b4\\u001fafter");
        _testJsonEncodeDecode("b4\\u0020after", "b4 after", "b4 after");
        _testJsonEncodeDecode("b4\\u0022after", "b4\"after", "b4\\\"after");
        _testJsonEncodeDecode("b4\\u0027after", "b4'after", "b4'after");
        _testJsonEncodeDecode("b4\\u003dafter", "b4=after", "b4=after");
        _testJsonEncodeDecode("b4\\u003Dafter", "b4=after", "b4=after");
        _testJsonEncodeDecode("b4\\u003cafter", "b4<after", "b4<after");
        _testJsonEncodeDecode("b4\\u003Cafter", "b4<after", "b4<after");
        _testJsonEncodeDecode("b4\\u003eafter", "b4>after", "b4>after");
        _testJsonEncodeDecode("b4\\u003Eafter", "b4>after", "b4>after");
        _testJsonEncodeDecode("b4\\u0060after", "b4`after", "b4`after");
        _testJsonEncodeDecode("b4\\xafter", "b4xafter", "b4xafter"); // unknown escape
        _testJsonEncodeDecode("b4\\", "b4\\", "b4\\\\"); // last char is \
        _testJsonEncodeDecode("b4\\/after", "b4/after", null);
        _testJsonEncodeDecode("not-valid-u\\uu", "not-valid-uuu", "not-valid-uuu");

        List<String> utfs = ResourceUtils.resourceAsLines("utf8-only-no-ws-test-strings.txt");
        for (String u : utfs) {
            String uu = "b4\\b\\f\\n\\r\\t" + u + "after";
            _testJsonEncodeDecode(uu, "b4\b\f\n\r\t" + u + "after", uu);
        }

        assertEquals("", Encoding.jsonEncode((String)null));
        assertEquals("", Encoding.jsonEncode(new StringBuilder(), (String)null).toString());
        assertEquals("", Encoding.jsonEncode(new char[0]));
        assertEquals("", Encoding.jsonEncode(new StringBuilder(), new char[0]).toString());
    }

    private void _testJsonEncodeDecode(String encodedInput, String targetDecode, String targetEncode) {
        String decoded = jsonDecode(encodedInput);
        assertEquals(targetDecode, decoded);
        verifyEncode(encodedInput, targetEncode, jsonEncode(decoded));
        verifyEncode(encodedInput, targetEncode, jsonEncode(new StringBuilder(), decoded).toString());
        verifyEncode(encodedInput, targetEncode, jsonEncode(decoded.toCharArray()));
        verifyEncode(encodedInput, targetEncode, jsonEncode(new StringBuilder(), decoded.toCharArray()).toString());
    }

    private static void verifyEncode(String encodedInput, String targetEncode, String encoded) {
        if (targetEncode == null) {
            assertEquals(encodedInput, encoded);
        }
        else {
            assertEquals(targetEncode, encoded);
        }
    }

    @Test
    public void testBase64BasicEncoding() {
        String text = "blahblah";
        byte[] btxt = text.getBytes();

        byte[] encBytesFromBytes = base64BasicEncode(btxt);
        String encFromBytes = base64BasicEncodeToString(btxt);
        String encFromString = base64BasicEncodeToString(text);
        assertEquals("YmxhaGJsYWg=", new String(encBytesFromBytes));
        assertEquals("YmxhaGJsYWg=", encFromBytes);
        assertEquals("YmxhaGJsYWg=", encFromString);

        encFromString = base64BasicEncodeToString(text, StandardCharsets.US_ASCII);
        assertEquals("YmxhaGJsYWg=", encFromString);

        assertArrayEquals(btxt, base64BasicDecode(encBytesFromBytes));
        assertArrayEquals(btxt, base64BasicDecode(encFromBytes));
        assertEquals(text, base64BasicDecodeToString(encFromBytes));
        assertEquals(text, base64BasicDecodeToString(encFromBytes, StandardCharsets.US_ASCII));

        String data = ResourceUtils.resourceAsString("test_bytes_000100.txt");
        String check = ResourceUtils.resourceAsString("basic_encoded_000100.txt");
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        String enc = base64BasicEncodeToString(data.getBytes());
        assertEquals(check, enc);
        assertArrayEquals(bytes, base64BasicDecode(enc));

        data = ResourceUtils.resourceAsString("test_bytes_001000.txt");
        check = ResourceUtils.resourceAsString("basic_encoded_001000.txt");
        bytes = data.getBytes(StandardCharsets.UTF_8);
        enc = base64BasicEncodeToString(data.getBytes());
        assertEquals(check, enc);
        assertArrayEquals(bytes, base64BasicDecode(enc));

        data = ResourceUtils.resourceAsString("test_bytes_010000.txt");
        check = ResourceUtils.resourceAsString("basic_encoded_010000.txt");
        bytes = data.getBytes(StandardCharsets.UTF_8);
        enc = base64BasicEncodeToString(data.getBytes());
        assertEquals(check, enc);
        assertArrayEquals(bytes, base64BasicDecode(enc));

        data = ResourceUtils.resourceAsString("test_bytes_100000.txt");
        check = ResourceUtils.resourceAsString("basic_encoded_100000.txt");
        bytes = data.getBytes(StandardCharsets.UTF_8);
        enc = base64BasicEncodeToString(data.getBytes());
        assertEquals(check, enc);
        assertArrayEquals(bytes, base64BasicDecode(enc));
    }

    @Test
    public void testBase64UrlEncoding() {
        String text = "blahblah";
        byte[] btxt = text.getBytes();
        String surl = "https://nats.io/";
        byte[] burl = surl.getBytes();

        byte[] uencBytes = base64UrlEncode(btxt);
        assertEquals("YmxhaGJsYWg", base64UrlEncodeToString(text));
        assertEquals("YmxhaGJsYWg", base64UrlEncodeToString(text, StandardCharsets.US_ASCII));
        assertEquals("YmxhaGJsYWg", new String(uencBytes));
        assertEquals(text, base64UrlDecodeToString(uencBytes));
        assertArrayEquals(btxt, base64UrlDecode(uencBytes));
        assertEquals(text, base64UrlDecodeToString(new String(uencBytes)));
        assertArrayEquals(btxt, base64UrlDecode(new String(uencBytes)));

        uencBytes = base64UrlEncode(burl);
        assertEquals("aHR0cHM6Ly9uYXRzLmlvLw", base64UrlEncodeToString(surl));
        assertEquals("aHR0cHM6Ly9uYXRzLmlvLw", new String(uencBytes));
        assertArrayEquals(burl, base64UrlDecode(uencBytes));

        assertEquals("+ hello world", uriDecode("+%20hello%20world"));

        String str = ResourceUtils.resourceAsString("test_bytes_000100.txt");
        String check = ResourceUtils.resourceAsString("url_encoded_000100.txt");
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        String senc = base64UrlEncodeToString(str);
        assertEquals(check, senc);
        String benc = base64UrlEncodeToString(bytes);
        assertEquals(check, benc);
        assertArrayEquals(bytes, base64UrlDecode(benc));

        str = ResourceUtils.resourceAsString("test_bytes_001000.txt");
        check = ResourceUtils.resourceAsString("url_encoded_001000.txt");
        bytes = str.getBytes(StandardCharsets.UTF_8);
        senc = base64UrlEncodeToString(str);
        assertEquals(check, senc);
        benc = base64UrlEncodeToString(bytes);
        assertEquals(check, benc);
        assertArrayEquals(bytes, base64UrlDecode(benc));

        str = ResourceUtils.resourceAsString("test_bytes_010000.txt");
        check = ResourceUtils.resourceAsString("url_encoded_010000.txt");
        bytes = str.getBytes(StandardCharsets.UTF_8);
        senc = base64UrlEncodeToString(str);
        assertEquals(check, senc);
        benc = base64UrlEncodeToString(bytes);
        assertEquals(check, benc);
        assertArrayEquals(bytes, base64UrlDecode(benc));

        str = ResourceUtils.resourceAsString("test_bytes_100000.txt");
        check = ResourceUtils.resourceAsString("url_encoded_100000.txt");
        bytes = str.getBytes(StandardCharsets.UTF_8);
        senc = base64UrlEncodeToString(str);
        assertEquals(check, senc);
        benc = base64UrlEncodeToString(bytes);
        assertEquals(check, benc);
        assertArrayEquals(bytes, base64UrlDecode(benc));
    }
}
