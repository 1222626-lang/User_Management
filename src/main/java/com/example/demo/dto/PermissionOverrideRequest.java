package com.example.demo.dto;

import com.example.demo.entity.PermissionEffect;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PermissionOverrideRequest(

        @NotBlank
        String permissionName,

        @NotNull
        PermissionEffect effect

) {
}