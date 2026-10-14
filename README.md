# HỆ THỐNG QUẢN LÝ VÀ ĐIỀU PHỐI CỨU HỘ, CỨU TRỢ THIÊN TAI (Disaster Rescue)

Phân hệ: **Xác thực và Quản lý người dùng (NCL-01)**  
Nhánh: `feature/login-backend`

---

## 1. Cấu trúc dự án
Dự án được phân chia theo cấu trúc rõ ràng, mô-đun hóa, tách biệt hoàn toàn giữa Backend và Frontend:

```text
d:\Đồ án tốt nghiệp 2027\
│
├── API.md                              # Tài liệu đặc tả API chuẩn
├── README.md                           # Hướng dẫn chạy và thông tin cấu hình
├── .gitignore                          # Cấu hình bỏ qua git (target, node_modules, logs,...)
│
├── backend/                            # Mã nguồn API Spring Boot 3
│   ├── pom.xml                         # Cấu hình Maven & dependencies
│   ├── src/main/java/com/disasterrescue/api/
│   │   ├── config/                     # Cấu hình Security, CORS, OpenAPI
│   │   ├── controller/                 # REST Controllers (Auth, User, Role)
│   │   ├── dto/                        # Data Transfer Objects (Request / Response)
│   │   ├── exception/                  # Mã lỗi và Global Exception Handler
│   │   ├── model/                      # Entities JPA (User, Role, RefreshToken, PasswordResetToken)
│   │   ├── repository/                 # Spring Data JPA Repositories
│   │   ├── security/                   # JWT Provider, CustomUserDetailsService, HashService
│   │   └── service/                    # Business Logic, Scheduler, Bootstrap
│   ├── src/main/resources/
│   │   ├── application.yml             # Cấu hình gốc
│   │   ├── application-dev.yml         # Môi trường dev (H2 MySQL mode, dev seeds)
│   │   ├── application-prod.yml        # Môi trường production (MySQL thật, bảo mật)
│   │   └── db/migration/               # Flyway migrations (V1, V2) & dev-migration (V3)
│   └── src/test/java/com/disasterrescue/api/
│       ├── AuthControllerTest.java     # Kiểm thử tích hợp luồng Auth (13 tests)
│       └── UserControllerTest.java     # Kiểm thử tích hợp luồng Admin Users (6 tests)
│
├── frontend/                           # Giao diện người dùng (tách riêng biệt HTML, CSS, JS, Assets)
│   ├── pages/                          # Các trang HTML riêng biệt (login, forgot-password,...)
│   ├── css/                            # Toàn bộ CSS (tách rời, module hóa)
│   ├── js/                             # Toàn bộ mã nguồn JavaScript (API, Auth, UI logic)
│   └── assets/                         # Ảnh, logo, biểu tượng SVG cứu hộ
│
└── postman/                            # Bộ API Collection & Workspace của Postman
```

---

## 2. Công nghệ Backend
- **Framework:** Spring Boot 3.3.4, Java 17 / 21
- **Bảo mật:** Spring Security 6 (Stateless, BCrypt, JWT, CORS)
- **Token JWT:** JJWT 0.12.5 (Access Token sống 15 phút, Refresh Token ngẫu nhiên 256-bit lưu băm SHA-256)
- **Cơ sở dữ liệu:**
  - Dev: H2 Database chế độ mô phỏng MySQL (`MODE=MySQL`)
  - Prod: MySQL 8.x
- **Quản lý lược đồ DB:** Flyway Migration (không dùng `ddl-auto`)
- **Tài liệu API:** SpringDoc OpenAPI 2.5.0 (Swagger UI)
- **Giám sát:** Spring Boot Actuator (`/actuator/health`)

---

## 3. Danh sách tài khoản thử nghiệm (Môi trường Dev)
Tất cả tài khoản mẫu dev có mật khẩu mặc định: `Demo@123`

