
package com.example.demo.service;

import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.User;
import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.CompanyStatus;

import com.example.demo.repository.RefreshTokenRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository) {
        this.repository = repository;
    }

    // Generate secure random token
    private String generateToken() {

        byte[] bytes = new byte[64];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // Hash token using SHA-256
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not hash refresh token", e);
        }
    }

    // Create and save refresh token
    @Transactional
    public String createRefreshToken(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        // Do not create tokens for unapproved users.
        if (user.isDeleted()
                || user.getStatus() != AccountStatus.APPROVED
                || user.getCompany() == null
                || user.getCompany().getStatus()
                != CompanyStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Account is not active or not approved");
        }

        String rawToken = generateToken();

        RefreshToken refreshToken = new RefreshToken(
                hashToken(rawToken),
                user,
                LocalDateTime.now().plusDays(7)
        );

        repository.save(refreshToken);

        // Return raw token to the client.
        // Only its hash is stored in the database.
        return rawToken;
    }

    // Verify refresh token
    @Transactional(readOnly = true)
    public User verifyToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh token is required");
        }

        String hash = hashToken(rawToken);

        RefreshToken token = repository
                .findByTokenHash(hash)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid refresh token"));

        // Check revocation and expiration.
        if (token.isRevoked()) {
            throw new IllegalArgumentException(
                    "Refresh token has been revoked");
        }

        if (token.getExpiryDate() == null
                || !token.getExpiryDate().isAfter(
                LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Refresh token has expired");
        }

        User user = token.getUser();

        if (user == null) {
            throw new IllegalArgumentException(
                    "Refresh token has no associated user");
        }

        // Check user account status.
        if (user.isDeleted()
                || user.getStatus() != AccountStatus.APPROVED) {

            throw new IllegalArgumentException(
                    "User account is not active or approved");
        }

        // Check company status.
        if (user.getCompany() == null
                || user.getCompany().getStatus()
                != CompanyStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "User company is not active");
        }

        return user;
    }

    // Revoke refresh token
    @Transactional
    public void revokeToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh token is required");
        }

        String hash = hashToken(rawToken);

        repository.findByTokenHash(hash)
                .ifPresent(token -> token.setRevoked(true));
    }
}
