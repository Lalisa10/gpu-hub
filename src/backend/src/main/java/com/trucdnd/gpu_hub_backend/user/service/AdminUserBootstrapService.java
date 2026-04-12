package com.trucdnd.gpu_hub_backend.user.service;

import com.trucdnd.gpu_hub_backend.common.constants.User.GlobalRole;
import com.trucdnd.gpu_hub_backend.user.entity.User;
import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserBootstrapService implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.email:admin@gmail.com.vn}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:admin}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByGlobalRole(GlobalRole.ADMIN)) {
            return;
        }

        User adminUser = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName("Administrator")
                .globalRole(GlobalRole.ADMIN)
                .isActive(true)
                .build();

        userRepository.save(adminUser);
        log.warn("No ADMIN user found. Bootstrapped default admin user '{}'.", adminUsername);
    }
}
