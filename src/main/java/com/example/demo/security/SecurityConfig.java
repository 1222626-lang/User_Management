package com.example.demo.security;

import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.CompanyStatus;
import com.example.demo.entity.User;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.PermissionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.convert.converter.Converter;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    // ==========================================
    // PASSWORD ENCODER
    // ==========================================

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // ==========================================
    // USER DETAILS SERVICE
    // ==========================================

    @Bean
    public UserDetailsService userDetailsService(
            UserRepository userRepository) {

        return username -> {

            User user = userRepository
                    .findWithCompanyByUsernameIgnoreCaseAndIsDeletedFalse(
                            username
                    )
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "User not found"
                            )
                    );

            if (user.getCompany() == null
                    || user.getCompany().getStatus()
                    != CompanyStatus.ACTIVE) {

                throw new UsernameNotFoundException(
                        "Company is disabled"
                );
            }

            boolean approved =
                    user.getStatus() == AccountStatus.APPROVED;

            if (user.getRole() == null) {

                throw new UsernameNotFoundException(
                        "User has no assigned role"
                );
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPassword())
                    .authorities(
                            "ROLE_" + user.getRole().getName()
                    )
                    .disabled(!approved)
                    .build();
        };
    }

    // ==========================================
    // AUTHENTICATION MANAGER
    // ==========================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    // ==========================================
    // JWT DECODER
    // ==========================================

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.jwt.secret}") String secret) {

        SecretKey key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // ==========================================
    // JWT AUTHORITIES CONVERTER
    // ==========================================

    /**
     * Load permissions from the database
     * for every authenticated JWT request.
     *
     * This ensures that changes to permissions
     * take effect without issuing a new JWT.
     */

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            PermissionService permissionService) {

        Converter<Jwt, Collection<GrantedAuthority>>
                authoritiesConverter = jwt -> {

            Set<String> names =
                    permissionService.getAuthorities(
                            jwt.getSubject()
                    );

            return names.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        };

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }

    // ==========================================
    // RATE LIMIT FILTER REGISTRATION
    // ==========================================

    @Bean
    public FilterRegistrationBean<RateLimitFilter>
    rateLimitFilterRegistration(RateLimitFilter filter) {

        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(filter);
        registration.setEnabled(false);

        return registration;
    }

    // ==========================================
    // SECURITY FILTER CHAIN
    // ==========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtConverter,
            RateLimitFilter rateLimitFilter)
            throws Exception {

        http

                // Disable CSRF for stateless JWT API.
                .csrf(csrf -> csrf.disable())

                // Stateless session.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // ==================================
                // REQUEST AUTHORIZATION
                // ==================================

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints.

                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/public/**"
                        ).permitAll()

                        // ==================================
                        // USER PROFILE
                        // ==================================

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/users/me",
                                "/api/users/me/**"
                        ).hasAuthority("USER_PROFILE_READ")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/users/me",
                                "/api/users/me/**"
                        ).hasAuthority("USER_PROFILE_UPDATE")

                        // ==================================
                        // USER MANAGEMENT
                        // ==================================

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/users",
                                "/api/users/page",
                                "/api/users/deleted",
                                "/api/users/deleted/**",
                                "/api/users/*"
                        ).hasAuthority("USER_READ")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/users"
                        ).hasAuthority("USER_CREATE")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/users/*"
                        ).hasAuthority("USER_UPDATE")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/users/*"
                        ).hasAuthority("USER_DELETE")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/users/*/restore"
                        ).hasAuthority("USER_RESTORE")

                        // ==================================
                        // USER APPROVAL
                        // ==================================

                        .requestMatchers(
                                "/api/admin/users/pending"
                        ).hasAuthority("USER_APPROVAL_READ")

                        .requestMatchers(
                                "/api/admin/users/*/approve",
                                "/api/admin/users/*/reject"
                        ).hasAuthority("USER_APPROVAL_MANAGE")

                        // ==================================
                        // COMPANY MANAGEMENT
                        // ==================================

                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/companies",
                                "/api/companies/page",
                                "/api/companies/*"
                        ).hasAuthority("COMPANY_READ")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/companies"
                        ).hasAuthority("COMPANY_CREATE")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/companies/*"
                        ).hasAuthority("COMPANY_UPDATE")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.PATCH,
                                "/api/companies/*"
                        ).hasAuthority("COMPANY_UPDATE")

                        .requestMatchers(
                                org.springframework.http.HttpMethod.DELETE,
                                "/api/companies/*"
                        ).hasAuthority("COMPANY_DELETE")

                        // ==================================
                        // AUTHORIZATION MANAGEMENT
                        // ==================================

                        .requestMatchers(
                                "/api/authorization/**"
                        ).hasAuthority("AUTHORIZATION_MANAGE")

                        // ==================================
                        // ALL OTHER REQUESTS
                        // ==================================

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable())

                // JWT Resource Server.

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtConverter
                                )
                        )
                )

                // Rate limit filter.

                .addFilterBefore(
                        rateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}