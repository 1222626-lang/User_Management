package com.example.demo.controller;

import com.example.demo.dto.BulkPermissionOverridesRequest;
import com.example.demo.dto.PermissionOverrideRequest;
import com.example.demo.dto.PermissionResponse;
import com.example.demo.dto.RolePermissionsRequest;
import com.example.demo.dto.RoleResponse;
import com.example.demo.dto.UserAuthorizationResponse;

import com.example.demo.service.AuthorizationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/authorization")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(
            AuthorizationService authorizationService) {

        this.authorizationService = authorizationService;
    }

    // ==========================================
    // GET ALL ROLES
    // ==========================================

    @GetMapping("/roles")
    public Set<RoleResponse> getRoles() {

        return authorizationService.getRoles();
    }

    // ==========================================
    // GET ALL PERMISSIONS
    // ==========================================

    @GetMapping("/permissions")
    public Set<PermissionResponse> getPermissions() {

        return authorizationService.getPermissions();
    }

    // ==========================================
    // UPDATE ROLE PERMISSIONS
    // ==========================================

    @PutMapping("/roles/{roleId}/permissions")
    public RoleResponse updateRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody RolePermissionsRequest request) {

        return authorizationService.updateRolePermissions(
                roleId,
                request.permissionNames()
        );
    }

    // ==========================================
    // GET USER PERMISSIONS
    // ==========================================

    @GetMapping("/users/{userId}/permissions")
    public UserAuthorizationResponse getUserPermissions(
            @PathVariable Long userId) {

        return authorizationService.getUserAuthorization(
                userId
        );
    }

    // ==========================================
    // SET ONE USER PERMISSION
    // ==========================================

    @PutMapping("/users/{userId}/permissions")
    public UserAuthorizationResponse setUserPermission(
            @PathVariable Long userId,
            @Valid @RequestBody PermissionOverrideRequest request) {

        return authorizationService.setUserPermission(
                userId,
                request
        );
    }

    // ==========================================
    // UPDATE MULTIPLE USER PERMISSIONS
    // ==========================================

    @PutMapping("/users/{userId}/permissions/bulk")
    public UserAuthorizationResponse replaceUserPermissionOverrides(
            @PathVariable Long userId,
            @Valid @RequestBody BulkPermissionOverridesRequest request) {

        return authorizationService.replaceUserPermissionOverrides(
                userId,
                request.overrides()
        );
    }

    // ==========================================
    // REMOVE USER PERMISSION OVERRIDE
    // ==========================================

    @DeleteMapping("/users/{userId}/permissions/{permissionId}")
    public ResponseEntity<Void> removeUserPermissionOverride(
            @PathVariable Long userId,
            @PathVariable Long permissionId) {

        authorizationService.removeUserPermissionOverride(
                userId,
                permissionId
        );

        return ResponseEntity.noContent().build();
    }
}