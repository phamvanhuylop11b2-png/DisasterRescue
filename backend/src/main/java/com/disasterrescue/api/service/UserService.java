package com.disasterrescue.api.service;

import com.disasterrescue.api.dto.request.AdminResetPasswordRequest;
import com.disasterrescue.api.dto.request.CreateUserRequest;
import com.disasterrescue.api.dto.request.UpdateUserRequest;
import com.disasterrescue.api.dto.request.UpdateUserStatusRequest;
import com.disasterrescue.api.dto.response.MessageResponse;
import com.disasterrescue.api.dto.response.PageResponse;
import com.disasterrescue.api.dto.response.UserDto;
import com.disasterrescue.api.exception.AppException;
import com.disasterrescue.api.exception.ErrorCode;
import com.disasterrescue.api.model.Role;
import com.disasterrescue.api.model.RoleCode;
import com.disasterrescue.api.model.User;
import com.disasterrescue.api.model.UserStatus;
import com.disasterrescue.api.repository.RefreshTokenRepository;
import com.disasterrescue.api.repository.RoleRepository;
import com.disasterrescue.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> getUsers(String q, RoleCode roleCode, UserStatus status, int page, int size) {
        // Enforce maximum page size = 100
        int validSize = Math.max(1, Math.min(size, 100));
        int validPage = Math.max(0, page);

        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> usersPage = userRepository.searchUsers(
                q != null ? q.trim() : null,
                roleCode,
                status,
                pageable
        );

        return PageResponse.of(usersPage.map(UserDto::fromEntity));
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND,
                        "Không tìm thấy người dùng với ID: " + id));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        String username = request.getUsername().toLowerCase();
        String email = request.getEmail().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new AppException(HttpStatus.CONFLICT, ErrorCode.USERNAME_TAKEN,
                    "Tên đăng nhập '" + username + "' đã được sử dụng");
        }

        if (userRepository.existsByEmail(email)) {
            throw new AppException(HttpStatus.CONFLICT, ErrorCode.EMAIL_TAKEN,
                    "Email '" + email + "' đã được sử dụng");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, ErrorCode.ROLE_NOT_FOUND,
                        "Vai trò không tồn tại"));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        return UserDto.fromEntity(savedUser);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request, Long currentAdminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND,
                        "Không tìm thấy người dùng với ID: " + id));

        String newEmail = request.getEmail().toLowerCase();
        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new AppException(HttpStatus.CONFLICT, ErrorCode.EMAIL_TAKEN,
                    "Email '" + newEmail + "' đã được sử dụng bởi tài khoản khác");
        }

        Role newRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, ErrorCode.ROLE_NOT_FOUND,
                        "Vai trò không tồn tại"));

        // Admin cannot change their own role
        if (id.equals(currentAdminId) && !user.getRole().getId().equals(newRole.getId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, ErrorCode.CANNOT_MODIFY_SELF,
                    "Quản trị viên không được phép tự thay đổi vai trò của chính mình");
        }

        user.setEmail(newEmail);
        user.setFullName(request.getFullName());
        user.setRole(newRole);
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        return UserDto.fromEntity(savedUser);
    }

    @Transactional
    public UserDto updateUserStatus(Long id, UpdateUserStatusRequest request, Long currentAdminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND,
                        "Không tìm thấy người dùng với ID: " + id));

        // Admin cannot lock their own account
        if (id.equals(currentAdminId) && request.getStatus() == UserStatus.LOCKED) {
            throw new AppException(HttpStatus.BAD_REQUEST, ErrorCode.CANNOT_MODIFY_SELF,
                    "Quản trị viên không được phép tự khóa tài khoản của chính mình");
        }

        user.setStatus(request.getStatus());
        user.setUpdatedAt(Instant.now());

        // When account is locked, immediately revoke all active sessions across devices
        if (request.getStatus() == UserStatus.LOCKED) {
            refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
        }

        User savedUser = userRepository.save(user);
        return UserDto.fromEntity(savedUser);
    }

    @Transactional
    public MessageResponse resetUserPassword(Long id, AdminResetPasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND,
                        "Không tìm thấy người dùng với ID: " + id));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Revoke all active sessions of this user so they must login with the new password
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());

        return new MessageResponse("Cấp lại mật khẩu thành công");
    }
}
