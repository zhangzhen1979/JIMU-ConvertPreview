package com.thinkdifferent.convertpreview.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

/**
 * @author ltian
 * @version 1.0
 * @date 2024/1/12 15:50
 */
public class AesUtil {
    private static final AES aes = SecureUtil.aes(("c558Gq0YQK2QUlMc").getBytes(StandardCharsets.UTF_8));

    private AesUtil() {
    }

    public static String decryptStr(String str) {
        return aes.decryptStr(str);
    }

    public static String encryptStr(String str) {
        return aes.encryptHex(str);
    }
}
