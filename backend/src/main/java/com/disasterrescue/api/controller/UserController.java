package com.disasterrescue.api.controller;

import com.disasterrescue.api.dto.request.AdminResetPasswordRequest;
import com.disasterrescue.api.dto.request.CreateUserRequest;
import com.disasterrescue.api.dto.request.UpdateUserRequest;
import com.disasterrescue.api.dto.request.UpdateUserStatusRequest;
import com.disasterrescue.api.dto.response.MessageResponse;
import com.disasterrescue.api.dto.response.PageResponse;
import com.disasterrescue.api.dto.response.UserDto;
import com.disasterrescue.api.model.RoleCode;
import com.disasterrescue.api.model.UserStatus;
import com.disasterrescue.api.security.CustomUserDetails;
import com.disasterrescue.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Người dùng (Users)", description = "Quản trị người dùng trong hệ thống (Chỉ dành cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm và phân trang người dùng", description = "Lọc người dùng theo từ khóa q, vai trò, trạng thái. Size tối đa 100.")
    public ResponseEntity<PageResponse<UserDto>> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) RoleCode role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<UserDto> response = userService.getUsers(q, role, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết người dùng", description = "Lấy thông tin chi tiết của người dùng theo ID")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @Operation(summary = "Tạo người dùng mới", description = "ADMIN cấp tài khoản người dùng mới (không hỗ trợ tự đăng ký)")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin người dùng", description = "Cập nhật email, fullName, vai trò. ADMIN không thể tự đổi vai trò của mình.")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserDto updated = userService.updateUser(id, request, userDetails.getId());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Thay đổi trạng thái tài khoản", description = "Khóa hoặc kích hoạt tài khoản. ADMIN không thể tự khóa chính mình. Khóa sẽ hủy mọi phiên đăng nhập.")
    public ResponseEntity<UserDto> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserDto updated = userService.updateUserStatus(id, request, userDetails.getId());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "ADMIN cấp lại mật khẩu", description = "ADMIN đặt lại mật khẩu mới cho tài khoản và hủy toàn bộ phiên đang hoạt động của tài khoản đó.")
    public ResponseEntity<MessageResponse> resetUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        MessageResponse response = userService.resetUserPassword(id, request);
        return ResponseEntity.ok(response);
    }
}
