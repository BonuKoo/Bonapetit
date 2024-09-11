package com.eatmate;


import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
    SecretKey 생성용
 */

public class KeyGenerator {

    //@Test
    public void testGenerateJwtKey(){
        // JWT 키 생성 테스트 로직
        String key = generateJwtKey(); // 메서드 생성해서 키를 만듦
        assertNotNull(key);
        System.out.println("Generated Key: " + key);
    }

    private String generateJwtKey() {
        byte[] key = new byte[32]; // 256비트
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

}
