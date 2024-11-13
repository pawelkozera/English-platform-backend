package com.learning.english.utils;

import java.util.Base64;

public class Base64Util {
    public static String encode(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    public static String decode(String encodedData) {
        return new String(Base64.getDecoder().decode(encodedData));
    }
}

