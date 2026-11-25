package com.example.demo.service;

import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.CompanyStatus;
import com.example.demo.entity.Permission;
import com.example.demo.entity.PermissionEffect;
import com.example.demo.entity.User;
import com.example.demo.entity.UserPermission;

import com.example.demo.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class PermissionService {

    private final UserRepository userRepository;

    public PermissionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(String username) {

        User user = loadUser(username);

        Set<String> effective = new HashSet<>();

        // 1. Get default role permissions.

        if (user.getRole() != null
                && user.getRole().getPermissions() != null) {

            for (Permission permission :
                    user.getRole().getPermissions()) {

                effective.add(permission.getName());
            }
        }

        Set<String> denied = new HashSet<>();
        Set<String> granted = new HashSet<>();

        // 2. Get individual user overrides.

        for (UserPermission override :
                user.getPermissionOverrides()) {

            String permissionName =
                    override.getPermission().getName();

            if (override.getEffect() == PermissionEffect.DENY) {

                denied.add(permissionName);

            } else if (override.getEffect()
                    == PermissionEffect.GRANT) {

                granted.add(permissionName);
            }
        }

        // 3. Apply DENY.

        effective.removeAll(denied);

        // 4. Apply GRANT.

        effective.addAll(granted);

        return effective;
    }

     // Get Spring Security authorities.

    @Transactional(readOnly = true)
    public Set<String> getAuthorities(String username) {

        User user = loadUser(username);

        // Disabled users must have no authorities.

        if (user.getStatus() != AccountStatus.APPROVED
                || user.getCompany() == null
                || user.getCompany().getStatus()
                != CompanyStatus.ACTIVE) {

            return Set.of();
        }

        Set<String> authorities =
                new HashSet<>(
                        getEffectivePermissions(username)
                );

        // Add role authority.

        if (user.getRole() != null) {

            authorities.add(
                    "ROLE_" + user.getRole().getName()
            );
        }

        return authorities;
    }

    //Load user with all authorization relations.

    private User loadUser(String username) {

        return userRepository
                .findSecurityUserByUsernameIgnoreCaseAndIsDeletedFalse(
                        username
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }
}