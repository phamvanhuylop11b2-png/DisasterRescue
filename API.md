# TÀI LIỆU ĐẶC TẢ API - XÁC THỰC VÀ QUẢN LÝ NGƯỜI DÙNG (NCL-01)
Dự án: **Hệ thống quản lý và điều phối cứu hộ, cứu trợ thiên tai (Disaster Rescue)**

---

## 1. Quy ước chung

### Định dạng dữ liệu
- Giao thức: `HTTP/1.1` hoặc `HTTP/2`, định dạng trao đổi: `application/json; charset=UTF-8`.
- Token xác thực: `Authorization: Bearer <access_token>`.
- Thời gian: Định dạng ISO-8601 UTC (ví dụ: `2026-10-14T08:00:00Z`).

### Định dạng lỗi chuẩn
Mọi phản hồi lỗi (4xx, 5xx) tuân thủ cấu trúc đồng nhất:
```json
{
  "timestamp": "2026-10-14T08:15:00.000Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "fieldErrors": [
    {
      "field": "usernameOrEmail",
      "message": "Tên đăng nhập hoặc email không được để trống"
    }
  ]
}
```
*(Mảng `fieldErrors` chỉ xuất hiện khi `code == "VALIDATION_ERROR"`).*

---

## 2. Bảng mã lỗi hệ thống

| Mã lỗi (code) | HTTP Status | Mô tả ý nghĩa |
| :--- | :---: | :--- |
| `VALIDATION_ERROR` | 400 | Dữ liệu đầu vào không hợp lệ hoặc để trống các trường bắt buộc (BR-LOGIN-01, BR-LOGIN-02) |
| `INVALID_CREDENTIALS` | 401 | Tên đăng nhập/email hoặc mật khẩu không chính xác (BR-LOGIN-05, bảo vệ timing attack) |
| `ACCOUNT_LOCKED` | 403 | Tài khoản đã bị khóa (BR-LOGIN-04, chỉ báo sau khi đã nhập đúng mật khẩu) |
| `ACCOUNT_PENDING` | 403 | Tài khoản chưa kích hoạt / đang chờ duyệt (chỉ báo sau khi đã nhập đúng mật khẩu) |
| `UNAUTHORIZED` | 401 | Thiếu token, token không hợp lệ hoặc đã hết hạn (BR-LOGIN-07) |
| `ACCESS_DENIED` | 403 | Không đủ quyền hạn để truy cập tài nguyên (BR-LOGIN-07) |
| `INVALID_REFRESH_TOKEN` | 400 | Refresh token không hợp lệ hoặc đã bị thu hồi / phát hiện bị dùng lại |
| `REFRESH_TOKEN_EXPIRED` | 400 | Refresh token đã hết hạn |
| `INVALID_RESET_TOKEN` | 400 | Token đặt lại mật khẩu không hợp lệ, đã hết hạn (quá 30 phút) hoặc đã được dùng |
| `USERNAME_TAKEN` | 409 | Tên đăng nhập đã tồn tại trong hệ thống |
| `EMAIL_TAKEN` | 409 | Địa chỉ email đã tồn tại trong hệ thống |
| `USER_NOT_FOUND` | 404 | Người dùng không tồn tại |
| `ROLE_NOT_FOUND` | 400 | Vai trò yêu cầu không tồn tại |
| `CANNOT_MODIFY_SELF` | 400 | Quản trị viên không được phép tự khóa tài khoản hoặc tự đổi vai trò của chính mình |
| `NOT_FOUND` | 404 | Đường dẫn API không tồn tại |
| `INTERNAL_SERVER_ERROR` | 500 | Lỗi nội bộ hệ thống |

---

## 3. Danh sách Endpoint API

### 3.1. Xác thực & Quản lý phiên (Authentication)

#### 1. Đăng nhập hệ thống
* **Endpoint:** `POST /api/auth/login`
* **Quyền hạn:** Công khai
* **Nghiệp vụ:** Nhận username hoặc email kèm mật khẩu. Kiểm tra bảo vệ timing attack. Trả token và `redirectPath` theo vai trò.
* **Request mẫu:**
```json
{
  "usernameOrEmail": "admin",
  "password": "Demo@123",
  "rememberMe": false
}
```
* **Response 200 OK:**
```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 900,
  "refreshToken": "c4f77e43bbbebe0b3258d4d203228266cc88d237e19ef67cfadbdd3e6812ede5",
  "refreshExpiresIn": 86400,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@disasterrescue.vn",
    "fullName": "Quản Trị Viên",
    "role": "ADMIN",
    "roleName": "Quản trị viên",
    "status": "ACTIVE",
    "createdAt": "2026-10-14T08:00:00Z",
    "lastLoginAt": "2026-10-14T08:15:00Z"
  },
  "redirectPath": "/admin"
}
```

