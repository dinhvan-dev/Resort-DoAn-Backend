package com.example.resort.configuration;

import com.example.resort.entity.User;
import com.example.resort.enums.Role;
import com.example.resort.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class ApplicationInitConfig {
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin1234}")
    private String adminPassword;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository)
    {
        return args ->
        {
            var existingAdmin = userRepository.findByUsername(adminUsername);
            if (existingAdmin.isEmpty())
            {
                User admin = User.builder()
                                .username(adminUsername)
                                        .password(passwordEncoder.encode(adminPassword))
                                                .role(Role.ADMIN)
                                                        .email("admin@resort.com")
                                                                .fullName("System Admin")
                                                                        .phoneNumber("0000000000")
                                                                                .build();

                userRepository.save(admin);
                log.warn("Admin user '{}' created with default credentials. Please change password immediately!", adminUsername);
            }
            else if (!existingAdmin.get().isActive())
            {
                User admin = existingAdmin.get();
                admin.setActive(true);
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                log.warn("Admin user '{}' was inactive and has been reactivated.", adminUsername);
            }
        };
    }
}
