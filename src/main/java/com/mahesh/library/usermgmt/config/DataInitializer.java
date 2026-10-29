package com.mahesh.library.usermgmt.config;

import com.mahesh.library.usermgmt.dto.ValidationRules;
import com.mahesh.library.usermgmt.entity.Role;
import com.mahesh.library.usermgmt.entity.User;
import com.mahesh.library.usermgmt.repository.RoleRepository;
import com.mahesh.library.usermgmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Seeds the three roles and the default admin (credentials from ADMIN_* environment variables) in one transaction. */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedRolesAndAdmin(RoleRepository roles, UserRepository users, PasswordEncoder encoder,
                                        TransactionTemplate transaction,
                                        @Value("${app.admin.username}") String username,
                                        @Value("${app.admin.email}") String email,
                                        @Value("${app.admin.password}") String password) {
        if (password == null || !password.matches(ValidationRules.PASSWORD)) {
            throw new IllegalStateException("ADMIN_PASSWORD must be set and " + ValidationRules.PASSWORD_MESSAGE);
        }
        // TransactionTemplate rather than @Transactional: a lambda calling a method on this object would bypass the proxy.
        return args -> transaction.executeWithoutResult(status -> {
            for (String name : new String[]{Role.USER, Role.MANAGER, Role.ADMIN}) {
                if (roles.findByName(name).isEmpty()) roles.save(new Role(name));
            }
            String name = username.trim().toLowerCase(Locale.ROOT);
            String mail = email.trim().toLowerCase(Locale.ROOT);
            if (!users.existsByUsername(name) && !users.existsByEmail(mail)) {
                User admin = new User();
                admin.setUsername(name);
                admin.setEmail(mail);
                admin.setFullName("Default Administrator");
                admin.setPassword(encoder.encode(password));
                admin.setRoles(new HashSet<>(Set.of(roles.findByName(Role.ADMIN).orElseThrow())));
                users.save(admin);
            }
        });
    }
}
