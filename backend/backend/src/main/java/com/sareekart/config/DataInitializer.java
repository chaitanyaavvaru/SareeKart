package com.sareekart.config;

import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.RoleName;
import com.sareekart.repository.RoleRepository;
import com.sareekart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent seed for the two well-known demo accounts surfaced on the login
 * page ("Playtester Quick Login"):
 *
 *   admin@sareekart.com    / admin123     -> ROLE_ADMIN
 *   customer@sareekart.com / customer123  -> ROLE_CUSTOMER
 *
 * Hashes are produced at runtime by the application's own PasswordEncoder, so
 * they always match regardless of environment. Users are only created when
 * missing; existing credentials are never overwritten.
 *
 * PRODUCTION: disable via --app.seed.enabled=false once real accounts exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.seed.enabled:true}")
    private boolean seedingEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedingEnabled) {
            log.info("User seeding disabled (app.seed.enabled=false)");
            return;
        }
        ensureUser("admin@sareekart.com", "admin123", "Admin", "User", RoleName.ROLE_ADMIN);
        ensureUser("customer@sareekart.com", "customer123", "Test", "Customer", RoleName.ROLE_CUSTOMER);
    }

    private void ensureUser(String email, String rawPassword, String firstName, String lastName, RoleName roleName) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("Missing role " + roleName + " — check Flyway V3"));

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone("+91-0000000000");
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRoles(java.util.Set.of(role));
        userRepository.save(user);

        log.info("Seeded {} account: {}", roleName, email);
    }
}