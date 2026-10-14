package com.disasterrescue.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AdminResetPasswordRequest {

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
            message = "Mật khẩu phải từ 8 đến 72 ký tự và chứa cả chữ cái lẫn chữ số"
    )
    private String newPassword;

    public AdminResetPasswordRequest() {
    }

    public AdminResetPasswordRequest(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
