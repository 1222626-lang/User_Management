package com.example.demo.service;

import com.example.demo.dto.PermissionOverrideRequest;
import com.example.demo.dto.PermissionRequest;
import com.example.demo.dto.PermissionResponse;
import com.example.demo.dto.RoleRequest;
import com.example.demo.dto.RoleResponse;
import com.example.demo.dto.UserAuthorizationResponse;
import com.example.demo.dto.UserPermissionOverrideResponse;

import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserPermission;

import com.example.demo.exception.ResourceNotFoundException;

import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserPermissionRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorizationService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PermissionService permissionService;

    public AuthorizationService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            UserPermissionRepository userPermissionRepository,
            PermissionService permissionService) {

        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.permissionService = permissionService;
    }

    // ==========================================
    // GET ALL ROLES
    // ==========================================

    @Transactional(readOnly = true)
    public Set<RoleResponse> getRoles() {

        return roleRepository.findAll()
                .stream()
                .map(role -> new RoleResponse(
                        role.getId(),
                        role.getName(),
                        role.getDescription(),
                        role.getPermissions()
                                .stream()
                                .map(Permission::getName)
                                .collect(Collectors.toSet())
                ))
                .collect(Collectors.toSet());
    }

    // CREATE A ROLE
    public RoleResponse createRole(RoleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Role request must not be null");
        }
        String name = normalizeRoleName(request.name());
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }
        Role role = new Role(name, normalizeDescription(request.description()));
        return toRoleResponse(roleRepository.save(role));
    }

    // UPDATE ROLE NAME AND DESCRIPTION
    public RoleResponse updateRole(Long roleId, RoleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Role request must not be null");
        }
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        String name = normalizeRoleName(request.name());
        String currentName = role.getName();
        if (isBuiltInRole(currentName) && !currentName.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("The built-in ADMIN and USER role names cannot be changed");
        }
        roleRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(roleId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Role already exists: " + name);
                });
        role.setName(name);
        role.setDescription(normalizeDescription(request.description()));
        return toRoleResponse(roleRepository.save(role));
    }

    // DELETE A CUSTOM ROLE IF IT IS NOT ASSIGNED
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        if (isBuiltInRole(role.getName())) {
            throw new IllegalArgumentException("The built-in ADMIN and USER roles cannot be deleted");
        }
        if (userRepository.existsByRole_Id(roleId)) {
            throw new IllegalArgumentException("Cannot delete a role assigned to one or more users; reassign those users first");
        }
        roleRepository.delete(role);
    }

    private String normalizeRoleName(String name) {
        if (name == null || !name.trim().matches("[A-Za-z][A-Za-z0-9_]{0,49}")) {
            throw new IllegalArgumentException(
                    "Role name must start with a letter and contain only letters, numbers, or underscores (maximum 50 characters)"
            );
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBuiltInRole(String name) {
        return "ADMIN".equalsIgnoreCase(name) || "USER".equalsIgnoreCase(name);
    }

    // ==========================================
    // GET ALL PERMISSIONS
    // ==========================================

    @Transactional(readOnly = true)
    public Set<PermissionResponse> getPermissions() {

        return permissionRepository.findAll()
                .stream()
                .map(permission -> new PermissionResponse(
                        permission.getId(),
                        permission.getName(),
                        permission.getDescription()
                ))
                .collect(Collectors.toSet());
    }

    // CREATE PERMISSION
    public PermissionResponse createPermission(PermissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Permission request must not be null");
        }
        String name = normalizePermissionName(request.name());
        if (permissionRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Permission already exists: " + name);
        }
        Permission permission = permissionRepository.save(
                new Permission(name, normalizeDescription(request.description()))
        );
        return toPermissionResponse(permission);
    }

    // UPDATE PERMISSION
    public PermissionResponse updatePermission(Long permissionId, PermissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Permission request must not be null");
        }
        Permission permission = getPermission(permissionId);
        String name = normalizePermissionName(request.name());
        permissionRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(permissionId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Permission already exists: " + name);
                });
        permission.setName(name);
        permission.setDescription(normalizeDescription(request.description()));
        return toPermissionResponse(permissionRepository.save(permission));
    }

    // DELETE PERMISSION AND ITS ASSOCIATIONS
    public void deletePermission(Long permissionId) {
        Permission permission = getPermission(permissionId);
        for (Role role : roleRepository.findAll()) {
            role.getPermissions().remove(permission);
        }
        userPermissionRepository.deleteAllByPermissionId(permissionId);
        permissionRepository.delete(permission);
    }

    private Permission getPermission(Long permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));
    }

    private String normalizePermissionName(String name) {
        if (name == null || !name.trim().matches("[A-Za-z][A-Za-z0-9_]{0,99}")) {
            throw new IllegalArgumentException(
                    "Permission name must start with a letter and contain only letters, numbers, or underscores (maximum 100 characters)"
            );
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(), permission.getName(), permission.getDescription()
        );
    }

    // UPDATE ROLE PERMISSIONS

    public RoleResponse updateRolePermissions(
            Long roleId,
            Set<String> permissionNames) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role",
                                roleId
                        )
                );

        Set<Permission> permissions =
                resolvePermissions(permissionNames);

        role.getPermissions().clear();

        role.getPermissions().addAll(permissions);

        Role saved = roleRepository.save(role);

        return toRoleResponse(saved);
    }

    // GET USER AUTHORIZATION


    @Transactional(readOnly = true)
    public UserAuthorizationResponse getUserAuthorization(
            Long userId) {

        User user = getAuthorizationUser(userId);

        return toUserAuthorizationResponse(user);
    }

    // ADD OR UPDATE ONE USER PERMISSION

    public UserAuthorizationResponse setUserPermission(
            Long userId,
            PermissionOverrideRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Permission request must not be null"
            );
        }

        User user = getAuthorizationUser(userId);

        Permission permission =
                resolvePermission(request.permissionName());

        if (request.effect() == null) {
            throw new IllegalArgumentException(
                    "Permission effect must not be null"
            );
        }

        UserPermission override =
                userPermissionRepository
                        .findByUserIdAndPermissionId(
                                userId,
                                permission.getId()
                        )
                        .orElseGet(UserPermission::new);

        override.setUser(user);
        override.setPermission(permission);
        override.setEffect(request.effect());

        userPermissionRepository.save(override);

        return getUserAuthorization(userId);
    }

    // REPLACE USER PERMISSION OVERRIDES


    public UserAuthorizationResponse replaceUserPermissionOverrides(
            Long userId,
            List<PermissionOverrideRequest> requests) {

        User user = getAuthorizationUser(userId);

        if (requests == null) {
            throw new IllegalArgumentException(
                    "Overrides must not be null"
            );
        }

        Set<String> uniqueNames = new HashSet<>();

        Map<Long, UserPermission> existingByPermissionId =
                new HashMap<>();

        for (UserPermission current :
                user.getPermissionOverrides()) {

            existingByPermissionId.put(
                    current.getPermission().getId(),
                    current
            );
        }

        Set<Long> requestedPermissionIds = new HashSet<>();

        for (PermissionOverrideRequest request : requests) {

            if (request == null
                    || request.permissionName() == null
                    || request.permissionName().isBlank()
                    || request.effect() == null) {

                throw new IllegalArgumentException(
                        "Each override must contain a permissionName and effect"
                );
            }

            String normalized =
                    request.permissionName()
                            .trim()
                            .toUpperCase(Locale.ROOT);

            if (!uniqueNames.add(normalized)) {

                throw new IllegalArgumentException(
                        "Duplicate permission: " + normalized
                );
            }

            Permission permission =
                    resolvePermission(normalized);

            requestedPermissionIds.add(permission.getId());

            UserPermission current =
                    existingByPermissionId.get(
                            permission.getId()
                    );

            if (current != null) {

                current.setEffect(request.effect());

            } else {

                user.addPermissionOverride(
                        new UserPermission(
                                user,
                                permission,
                                request.effect()
                        )
                );
            }
        }

        // Remove overrides not included in the new request.

        user.getPermissionOverrides().removeIf(
                override ->
                        !requestedPermissionIds.contains(
                                override.getPermission().getId()
                        )
        );

        userRepository.save(user);

        return toUserAuthorizationResponse(user);
    }

    // REMOVE USER PERMISSION OVERRIDE


    public void removeUserPermissionOverride(
            Long userId,
            Long permissionId) {

        getAuthorizationUser(userId);

        UserPermission override =
                userPermissionRepository
                        .findByUserIdAndPermissionId(
                                userId,
                                permissionId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User permission override",
                                        permissionId
                                )
                        );

        userPermissionRepository.delete(override);
    }

    // ==========================================
    // FIND USER
    // ==========================================

    private User getAuthorizationUser(Long userId) {

        return userRepository
                .findAuthorizationUserByIdAndIsDeletedFalse(
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                userId
                        )
                );
    }

    // FIND PERMISSION

    private Permission resolvePermission(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException(
                    "Permission name must not be blank"
            );
        }

        String normalized = name.trim();

        return permissionRepository
                .findByNameIgnoreCase(normalized)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Permission not found: " + normalized
                        )
                );
    }


    // RESOLVE MULTIPLE PERMISSIONS

    private Set<Permission> resolvePermissions(
            Set<String> names) {

        if (names == null) {

            throw new IllegalArgumentException(
                    "Permission names must not be null"
            );
        }

        return names.stream()
                .map(this::resolvePermission)
                .collect(Collectors.toSet());
    }

    // CONVERT ROLE TO RESPONSE


    private RoleResponse toRoleResponse(Role role) {

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions()
                        .stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet())
        );
    }

    // CONVERT USER TO AUTHORIZATION RESPONSE

    private UserAuthorizationResponse toUserAuthorizationResponse(
            User user) {

        Set<String> rolePermissions =
                user.getRole() == null
                        ? Set.of()
                        : user.getRole()
                        .getPermissions()
                        .stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet());

        Set<UserPermissionOverrideResponse> overrides =
                user.getPermissionOverrides()
                        .stream()
                        .map(override ->
                                new UserPermissionOverrideResponse(
                                        override.getPermission().getId(),
                                        override.getPermission().getName(),
                                        override.getEffect()
                                )
                        )
                        .collect(Collectors.toSet());

        Set<String> effectivePermissions =
                permissionService.getEffectivePermissions(
                        user.getUsername()
                );

        return new UserAuthorizationResponse(
                user.getId(),
                user.getUsername(),
                user.getRole() == null
                        ? null
                        : user.getRole().getName(),
                rolePermissions,
                overrides,
                effectivePermissions
        );
    }
}