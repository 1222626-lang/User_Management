package com.example.demo.dto;

import com.example.demo.entity.PermissionEffect;

public record UserPermissionOverrideResponse(

        Long permissionId,

        String permissionName,

        PermissionEffect effect

) {
}