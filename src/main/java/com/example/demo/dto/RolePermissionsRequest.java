package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RolePermissionsRequest(

        @NotNull
        Set<String> permissionNames

) {
}