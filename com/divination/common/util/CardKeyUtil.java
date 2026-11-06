package com.divination.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CardKeyUtil {

    /**
     * Generate a unique card key string with random characters.
     */
    public String generateCardKey() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            if (i < 3) sb.append('-');
        }
        return sb.toString();
    }
}
