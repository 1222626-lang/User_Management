package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "user_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_permission",
                        columnNames = {"user_id", "permission_id"}
                )
        }
)
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PermissionEffect effect;

    public UserPermission() {
    }

    public UserPermission(
            User user,
            Permission permission,
            PermissionEffect effect) {

        this.user = user;
        this.permission = permission;
        this.effect = effect;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public PermissionEffect getEffect() {
        return effect;
    }

    public void setEffect(PermissionEffect effect) {
        this.effect = effect;
    }
}