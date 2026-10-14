package com.disasterrescue.api.service;

import com.disasterrescue.api.dto.request.ForgotPasswordRequest;
import com.disasterrescue.api.dto.request.LoginRequest;
import com.disasterrescue.api.dto.request.LogoutRequest;
import com.disasterrescue.api.dto.request.RefreshTokenRequest;
import com.disasterrescue.api.dto.request.ResetPasswordRequest;
import com.disasterrescue.api.dto.response.LoginResponse;
import com.disasterrescue.api.dto.response.MessageResponse;
import com.disasterrescue.api.dto.response.UserDto;
import com.disasterrescue.api.exception.AppException;
import com.disasterrescue.api.exception.ErrorCode;
import com.disasterrescue.api.model.PasswordResetToken;
import com.disasterrescue.api.model.RefreshToken;
import com.disasterrescue.api.model.User;
import com.disasterrescue.api.model.UserStatus;
import com.disasterrescue.api.repository.PasswordResetTokenRepository;
import com.disasterrescue.api.repository.RefreshTokenRepository;
import com.disasterrescue.api.repository.UserRepository;
import com.disasterrescue.api.security.JwtTokenProvider;
import com.disasterrescue.api.security.SecurityRedirectService;
import com.disasterrescue.api.security.TokenHashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Dummy BCrypt hash to prevent timing attacks when username does not exist
    private static final String DUMMY_HASH = "$2a$10$Gs52vTYAcDMDWfGqjOAvSeIumDPF1J2ImoyFMO3icibf8kFI9P/I6";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenHashService tokenHashService;
    private final SecurityRedirectService securityRedirectService;
    private final MailService mailService;

    @Value("${app.jwt.refresh-token-expiration-seconds:86400}")
    private long refreshTokenExpirationSeconds;

    @Value("${app.jwt.refresh-token-remember-me-seconds:2592000}")
    private long refreshTokenRememberMeSeconds;

    @Value("${app.jwt.reset-password-expiration-seconds:1800}")
    private long resetPasswordExpirationSeconds;

    @Value("${app.frontend-reset-url:http://127.0.0.1:5500/frontend/pages/reset-password.html}")
    private String frontendResetUrl;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TokenHashService tokenHashService,
                       SecurityRedirectService securityRedirectService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenHashService = tokenHashService;
        this.securityRedirectService = securityRedirectService;
        this.mailService = mailService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String login = request.getUsernameOrEmail() != null ? request.getUsernameOrEmail().trim().toLowerCase() : "";
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(login);

        // BR-LOGIN-05: Prevent timing attacks by executing matches even if user is not found
        if (userOpt.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH);
            throw new AppException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS,
                    "Tên đăng nhập/email hoặc mật khẩu không chính xác");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS,
                    "Tên đăng nhập/email hoặc mật khẩu không chính xác");
        }

        // BR-LOGIN-04: Only check user status AFTER password check succeeds
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_LOCKED,
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên.");
        }
        if (user.getStatus() == UserStatus.PENDING) {
            throw new AppException(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_PENDING,
                    "Tài khoản đang chờ kích hoạt. Vui lòng liên hệ Quản trị viên.");
        }

        // Update last login timestamp
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate Access Token (JWT)
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        long accessExpiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

        // Generate Refresh Token (256-bit random)
        long refreshExpirySeconds = request.getRememberMe() ? refreshTokenRememberMeSeconds : refreshTokenExpirationSeconds;
        String rawRefreshToken = tokenHashService.generateSecureRandomToken();
        String tokenHash = tokenHashService.hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                Instant.now().plusSeconds(refreshExpirySeconds)
        );
        refreshTokenRepository.save(refreshToken);

        // BR-LOGIN-06: redirectPath based on role
        String redirectPath = securityRedirectService.getRedirectPathForRole(
                user.getRole() != null ? user.getRole().getCode() : null
        );

        UserDto userDto = UserDto.fromEntity(user);
        return new LoginResponse(accessToken, accessExpiresIn, rawRefreshToken, refreshExpirySeconds, userDto, redirectPath);
    }

    @Transactional(noRollbackFor = AppException.class)
    public LoginResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        String hash = tokenHashService.hashToken(rawToken);

        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(hash);
        if (tokenOpt.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REFRESH_TOKEN,
                    "Refresh token không hợp lệ hoặc không tồn tại");
        }

        RefreshToken token = tokenOpt.get();
        User user = token.getUser();

        // Token reuse detection: if revoked token is used, breach detected! Revoke all user sessions.
        if (token.getRevokedAt() != null) {
            log.warn("Refresh token reuse detected for user ID {}. Revoking all active sessions!", user.getId());
            refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
            throw new AppException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REFRESH_TOKEN,
                    "Phát hiện refresh token đã từng bị thu hồi hoặc sử dụng lại. Toàn bộ phiên đăng nhập đã bị hủy vì lý do an toàn.");
        }

        if (token.isExpired()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ErrorCode.REFRESH_TOKEN_EXPIRED,
                    "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_LOCKED,
                    "Tài khoản không ở trạng thái hoạt động");
        }

        // Single-use: revoke current token
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        // Issue new token pair (rotation)
        String newRawRefreshToken = tokenHashService.generateSecureRandomToken();
        String newTokenHash = tokenHashService.hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = new RefreshToken(
                user,
                newTokenHash,
                Instant.now().plusSeconds(refreshTokenExpirationSeconds)
        );
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        long accessExpiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

        String redirectPath = securityRedirectService.getRedirectPathForRole(
                user.getRole() != null ? user.getRole().getCode() : null
        );

        UserDto userDto = UserDto.fromEntity(user);
        return new LoginResponse(newAccessToken, accessExpiresIn, newRawRefreshToken, refreshTokenExpirationSeconds, userDto, redirectPath);
    }

    @Transactional
    public MessageResponse logout(LogoutRequest request, User currentUser) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            String hash = tokenHashService.hashToken(request.getRefreshToken().trim());
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                if (token.getRevokedAt() == null) {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                }
            });
        } else if (currentUser != null) {
            refreshTokenRepository.revokeAllByUserId(currentUser.getId(), Instant.now());
        }
        return new MessageResponse("Đăng xuất thành công");
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "Không tìm thấy thông tin người dùng"));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Always return the exact same generic message whether the email exists or not
        String genericMessage = "Nếu email của bạn tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn.";

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getStatus() == UserStatus.ACTIVE) {
                // Invalidate all previous unused reset tokens for this user
                passwordResetTokenRepository.invalidateAllByUserId(user.getId(), Instant.now());

                String rawToken = tokenHashService.generateSecureRandomToken();
                String tokenHash = tokenHashService.hashToken(rawToken);

                PasswordResetToken resetToken = new PasswordResetToken(
                        user,
                        tokenHash,
                        Instant.now().plusSeconds(resetPasswordExpirationSeconds)
                );
                passwordResetTokenRepository.save(resetToken);

                String resetLink = frontendResetUrl + "?token=" + rawToken;
                mailService.sendPasswordResetEmail(user.getEmail(), resetLink, rawToken);
            }
        }

        return new MessageResponse(genericMessage);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String hash = tokenHashService.hashToken(request.getToken().trim());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_RESET_TOKEN,
                        "Token đặt lại mật khẩu không hợp lệ hoặc không tồn tại"));

        if (!token.isValid()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_RESET_TOKEN,
                    "Token đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        // Invalidate all active sessions across devices
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());

        return new MessageResponse("Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.");
    }
}
