package com.disasterrescue.api.controller;

import com.disasterrescue.api.dto.request.ForgotPasswordRequest;
import com.disasterrescue.api.dto.request.LoginRequest;
import com.disasterrescue.api.dto.request.LogoutRequest;
import com.disasterrescue.api.dto.request.RefreshTokenRequest;
import com.disasterrescue.api.dto.request.ResetPasswordRequest;
import com.disasterrescue.api.dto.response.LoginResponse;
import com.disasterrescue.api.dto.response.MessageResponse;
import com.disasterrescue.api.dto.response.UserDto;
import com.disasterrescue.api.security.CustomUserDetails;
import com.disasterrescue.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Xác thực (Authentication)", description = "Các API xác thực, phiên làm việc và mật khẩu")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống", description = "Đăng nhập bằng username hoặc email kèm mật khẩu, trả về cặp Access Token và Refresh Token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới Access Token", description = "Xoay vòng refresh token, thu hồi token cũ và cấp token mới")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản", description = "Thu hồi refresh token của phiên hiện tại, gọi lặp lại vẫn thành công")
    public ResponseEntity<MessageResponse> logout(
            @RequestBody(required = false) LogoutRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MessageResponse response = authService.logout(
                request,
                userDetails != null ? userDetails.getUser() : null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Thông tin tài khoản hiện tại", description = "Lấy thông tin người dùng đang đăng nhập dựa trên JWT token")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserDto userDto = authService.getCurrentUser(userDetails.getId());
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu quên mật khẩu", description = "Gửi email hướng dẫn đặt lại mật khẩu với token hiệu lực 30 phút")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu", description = "Xác nhận token và đặt mật khẩu mới, hủy toàn bộ phiên làm việc cũ")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}
