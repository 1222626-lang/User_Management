package com.example.demo.dto;

import com.example.demo.entity.AccountStatus;

public record UserResponse(

        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String role,
        Long companyId,
        String companyName,
        AccountStatus approvalStatus

) {
}