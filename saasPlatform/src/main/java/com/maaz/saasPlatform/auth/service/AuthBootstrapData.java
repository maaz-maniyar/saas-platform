package com.maaz.saasPlatform.auth.service;

import com.maaz.saasPlatform.auth.model.Role;
import com.maaz.saasPlatform.tenant.context.TenantContext;
import com.maaz.saasPlatform.user.model.User;
import com.maaz.saasPlatform.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthBootstrapData implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AuthBootstrapData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${bootstrap.super-admin.email}") String email,
            @Value("${bootstrap.super-admin.password}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        TenantContext.setTenant("public");

        try {
            if (userRepository.findByEmail(email).isEmpty()) {
                User user = new User();
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(password));
                user.setRole(Role.SUPER_ADMIN);
                userRepository.save(user);
            }
        } finally {
            TenantContext.clear();
        }
    }
}
