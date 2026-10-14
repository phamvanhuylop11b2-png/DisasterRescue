package com.disasterrescue.api.service;

import com.disasterrescue.api.model.Role;
import com.disasterrescue.api.model.RoleCode;
import com.disasterrescue.api.model.User;
import com.disasterrescue.api.model.UserStatus;
import com.disasterrescue.api.repository.RoleRepository;
import com.disasterrescue.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class BootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin-password:}")
    private String bootstrapAdminPassword;

    public BootstrapService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(bootstrapAdminPassword)) {
            return;
        }

        // Check if admin user already exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(null, RoleCode.ADMIN, "Quản trị viên", "Quản trị toàn bộ hệ thống")));

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@disasterrescue.vn");
            admin.setFullName("Quản Trị Viên Hệ Thống");
            admin.setPasswordHash(passwordEncoder.encode(bootstrapAdminPassword));
            admin.setRole(adminRole);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setCreatedAt(Instant.now());
            admin.setUpdatedAt(Instant.now());

            userRepository.save(admin);
            log.info("Khởi tạo tài khoản quản trị viên (admin) ban đầu thành công từ biến môi trường BOOTSTRAP_ADMIN_PASSWORD.");
        }
    }
}
