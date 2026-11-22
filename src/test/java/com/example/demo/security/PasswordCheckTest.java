package com.example.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordCheckTest {

    @Test
    void checkOmarPassword() {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String password = "omar123!";

        String storedHash =
                "$2a$10$VGmfZ83NgadJafEw9t.mpOBQ5QtJS3FxO9F/hvheF6IN5xJiq3GZu";

        assertTrue(
                encoder.matches(password, storedHash),
                "Password does not match the stored BCrypt hash"
        );
    }
}