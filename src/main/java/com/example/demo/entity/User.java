package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deleted_by_company", nullable = false)
    private boolean deletedByCompany = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<UserPermission> permissionOverrides = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(
            name = "approval_status",
            nullable = false,
            length = 20
    )
    private AccountStatus status = AccountStatus.APPROVED;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    public User() {
    }

    public User(
            String username,
            String firstName,
            String lastName,
            String email,
            String password,
            Role role,
            Company company) {

        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.company = company;
        this.isDeleted = false;
        this.deletedAt = null;
        this.status = AccountStatus.APPROVED;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedByCompany = false;
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedByCompany = false;
    }

    public void addPermissionOverride(UserPermission override) {
        permissionOverrides.add(override);
        override.setUser(this);
    }

    public void clearPermissionOverrides() {
        permissionOverrides.clear();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeletedByCompany() {
        return deletedByCompany;
    }

    public void setDeletedByCompany(boolean deletedByCompany) {
        this.deletedByCompany = deletedByCompany;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Set<UserPermission> getPermissionOverrides() {
        return permissionOverrides;
    }

    public void setPermissionOverrides(
            Set<UserPermission> permissionOverrides) {
        this.permissionOverrides = permissionOverrides;
    }
}