> **Bảng điều hướng theo vai trò (redirectPath):**
> - `ADMIN` (Quản trị viên) ➔ `/admin`
> - `OPERATOR` (Điều phối viên) ➔ `/dieu-phoi`
> - `RESCUER` (Nhân viên cứu hộ) ➔ `/nhiem-vu`
> - `CITIZEN` (Người dân) ➔ `/gui-yeu-cau`

---

#### 2. Làm mới Access Token (Token Rotation)
* **Endpoint:** `POST /api/auth/refresh`
* **Quyền hạn:** Công khai
* **Nghiệp vụ:** Thu hồi refresh token cũ và phát hành cặp token mới. Nếu token đã thu hồi bị gửi lại (nghi ngờ token bị đánh cắp), toàn bộ phiên của người dùng sẽ bị hủy ngay lập tức.
* **Request mẫu:**
```json
{
  "refreshToken": "c4f77e43bbbebe0b3258d4d203228266cc88d237e19ef67cfadbdd3e6812ede5"
}
```
* **Response 200 OK:** Trả về cấu trúc tương tự login với cặp accessToken và refreshToken mới.

---

#### 3. Đăng xuất
* **Endpoint:** `POST /api/auth/logout`
* **Quyền hạn:** Công khai (hỗ trợ truyền kèm token xác thực hoặc refreshToken trong body)
* **Nghiệp vụ:** Thu hồi refreshToken. Gọi lặp lại vẫn trả về 200 OK (idempotent).
* **Request mẫu:**
```json
{
  "refreshToken": "c4f77e43bbbebe0b3258d4d203228266cc88d237e19ef67cfadbdd3e6812ede5"
}
```
* **Response 200 OK:**
```json
{
  "message": "Đăng xuất thành công"
}
```

---

