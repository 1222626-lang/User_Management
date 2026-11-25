package com.example.demo.controller;

import com.example.demo.dto.BulkPermissionOverridesRequest;
import com.example.demo.dto.PermissionOverrideRequest;
import com.example.demo.dto.PermissionRequest;
import com.example.demo.dto.PermissionResponse;
import com.example.demo.dto.RolePermissionsRequest;
import com.example.demo.dto.RoleRequest;
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
    // CREATE ROLE
    // ==========================================
    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(201)
                .body(authorizationService.createRole(request));
    }

    // ==========================================
    // UPDATE ROLE NAME/DESCRIPTION
    // ==========================================
    @PutMapping("/roles/{roleId}")
    public RoleResponse updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleRequest request) {
        return authorizationService.updateRole(roleId, request);
    }

    // ==========================================
    // DELETE CUSTOM ROLE
    // ==========================================
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        authorizationService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // GET ALL PERMISSIONS
    // ==========================================

    @GetMapping("/permissions")
    public Set<PermissionResponse> getPermissions() {

        return authorizationService.getPermissions();
    }

    // ==========================================
    // CREATE PERMISSION
    // ==========================================
    @PostMapping("/permissions")
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody PermissionRequest request) {
        return ResponseEntity.status(201)
                .body(authorizationService.createPermission(request));
    }

    // ==========================================
    // UPDATE PERMISSION
    // ==========================================
    @PutMapping("/permissions/{permissionId}")
    public PermissionResponse updatePermission(
            @PathVariable Long permissionId,
            @Valid @RequestBody PermissionRequest request) {
        return authorizationService.updatePermission(permissionId, request);
    }

    // DELETE PERMISSION
    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<Void> deletePermission(
            @PathVariable Long permissionId) {
        authorizationService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    // UPDATE ROLE PERMISSIONS

    @PutMapping("/roles/{roleId}/permissions")
    public RoleResponse updateRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody RolePermissionsRequest request) {

        return authorizationService.updateRolePermissions(
                roleId,
                request.permissionNames()
        );
    }

    // GET USER PERMISSIONS

    @GetMapping("/users/{userId}/permissions")
    public UserAuthorizationResponse getUserPermissions(
            @PathVariable Long userId) {

        return authorizationService.getUserAuthorization(
                userId
        );
    }

    // SET ONE USER PERMISSION

    @PutMapping("/users/{userId}/permissions")
    public UserAuthorizationResponse setUserPermission(
            @PathVariable Long userId,
            @Valid @RequestBody PermissionOverrideRequest request) {

        return authorizationService.setUserPermission(
                userId,
                request
        );
    }

    // UPDATE MULTIPLE USER PERMISSIONS

    @PutMapping("/users/{userId}/permissions/bulk")
    public UserAuthorizationResponse replaceUserPermissionOverrides(
            @PathVariable Long userId,
            @Valid @RequestBody BulkPermissionOverridesRequest request) {

        return authorizationService.replaceUserPermissionOverrides(
                userId,
                request.overrides()
        );
    }


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