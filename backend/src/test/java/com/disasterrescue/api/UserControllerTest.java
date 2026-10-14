package com.disasterrescue.api;

import com.disasterrescue.api.dto.request.CreateUserRequest;
import com.disasterrescue.api.dto.request.LoginRequest;
import com.disasterrescue.api.dto.request.RefreshTokenRequest;
import com.disasterrescue.api.dto.request.UpdateUserRequest;
import com.disasterrescue.api.dto.request.UpdateUserStatusRequest;
import com.disasterrescue.api.model.UserStatus;
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

import org.springframework.test.annotation.DirtiesContext;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String getAccessToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password, false);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    @DisplayName("Người dùng không phải ADMIN bị từ chối truy cập 403 ACCESS_DENIED khi gọi API quản trị")
    void testNonAdmin_AccessDenied() throws Exception {
        String citizenToken = getAccessToken("nguoidan", "Demo@123");

        // Gọi /api/users
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        // Gọi /api/roles
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("ADMIN truy cập thành công danh sách người dùng và danh sách vai trò")
    void testAdmin_AccessSuccess() throws Exception {
        String adminToken = getAccessToken("admin", "Demo@123");

        // Lấy danh sách roles
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        // Lấy danh sách users có phân trang
        mockMvc.perform(get("/api/users?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("ADMIN tạo người dùng mới thành công và phát hiện trùng lặp username/email")
    void testAdmin_CreateUser_AndConflict() throws Exception {
        String adminToken = getAccessToken("admin", "Demo@123");

        CreateUserRequest req = new CreateUserRequest(
                "cuuho_moi",
                "cuuho_moi@disasterrescue.vn",
                "NewPassword@123",
                "Đội viên Cứu Hộ Mới",
                3L // RESCUER
        );

        // Tạo mới thành công
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("cuuho_moi"))
                .andExpect(jsonPath("$.role").value("RESCUER"));

        // Trùng username -> 409 USERNAME_TAKEN
        CreateUserRequest duplicateUser = new CreateUserRequest(
                "cuuho_moi",
                "another_email@disasterrescue.vn",
                "NewPassword@123",
                "Đội viên Trùng Username",
                3L
        );
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));

        // Trùng email -> 409 EMAIL_TAKEN
        CreateUserRequest duplicateEmail = new CreateUserRequest(
                "cuuho_khac",
                "cuuho_moi@disasterrescue.vn",
                "NewPassword@123",
                "Đội viên Trùng Email",
                3L
        );
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    @DisplayName("ADMIN không được phép tự khóa tài khoản của chính mình")
    void testAdmin_CannotLockSelf() throws Exception {
        String adminToken = getAccessToken("admin", "Demo@123");

        UpdateUserStatusRequest lockReq = new UpdateUserStatusRequest(UserStatus.LOCKED);
        mockMvc.perform(patch("/api/users/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"))
                .andExpect(jsonPath("$.message").value(containsString("không được phép tự khóa")));
    }

    @Test
    @DisplayName("ADMIN không được phép tự thay đổi vai trò của chính mình")
    void testAdmin_CannotChangeOwnRole() throws Exception {
        String adminToken = getAccessToken("admin", "Demo@123");

        UpdateUserRequest updateReq = new UpdateUserRequest("admin@disasterrescue.vn", "Quản Trị Viên Hệ Thống", 4L); // role CITIZEN
        mockMvc.perform(put("/api/users/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"))
                .andExpect(jsonPath("$.message").value(containsString("không được phép tự thay đổi vai trò")));
    }

    @Test
    @DisplayName("Khóa tài khoản lập tức thu hồi mọi phiên đăng nhập và chặn các request tiếp theo")
    void testAdminLockUser_RevokesSessionsImmediately() throws Exception {
        // Đăng nhập tài khoản dieuphoi
        LoginRequest loginReq = new LoginRequest("dieuphoi", "Demo@123", false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String userAccessToken = loginJson.get("accessToken").asText();
        String userRefreshToken = loginJson.get("refreshToken").asText();

        // Kiểm tra request ban đầu hợp lệ
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dieuphoi"));

        // Admin thực hiện khóa tài khoản dieuphoi (ID = 2)
        String adminToken = getAccessToken("admin", "Demo@123");
        UpdateUserStatusRequest lockReq = new UpdateUserStatusRequest(UserStatus.LOCKED);
        mockMvc.perform(patch("/api/users/2/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        // Request tiếp theo bằng access token cũ bị chặn ngay lập tức (do filter kiểm tra DB)
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isUnauthorized());

        // Refresh token của phiên cũng bị hủy
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(userRefreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }
}
