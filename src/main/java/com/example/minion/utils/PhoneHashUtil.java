package com.example.minion.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PhoneHashUtil {
    public static String formatPhoneNumber(long number) {
        return String.format("05%d-%07d", (number / 10000000) % 10, number % 10000000);
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash input: " + e.getMessage());
        }
    }
}
