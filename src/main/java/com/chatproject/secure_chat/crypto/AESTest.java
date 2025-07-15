package com.chatproject.secure_chat.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class AESTest {
    public static void main(String[] args) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey secretKey = keyGenerator.generateKey();
            String key = "hellohellohelloo";
            String message = "Hello AES World!";

            String encrypted = AESUtil.encrypt(message, secretKey);
            System.out.println("🔐 Encrypted: " + encrypted);

            String decrypted = AESUtil.decrypt(encrypted,secretKey);
            System.out.println("🔓 Decrypted: " + decrypted);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
