package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        String password,

        @NotBlank(message = "Role name is required")
        String role,

        @NotNull(message = "Company ID is required")
        Long companyId

) {
}