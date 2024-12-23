package com.learning.english.unit;

import com.learning.english.utils.Base64Util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Base64UtilTest {

    @Test
    void shouldEncodeDataToBase64() {
        String originalData = "Hello World";

        String encodedData = Base64Util.encode(originalData);

        assertNotNull(encodedData);
        assertEquals("SGVsbG8gV29ybGQ=", encodedData);
    }

    @Test
    void shouldDecodeBase64ToOriginalData() {
        String encodedData = "SGVsbG8gV29ybGQ=";

        String decodedData = Base64Util.decode(encodedData);

        assertNotNull(decodedData);
        assertEquals("Hello World", decodedData);
    }

    @Test
    void shouldEncodeAndDecodeRoundtrip() {
        String originalData = "Test String";

        String encodedData = Base64Util.encode(originalData);
        String decodedData = Base64Util.decode(encodedData);

        assertEquals(originalData, decodedData);
    }

    @Test
    void shouldHandleEmptyString() {
        String emptyString = "";

        String encodedData = Base64Util.encode(emptyString);
        String decodedData = Base64Util.decode(encodedData);

        assertEquals(emptyString, decodedData);
    }

    @Test
    void shouldHandleNullData() {
        String nullString = null;

        assertThrows(NullPointerException.class, () -> {
            Base64Util.encode(nullString);
        });

        assertThrows(NullPointerException.class, () -> {
            Base64Util.decode(nullString);
        });
    }
}
