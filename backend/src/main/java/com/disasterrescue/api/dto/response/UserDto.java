package com.disasterrescue.api.dto.response;

import com.disasterrescue.api.model.User;
import com.disasterrescue.api.model.UserStatus;
import java.time.Instant;

public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String roleName;
    private UserStatus status;
    private Instant createdAt;
    private Instant lastLoginAt;

    public UserDto() {
    }

    public UserDto(Long id, String username, String email, String fullName, String role, String roleName, UserStatus status, Instant createdAt, Instant lastLoginAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.roleName = roleName;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole() != null && user.getRole().getCode() != null ? user.getRole().getCode().name() : null,
                user.getRole() != null ? user.getRole().getName() : null,
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