#### 4. Lấy thông tin tài khoản hiện tại
* **Endpoint:** `GET /api/auth/me`
* **Quyền hạn:** Yêu cầu xác thực (`Bearer <accessToken>`)
* **Response 200 OK:**
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@disasterrescue.vn",
  "fullName": "Quản Trị Viên",
  "role": "ADMIN",
  "roleName": "Quản trị viên",
  "status": "ACTIVE",
  "createdAt": "2026-10-14T08:00:00Z",
  "lastLoginAt": "2026-10-14T08:15:00Z"
}
```

---

#### 5. Yêu cầu quên mật khẩu
* **Endpoint:** `POST /api/auth/forgot-password`
* **Quyền hạn:** Công khai
* **Nghiệp vụ:** Luôn trả cùng một thông báo dù email có tồn tại hay không. Tạo token reset có hiệu lực 30 phút.
* **Request mẫu:**
```json
{
  "email": "nguoidan@disasterrescue.vn"
}
```
* **Response 200 OK:**
```json
{
  "message": "Nếu email của bạn tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn."
}
```

---

#### 6. Đặt lại mật khẩu
* **Endpoint:** `POST /api/auth/reset-password`
* **Quyền hạn:** Công khai
* **Nghiệp vụ:** Xác nhận token và đổi mật khẩu mới (8-72 ký tự gồm chữ và số). Hủy toàn bộ phiên đăng nhập cũ trên mọi thiết bị.
* **Request mẫu:**
```json
{
  "token": "7a3f81e8b93b5a1c4d2e...",
  "newPassword": "NewPassword@2026"
}
```
* **Response 200 OK:**
```json
{
  "message": "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới."
}
```

---

### 3.2. Quản trị vai trò & Người dùng (Chỉ ADMIN)

#### 7. Lấy danh sách vai trò
* **Endpoint:** `GET /api/roles`
* **Quyền hạn:** `ROLE_ADMIN`
* **Response 200 OK:**
```json
[
  { "id": 1, "code": "ADMIN", "name": "Quản trị viên", "description": "Quản trị toàn bộ hệ thống" },
  { "id": 2, "code": "OPERATOR", "name": "Điều phối viên", "description": "Điều phối hoạt động cứu hộ, cứu trợ" },
  { "id": 3, "code": "RESCUER", "name": "Nhân viên cứu hộ", "description": "Thực hiện nhiệm vụ cứu hộ tại hiện trường" },
  { "id": 4, "code": "CITIZEN", "name": "Người dân", "description": "Gửi yêu cầu cứu trợ, theo dõi thông tin" }
]
```

---

#### 8. Tìm kiếm và phân trang người dùng
* **Endpoint:** `GET /api/users`
* **Quyền hạn:** `ROLE_ADMIN`
* **Query Params:**
  - `q` (tùy chọn): Tìm kiếm gần đúng theo username, email, fullName.
  - `role` (tùy chọn): `ADMIN`, `OPERATOR`, `RESCUER`, `CITIZEN`.
  - `status` (tùy chọn): `ACTIVE`, `LOCKED`, `PENDING`.
  - `page` (mặc định 0): Số trang (0-indexed).
  - `size` (mặc định 20, tối đa 100): Kích thước trang.
* **Response 200 OK:**
```json
{
  "content": [
    {
      "id": 1,
      "username": "admin",
      "email": "admin@disasterrescue.vn",
      "fullName": "Quản Trị Viên",
      "role": "ADMIN",
      "roleName": "Quản trị viên",
      "status": "ACTIVE",
      "createdAt": "2026-10-14T08:00:00Z",
      "lastLoginAt": "2026-10-14T08:15:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1,
  "last": true
}
```

---

#### 9. Lấy chi tiết người dùng
* **Endpoint:** `GET /api/users/{id}`
* **Quyền hạn:** `ROLE_ADMIN`
* **Response 200 OK:** Đối tượng `UserDto`.

---

#### 10. Tạo người dùng mới
* **Endpoint:** `POST /api/users`
* **Quyền hạn:** `ROLE_ADMIN`
* **Request mẫu:**
```json
{
  "username": "cuuho_02",
  "email": "cuuho02@disasterrescue.vn",
  "password": "Password@123",
  "fullName": "Lê Văn Cứu Hộ",
  "roleId": 3
}
```
* **Response 201 Created:** Trả về `UserDto` vừa tạo.

---

#### 11. Cập nhật thông tin người dùng
* **Endpoint:** `PUT /api/users/{id}`
* **Quyền hạn:** `ROLE_ADMIN`
* **Quy tắc:** ADMIN không được tự đổi vai trò của chính mình.
* **Request mẫu:**
```json
{
  "email": "cuuho_moi@disasterrescue.vn",
  "fullName": "Lê Văn Cứu Hộ Đội 1",
  "roleId": 3
}
```
* **Response 200 OK:** Trả về `UserDto` đã cập nhật.

---

#### 12. Thay đổi trạng thái tài khoản (Khóa / Mở khóa)
* **Endpoint:** `PATCH /api/users/{id}/status`
* **Quyền hạn:** `ROLE_ADMIN`
* **Quy tắc:**
  - ADMIN không được tự khóa chính mình.
  - Khi khóa tài khoản (`LOCKED`), toàn bộ phiên làm việc (refresh tokens) lập tức bị thu hồi và các request tiếp theo bị từ chối ngay lập tức.
* **Request mẫu:**
```json
{
  "status": "LOCKED"
}
```
* **Response 200 OK:** Trả về `UserDto` đã cập nhật trạng thái.

---

#### 13. ADMIN cấp lại mật khẩu
* **Endpoint:** `POST /api/users/{id}/reset-password`
* **Quyền hạn:** `ROLE_ADMIN`
* **Nghiệp vụ:** Đặt mật khẩu mới và hủy toàn bộ phiên đang đăng nhập của tài khoản đó.
* **Request mẫu:**
```json
{
  "newPassword": "NewPassword@123"
}
```
* **Response 200 OK:**
```json
{
  "message": "Cấp lại mật khẩu thành công"
}
```

---

### 3.3. Kiểm tra tình trạng dịch vụ (Actuator)

#### 14. Kiểm tra sức khỏe hệ thống
* **Endpoint:** `GET /actuator/health`
* **Quyền hạn:** Công khai
* **Response 200 OK:**
```json
{
  "status": "UP"
}
```
