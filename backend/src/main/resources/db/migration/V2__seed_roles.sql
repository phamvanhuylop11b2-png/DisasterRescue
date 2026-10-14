-- V2__seed_roles.sql
-- Seed standard roles for Disaster Rescue system

INSERT INTO roles (id, code, name, description) VALUES
(1, 'ADMIN', 'Quản trị viên', 'Quản trị toàn bộ hệ thống'),
(2, 'OPERATOR', 'Điều phối viên', 'Điều phối hoạt động cứu hộ, cứu trợ'),
(3, 'RESCUER', 'Nhân viên cứu hộ', 'Thực hiện nhiệm vụ cứu hộ tại hiện trường'),
(4, 'CITIZEN', 'Người dân', 'Gửi yêu cầu cứu trợ, theo dõi thông tin');
