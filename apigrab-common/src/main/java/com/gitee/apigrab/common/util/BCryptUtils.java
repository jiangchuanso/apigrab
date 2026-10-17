package com.gitee.apigrab.common.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * Password hashing for system users, backed by BCrypt. The generated hash embeds
 * its own salt, so no separate salt column is required.
 */
public final class BCryptUtils {

    private BCryptUtils() {
    }

    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        return BCrypt.hashpw(rawPassword);
    }

    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
