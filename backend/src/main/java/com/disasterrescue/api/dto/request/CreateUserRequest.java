package com.disasterrescue.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]{3,50}$",
            message = "Tên đăng nhập từ 3 đến 50 ký tự, chỉ gồm chữ cái, số, dấu chấm, gạch dưới, gạch ngang"
    )
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
            message = "Mật khẩu phải từ 8 đến 72 ký tự và chứa cả chữ cái lẫn chữ số"
    )
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên từ 2 đến 100 ký tự")
    private String fullName;

    @NotNull(message = "Vai trò (roleId) không được để trống")
    private Long roleId;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String username, String email, String password, String fullName, Long roleId) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.roleId = roleId;
    }

    public String getUsername() {
        return username != null ? username.trim().toLowerCase() : null;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim().toLowerCase() : null;
    }

    public String getEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName != null ? fullName.trim() : null;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : null;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
