package com.disasterrescue.api;

import com.disasterrescue.api.dto.request.ForgotPasswordRequest;
import com.disasterrescue.api.dto.request.LoginRequest;
import com.disasterrescue.api.dto.request.LogoutRequest;
import com.disasterrescue.api.dto.request.RefreshTokenRequest;
import com.disasterrescue.api.dto.request.ResetPasswordRequest;
import com.disasterrescue.api.model.PasswordResetToken;
import com.disasterrescue.api.model.User;
import com.disasterrescue.api.repository.PasswordResetTokenRepository;
import com.disasterrescue.api.repository.UserRepository;
import com.disasterrescue.api.security.TokenHashService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.test.annotation.DirtiesContext;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private TokenHashService tokenHashService;

    @Test
    @DisplayName("Đăng nhập thành công và trả về đầy đủ token, user, redirectPath theo vai trò")
    void testLogin_Success_Admin() throws Exception {
        LoginRequest request = new LoginRequest("admin", "Demo@123", false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshExpiresIn").value(86400))
                .andExpect(jsonPath("$.redirectPath").value("/admin"))
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.roleName").value("Quản trị viên"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Đăng nhập các vai trò khác nhau trả về đúng redirectPath tương ứng")
    void testLogin_RoleRedirectPaths() throws Exception {
        // OPERATOR -> /dieu-phoi
        LoginRequest opReq = new LoginRequest("dieuphoi", "Demo@123", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(opReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectPath").value("/dieu-phoi"));

        // RESCUER -> /nhiem-vu
        LoginRequest rescuerReq = new LoginRequest("cuuho", "Demo@123", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rescuerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectPath").value("/nhiem-vu"));

        // CITIZEN -> /gui-yeu-cau
        LoginRequest citizenReq = new LoginRequest("nguoidan", "Demo@123", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citizenReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectPath").value("/gui-yeu-cau"));
    }

    @Test
    @DisplayName("Ghi nhớ đăng nhập (rememberMe) cấp refreshToken hạn 30 ngày")
    void testLogin_RememberMe() throws Exception {
        LoginRequest req = new LoginRequest("admin", "Demo@123", true);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshExpiresIn").value(2592000));
    }

    @Test
    @DisplayName("Sai mật khẩu và tài khoản không tồn tại đều trả cùng mã lỗi 401 INVALID_CREDENTIALS")
    void testLogin_InvalidCredentials_SameError() throws Exception {
        // Sai mật khẩu
        LoginRequest wrongPass = new LoginRequest("admin", "WrongPass@123", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập/email hoặc mật khẩu không chính xác"));

        // Tài khoản không tồn tại
        LoginRequest notFound = new LoginRequest("user_not_exist", "Demo@123", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notFound)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập/email hoặc mật khẩu không chính xác"));
    }

    @Test
    @DisplayName("Tài khoản bị khóa chỉ báo lỗi sau khi nhập đúng mật khẩu; sai mật khẩu vẫn báo INVALID_CREDENTIALS")
    void testLogin_AccountLocked() throws Exception {
        // Nhập đúng mật khẩu với tài khoản bị khóa -> 403 ACCOUNT_LOCKED
        LoginRequest correctPassLocked = new LoginRequest("khoa", "Demo@123", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctPassLocked)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").value(containsString("đã bị khóa")));

        // Nhập sai mật khẩu với tài khoản bị khóa -> vẫn trả 401 INVALID_CREDENTIALS
        LoginRequest wrongPassLocked = new LoginRequest("khoa", "WrongPass@123", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassLocked)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Để trống tên đăng nhập hoặc mật khẩu trả về 400 VALIDATION_ERROR kèm fieldErrors")
    void testLogin_ValidationErrors_BlankFields() throws Exception {
        LoginRequest blankReq = new LoginRequest("", "", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/auth/me yêu cầu token hợp lệ, thiếu token trả về 401 UNAUTHORIZED")
    void testGetMe_Profile() throws Exception {
        // Không có token
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // Đăng nhập lấy token
        LoginRequest req = new LoginRequest("admin", "Demo@123", false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = jsonNode.get("accessToken").asText();

        // Gọi /me với Bearer token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@disasterrescue.vn"));
    }

    @Test
    @DisplayName("Xoay vòng refresh token thành công, và phát hiện dùng lại token đã thu hồi sẽ hủy toàn bộ phiên")
    void testRefreshToken_Rotation_And_ReuseDetection() throws Exception {
        // 1. Đăng nhập
        LoginRequest loginReq = new LoginRequest("admin", "Demo@123", false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String initialRefreshToken = loginJson.get("refreshToken").asText();

        // 2. Làm mới token thành công (Token Rotation)
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJson.get("refreshToken").asText();
        assertNotEquals(initialRefreshToken, newRefreshToken);

        // 3. Phát hiện token cũ bị dùng lại (Tấn công đánh cắp token)
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value(containsString("thu hồi hoặc sử dụng lại")));

        // 4. Token mới cũng đã bị hủy do phát hiện tái sử dụng token
        RefreshTokenRequest secondRefreshReq = new RefreshTokenRequest(newRefreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRefreshReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("Đăng xuất thu hồi refresh token, gọi lặp lại vẫn thành công (idempotent)")
    void testLogout_Success() throws Exception {
        // Đăng nhập
        LoginRequest loginReq = new LoginRequest("admin", "Demo@123", false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        LogoutRequest logoutReq = new LogoutRequest(refreshToken);

        // Gọi logout lần 1
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

        // Gọi logout lặp lại lần 2 vẫn 200 OK
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk());

        // Refresh bằng token đã đăng xuất sẽ báo lỗi
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("Quên mật khẩu trả về cùng một thông báo dù email có tồn tại hay không")
    void testForgotPassword_SameMessage() throws Exception {
        String expectedMessage = "Nếu email của bạn tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn.";

        // Email có tồn tại
        ForgotPasswordRequest existsReq = new ForgotPasswordRequest("admin@disasterrescue.vn");
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existsReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedMessage));

        // Email không tồn tại
        ForgotPasswordRequest notExistsReq = new ForgotPasswordRequest("nonexistent@disasterrescue.vn");
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notExistsReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    @DisplayName("Quy trình đặt lại mật khẩu thành công và hủy các phiên đăng nhập cũ")
    void testResetPassword_Success() throws Exception {
        User user = userRepository.findByUsername("nguoidan").orElseThrow();

        // Tạo reset token hợp lệ cho người dùng
        String rawToken = tokenHashService.generateSecureRandomToken();
        String tokenHash = tokenHashService.hashToken(rawToken);
        PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, Instant.now().plusSeconds(1800));
        passwordResetTokenRepository.save(resetToken);

        // Gọi đặt lại mật khẩu
        ResetPasswordRequest resetReq = new ResetPasswordRequest(rawToken, "NewSecret@2026");
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Đặt lại mật khẩu thành công")));

        // Thử đăng nhập lại bằng mật khẩu mới
        LoginRequest newLoginReq = new LoginRequest("nguoidan", "NewSecret@2026", false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk());

        // Khôi phục lại mật khẩu cho user nguoidan để các test khác tiếp tục dùng
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Demo@123"));
        userRepository.save(user);

        // Token đặt lại đã dùng không thể sử dụng lại lần 2
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    @DisplayName("Endpoint Actuator /actuator/health công khai")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Đường dẫn không tồn tại trả về 404 NOT_FOUND theo format lỗi chung")
    void testNotFoundRoute() throws Exception {
        LoginRequest req = new LoginRequest("admin", "Demo@123", false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = jsonNode.get("accessToken").asText();

        mockMvc.perform(get("/api/unknown/endpoint/route")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
