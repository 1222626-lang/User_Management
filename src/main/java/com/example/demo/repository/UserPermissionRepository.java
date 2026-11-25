package com.example.demo.repository;

import com.example.demo.entity.UserPermission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPermissionRepository
        extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findAllByUserId(Long userId);

    Optional<UserPermission> findByUserIdAndPermissionId(
            Long userId,
            Long permissionId
    );

    void deleteAllByUserId(Long userId);
    void deleteAllByPermissionId(Long permissionId);
}