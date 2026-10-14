package com.disasterrescue.api.dto.response;

import com.disasterrescue.api.model.Role;

public class RoleDto {
    private Long id;
    private String code;
    private String name;
    private String description;

    public RoleDto() {
    }

    public RoleDto(Long id, String code, String name, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static RoleDto fromEntity(Role role) {
        if (role == null) return null;
        return new RoleDto(
                role.getId(),
                role.getCode() != null ? role.getCode().name() : null,
                role.getName(),
                role.getDescription()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