| Username | Email | Họ và tên | Vai trò (Role) | Trạng thái | Trang đích (redirectPath) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `admin` | `admin@disasterrescue.vn` | Quản Trị Viên | `ADMIN` | `ACTIVE` | `/admin` |
| `dieuphoi` | `dieuphoi@disasterrescue.vn` | Nguyễn Văn Điều Phối | `OPERATOR` | `ACTIVE` | `/dieu-phoi` |
| `cuuho` | `cuuho@disasterrescue.vn` | Trần Văn Cứu Hộ | `RESCUER` | `ACTIVE` | `/nhiem-vu` |
| `nguoidan` | `nguoidan@disasterrescue.vn` | Lê Thị Người Dân | `CITIZEN` | `ACTIVE` | `/gui-yeu-cau` |
| `khoa` | `khoa@disasterrescue.vn` | Phạm Văn Khóa | `CITIZEN` | `LOCKED` | - (Bị từ chối đăng nhập) |

---

## 4. Các biến môi trường hỗ trợ cấu hình

| Biến môi trường | Mặc định (Dev) | Ý nghĩa |
| :--- | :--- | :--- |
| `JWT_SECRET` | Khóa bí mật mặc định | Secret key ký JWT (HMAC-SHA512) |
| `DB_URL` | `jdbc:h2:mem:disaster_rescue;MODE=MySQL...` | Đường dẫn kết nối cơ sở dữ liệu |
| `DB_USERNAME` | `sa` (hoặc `root`) | Tài khoản kết nối DB |
| `DB_PASSWORD` | *(trống)* | Mật khẩu DB |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://127.0.0.1:5500...` | Danh sách tên miền được phép truy cập |
| `FRONTEND_RESET_URL` | `http://127.0.0.1:5500/frontend/pages/reset-password.html` | Đường dẫn trang đặt lại mật khẩu |
| `BOOTSTRAP_ADMIN_PASSWORD` | *(trống)* | Mật khẩu khởi tạo admin đầu tiên khi chạy Production |

---

## 5. Hướng dẫn chạy Backend

### Chạy ứng dụng (Khuyên dùng trên Windows):
```powershell
cd backend
java -jar target\disaster-rescue-backend-1.0.0-SNAPSHOT.jar
```
*Hoặc nhấp đúp file script tiện ích: `backend\run.bat`*

*(Lưu ý: Do thư mục gốc chứa ký tự tiếng Việt có dấu `Đồ án tốt nghiệp 2027`, lệnh `mvn spring-boot:run` trên Windows có thể gặp lỗi mã hóa đường dẫn tạm `@argfile` của hệ thống; chạy trực tiếp bằng `java -jar` hoặc `run.bat` sẽ khởi động ngay lập tức).*

API sẽ khởi động tại: `http://localhost:8080`  
Swagger UI (Dev): `http://localhost:8080/swagger-ui.html`  
Actuator Health: `http://localhost:8080/actuator/health`

### Chạy toàn bộ kiểm thử (MockMvc Integration Tests):
```bash
cd backend
mvn test
```

### Kết quả kiểm thử thực tế:
```text
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.disasterrescue.api.AuthControllerTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in com.disasterrescue.api.UserControllerTest
[INFO] 
[INFO] Results:
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
*(100% test tích hợp đạt chuẩn, bao gồm kiểm tra timing attack, khóa tài khoản, phân quyền, xoay vòng và thu hồi phiên).*

---

## 6. Hướng dẫn chạy Frontend

Frontend được viết bằng HTML5/CSS3/JavaScript thuần. Bạn có thể chạy bằng các cách sau:

### Cách 1: Sử dụng lệnh `npm run dev` (Khuyên dùng)
```powershell
cd frontend
npm run dev
```
Giao diện sẽ chạy tại: `http://localhost:3000` (tự động chuyển hướng đến trang đăng nhập `pages/login.html`).

### Cách 2: Sử dụng Live Server trong VS Code
- Nhấp chuột phải vào file `frontend/index.html` hoặc `frontend/pages/login.html`
- Chọn **"Open with Live Server"** (truy cập tại `http://127.0.0.1:5500`).