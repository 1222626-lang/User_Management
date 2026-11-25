package com.example.demo.service;

import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;

import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RoleRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(1)
public class AuthorizationInitializer
        implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public AuthorizationInitializer(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {

        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // 1. DEFINE PERMISSIONS

        List<String> permissionNames = List.of(

                "USER_PROFILE_READ",
                "USER_PROFILE_UPDATE",

                "USER_READ",
                "USER_CREATE",
                "USER_UPDATE",
                "USER_DELETE",
                "USER_RESTORE",

                "USER_APPROVAL_READ",
                "USER_APPROVAL_MANAGE",

                "COMPANY_READ",
                "COMPANY_CREATE",
                "COMPANY_UPDATE",
                "COMPANY_DELETE",

                "AUTHORIZATION_MANAGE"
        );

        // 2. CREATE MISSING PERMISSIONS
        for (String name : permissionNames) {

            if (!permissionRepository
                    .existsByNameIgnoreCase(name)) {

                Permission permission = new Permission(
                        name,
                        name.replace('_', ' ')
                );

                permissionRepository.save(permission);
            }
        }

        // 3. LOAD ALL PERMISSIONS

        Set<Permission> allPermissions =
                permissionRepository.findAll()
                        .stream()
                        .collect(Collectors.toSet());

        // 4. CREATE OR UPDATE ADMIN ROLE

        Role admin = roleRepository
                .findByNameIgnoreCase("ADMIN")
                .orElseGet(() -> new Role(
                        "ADMIN",
                        "System administrator"
                ));

        // Ensure ADMIN has all permissions.

        admin.getPermissions().addAll(allPermissions);

        roleRepository.save(admin);

        // 5. CREATE OR UPDATE USER ROLE

        Role user = roleRepository
                .findByNameIgnoreCase("USER")
                .orElseGet(() -> new Role(
                        "USER",
                        "Standard user"
                ));

        // Standard users can access their own profile.

        permissionRepository
                .findByNameIgnoreCase("USER_PROFILE_READ")
                .ifPresent(
                        user.getPermissions()::add
                );

        permissionRepository
                .findByNameIgnoreCase("USER_PROFILE_UPDATE")
                .ifPresent(
                        user.getPermissions()::add
                );

        roleRepository.save(user);

        // 6. INITIALIZATION COMPLETED

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "AUTHORIZATION INITIALIZATION COMPLETED"
        );

        System.out.println(
                "ADMIN permissions: "
                        + admin.getPermissions().size()
        );

        System.out.println(
                "USER permissions: "
                        + user.getPermissions().size()
        );

        System.out.println(
                "=========================================="
        );
    }
}