package com.example.demo.repository;

import com.example.demo.entity.AccountStatus;
import com.example.demo.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    Page<User> findAllByIsDeletedTrue(Pageable pageable);

    Optional<User> findByIdAndIsDeletedFalse(Long id);

    Optional<User> findByIdAndIsDeletedTrue(Long id);

    Optional<User> findByUsernameIgnoreCaseAndIsDeletedFalse(
            String username
    );

    @EntityGraph(attributePaths = {"company", "role"})
    Optional<User> findWithCompanyByUsernameIgnoreCaseAndIsDeletedFalse(
            String username
    );

    Optional<User> findByUsernameIgnoreCaseAndIsDeletedFalseAndStatus(
            String username,
            AccountStatus status
    );

    Optional<User> findByEmailIgnoreCaseAndIsDeletedFalse(
            String email
    );

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );

    boolean existsByUsernameIgnoreCaseAndIdNot(
            String username,
            Long id
    );

    // Pending registrations.

    Page<User> findAllByStatusAndIsDeletedFalse(
            AccountStatus status,
            Pageable pageable
    );

    // Soft delete users by company.

    @Modifying(clearAutomatically = true)
    @Query("""
        update User u
        set u.isDeleted = true,
            u.deletedAt = CURRENT_TIMESTAMP,
            u.deletedByCompany = true
        where u.company.id = :companyId
          and u.isDeleted = false
    """)
    int softDeleteAllByCompanyId(
            @Param("companyId") Long companyId
    );

    // Restore users by company.

    @Modifying(clearAutomatically = true)
    @Query("""
        update User u
        set u.isDeleted = false,
            u.deletedAt = null,
            u.deletedByCompany = false
        where u.company.id = :companyId
          and u.isDeleted = true
          and u.deletedByCompany = true
    """)
    int restoreAllByCompanyId(
            @Param("companyId") Long companyId
    );

    // ==========================================
    // LOAD USER WITH AUTHORIZATION DATA
    // ==========================================

    @EntityGraph(attributePaths = {
            "company",
            "role",
            "role.permissions",
            "permissionOverrides",
            "permissionOverrides.permission"
    })
    Optional<User>
    findSecurityUserByUsernameIgnoreCaseAndIsDeletedFalse(
            String username
    );

    // ==========================================
    // LOAD USER BY ID WITH AUTHORIZATION DATA
    // ==========================================

    @EntityGraph(attributePaths = {
            "company",
            "role",
            "role.permissions",
            "permissionOverrides",
            "permissionOverrides.permission"
    })
    Optional<User> findAuthorizationUserByIdAndIsDeletedFalse(
            Long id
    );
}
