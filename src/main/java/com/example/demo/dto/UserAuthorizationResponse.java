package com.example.demo.dto;

import java.util.Set;

public record UserAuthorizationResponse(

        Long userId,

        String username,

        String roleName,

        Set<String> rolePermissions,

        Set<UserPermissionOverrideResponse> userOverrides,

        Set<String> effectivePermissions

) {
}