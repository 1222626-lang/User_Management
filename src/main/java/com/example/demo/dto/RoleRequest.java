package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleRequest(
        @NotBlank(message = "Role name is required")
        @Size(max = 50, message = "Role name must not exceed 50 characters")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*", message = "Role name must start with a letter and contain only letters, numbers, or underscores")
        String name,
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}
