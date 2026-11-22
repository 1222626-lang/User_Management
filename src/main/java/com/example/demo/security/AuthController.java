package com.example.demo.security;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;

import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.CompanyStatus;
import com.example.demo.entity.User;

import com.example.demo.repository.UserRepository;

import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService,
            UserRepository userRepository,
            RefreshTokenService refreshTokenService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    // ==========================================
    // REGISTER
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ==========================================
    // LOGIN
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        // Validate request.

        if (request == null
                || request.username() == null
                || request.username().isBlank()
                || request.password() == null
                || request.password().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username and password are required"
            );
        }

        String username = request.username().trim();

        // Find account.

        User account = userRepository
                .findWithCompanyByUsernameIgnoreCaseAndIsDeletedFalse(
                        username
                )
                .orElse(null);

        if (account == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        // Check account status.

        if (account.getStatus() == AccountStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your registration is waiting for admin approval"
            );
        }

        if (account.getStatus() == AccountStatus.REJECTED) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your registration request was rejected"
            );
        }

        if (account.getStatus() != AccountStatus.APPROVED) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is not approved"
            );
        }

        // Check company.

        if (account.getCompany() == null
                || account.getCompany().getStatus()
                != CompanyStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your company is disabled"
            );
        }

        // Check role.

        if (account.getRole() == null) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No role assigned to this account"
            );
        }

        // Authenticate username and password.

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                request.password()
                        )
                );

        // Load authenticated account.

        User user = userRepository
                .findWithCompanyByUsernameIgnoreCaseAndIsDeletedFalse(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );

        // Generate access token.

        String accessToken =
                jwtService.generateToken(user);

        // Generate refresh token.

        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        // Return login response.

        return ResponseEntity.ok(
                Map.of(
                        "accessToken", accessToken,
                        "refreshToken", refreshToken,
                        "tokenType", "Bearer",
                        "username", user.getUsername(),
                        "role", user.getRole().getName()
                )
        );
    }

    // ==========================================
    // LOGIN REQUEST DTO
    // ==========================================

    public record LoginRequest(
            String username,
            String password
    ) {
    }
}