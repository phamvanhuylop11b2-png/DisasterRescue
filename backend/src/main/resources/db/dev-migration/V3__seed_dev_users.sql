-- V3__seed_dev_users.sql
-- 5 sample dev accounts with password 'Demo@123'
-- admin, dieuphoi, cuuho, nguoidan, khoa (locked)

INSERT INTO users (id, username, email, password_hash, full_name, role_id, status, created_at, updated_at) VALUES
(1, 'admin', 'admin@disasterrescue.vn', '$2a$10$Gs52vTYAcDMDWfGqjOAvSeIumDPF1J2ImoyFMO3icibf8kFI9P/I6', 'Quản Trị Viên', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'dieuphoi', 'dieuphoi@disasterrescue.vn', '$2a$10$Gs52vTYAcDMDWfGqjOAvSeIumDPF1J2ImoyFMO3icibf8kFI9P/I6', 'Nguyễn Văn Điều Phối', 2, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'cuuho', 'cuuho@disasterrescue.vn', '$2a$10$Gs52vTYAcDMDWfGqjOAvSeIumDPF1J2ImoyFMO3icibf8kFI9P/I6', 'Trần Văn Cứu Hộ', 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'nguoidan', 'nguoidan@disasterrescue.vn', '$2a$10$Gs52vTYAcDMDWfGqjOAvSeIumDPF1J2ImoyFMO3icibf8kFI9P/I6', 'Lê Thị Người Dân', 4, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'khoa', 'khoa@disasterrescue.vn', '$2a$10$Gs52vTYAcDMDWfGqjOAvSeIumDPF1J2ImoyFMO3icibf8kFI9P/I6', 'Phạm Văn Khóa', 4, 'LOCKED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
