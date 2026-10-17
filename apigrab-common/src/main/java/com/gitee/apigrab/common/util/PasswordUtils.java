package com.gitee.apigrab.common.util;

import com.gitee.apigrab.common.constant.CommonConstant;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

public class PasswordUtils {

    private static final byte[] KEY = CommonConstant.AES_KEY.getBytes(StandardCharsets.UTF_8);
    private static final AES AES = SecureUtil.aes(KEY);

    /**
     * AES 加密
     */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        return AES.encryptBase64(plainText);
    }

    /**
     * AES 解密
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        return AES.decryptStr(encryptedText);
    }
}
