package com.example.demo.service;

import com.example.demo.entity.Company;
import com.example.demo.entity.CompanyStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;

import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String username;

    @Value("${app.admin.password:}")
    private String password;

    public AdminInitializer(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {

        if (username == null || username.isBlank()
                || password == null || password.length() < 12) {

            System.out.println(
                    "Admin bootstrap skipped: configure admin credentials.");
            return;
        }

        if (userRepository.existsByUsernameIgnoreCase(username.trim())) {
            System.out.println("Admin account already exists.");
            return;
        }

        Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ADMIN role was not initialized"));

        String systemEmail = "system-admin-company@example.invalid";

        Company company = companyRepository
                .findByEmailIgnoreCase(systemEmail)
                .orElseGet(() -> {

                    Company newCompany = new Company(
                            "System Administration",
                            systemEmail,
                            "System"
                    );

                    newCompany.setStatus(CompanyStatus.ACTIVE);

                    return companyRepository.save(newCompany);
                });

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            company.setStatus(CompanyStatus.ACTIVE);
            companyRepository.save(company);
        }

        User admin = new User();

        admin.setUsername(username.trim().toLowerCase());
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setEmail("system-admin@example.invalid");
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(adminRole);
        admin.setCompany(company);

        userRepository.save(admin);

        System.out.println("Initial ADMIN account created successfully.");
    }
}