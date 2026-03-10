package com.alibaba.polardbx.batchweb.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 密码加密工具
 */
@Slf4j
@Component
public class PasswordEncryptor {

    private static final String ALGORITHM = "AES";

    @Value("${encrypt.secret-key:BatchToolWebSecretKey}")
    private String secretKey;

    private static String staticSecretKey;

    @PostConstruct
    public void init() {
        staticSecretKey = secretKey;
    }

    /**
     * 加密
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            SecretKeySpec keySpec = getKeySpec();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("加密失败: " + e.getMessage());
        }
    }

    /**
     * 解密
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            SecretKeySpec keySpec = getKeySpec();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("解密失败: " + e.getMessage());
        }
    }

    private static SecretKeySpec getKeySpec() {
        // 确保密钥长度为16字节（AES-128）
        String key = staticSecretKey != null ? staticSecretKey : "BatchToolWebSecretKey";
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] validKeyBytes = new byte[16];
        System.arraycopy(keyBytes, 0, validKeyBytes, 0, Math.min(keyBytes.length, 16));
        return new SecretKeySpec(validKeyBytes, ALGORITHM);
    }
}
