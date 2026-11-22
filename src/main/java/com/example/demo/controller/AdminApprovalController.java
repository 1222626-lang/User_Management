
package com.example.demo.controller;

import com.example.demo.dto.UserResponse;
import com.example.demo.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminApprovalController {

    private final UserService userService;

    public AdminApprovalController(UserService userService) {
        this.userService = userService;
    }

    // GET ALL PENDING USERS
    @GetMapping("/pending")
    public ResponseEntity<Page<UserResponse>> getPendingUsers(
            Pageable pageable) {

        return ResponseEntity.ok(
                userService.getPendingRegistrations(pageable)
        );
    }

    // APPROVE USER
    @PatchMapping("/{id}/approve")
    public ResponseEntity<UserResponse> approveUser(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                userService.approveRegistration(id)
        );
    }

    // REJECT USER
    @PatchMapping("/{id}/reject")
    public ResponseEntity<UserResponse> rejectUser(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                userService.rejectRegistration(id)
        );
    }
}
