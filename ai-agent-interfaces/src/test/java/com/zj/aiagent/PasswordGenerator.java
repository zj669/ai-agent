package com.zj.aiagent;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    @Test
    void generatePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "demo123456";
        String encoded = encoder.encode(rawPassword);
        System.out.println("Raw: " + rawPassword);
        System.out.println("Encoded: " + encoded);
        System.out.println("Verify: " + encoder.matches(rawPassword, encoded));
    }
}
