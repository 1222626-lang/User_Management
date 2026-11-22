package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkPermissionOverridesRequest(

        @NotNull
        List<@Valid PermissionOverrideRequest> overrides

) {
}