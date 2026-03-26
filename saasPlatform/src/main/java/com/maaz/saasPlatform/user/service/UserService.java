package com.maaz.saasPlatform.user.service;

import com.maaz.saasPlatform.auth.model.Role;
import com.maaz.saasPlatform.auth.model.UserType;
import com.maaz.saasPlatform.tenant.context.TenantContext;
import com.maaz.saasPlatform.tenant.repository.TenantRepository;
import com.maaz.saasPlatform.user.dto.CreateUserRequest;
import com.maaz.saasPlatform.user.model.User;
import com.maaz.saasPlatform.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getPlatformUsers() {
        return runInTenant("public", userRepository::findAll);
    }

    public User createPlatformUser(CreateUserRequest request) {
        return runInTenant("public", () -> createUser(request, true));
    }

    public List<User> getUsersForTenant(String tenantId) {
        return runInTenant(resolveTenantSchema(tenantId), userRepository::findAll);
    }

    public User createUserForTenant(String tenantId, CreateUserRequest request) {
        return runInTenant(resolveTenantSchema(tenantId), () -> createUser(request, false));
    }

    public List<User> getCurrentTenantUsers() {
        return userRepository.findAll();
    }

    public User createCurrentTenantUser(CreateUserRequest request) {
        return createUser(request, false);
    }

    private User createUser(CreateUserRequest request, boolean publicTenant) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        Role role = parseRole(request.getRole());
        UserType userType = parseUserType(request.getUserType());

        validateRoleAssignment(role, userType, publicTenant);

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setUserType(userType);

        return userRepository.save(user);
    }

    private void validateRoleAssignment(Role role, UserType userType, boolean publicTenant) {
        if (publicTenant && role != Role.SUPER_ADMIN) {
            throw new RuntimeException("Public users can only be SUPER_ADMIN");
        }

        if (!publicTenant && role == Role.SUPER_ADMIN) {
            throw new RuntimeException("SUPER_ADMIN cannot be created in a tenant schema");
        }

        if (role == Role.USER && userType == null) {
            throw new RuntimeException("USER must have userType DEV or TESTER");
        }

        if (role != Role.USER && userType != null) {
            throw new RuntimeException("Only USER can have a userType");
        }
    }

    private Role parseRole(String rawRole) {
        try {
            return Role.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new RuntimeException("Invalid role");
        }
    }

    private UserType parseUserType(String rawUserType) {
        if (rawUserType == null || rawUserType.isBlank()) {
            return null;
        }

        try {
            return UserType.valueOf(rawUserType.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new RuntimeException("Invalid userType");
        }
    }

    private String resolveTenantSchema(String tenantId) {
        tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        return "tenant_" + tenantId;
    }

    private <T> T runInTenant(String tenant, Supplier<T> supplier) {
        String previousTenant = TenantContext.getTenant();
        TenantContext.setTenant(tenant);

        try {
            return supplier.get();
        } finally {
            if (previousTenant == null || previousTenant.isBlank()) {
                TenantContext.clear();
            } else {
                TenantContext.setTenant(previousTenant);
            }
        }
    }
}
