package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PermissionRequest(
        @NotBlank(message = "Permission name is required")
        @Size(max = 100, message = "Permission name must not exceed 100 characters")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*", message = "Permission name must start with a letter and contain only letters, numbers, or underscores")
        String name,
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}
