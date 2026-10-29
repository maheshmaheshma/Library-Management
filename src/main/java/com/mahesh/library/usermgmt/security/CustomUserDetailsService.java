package com.mahesh.library.usermgmt.security;

import com.mahesh.library.usermgmt.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Loads a user by username or email (both stored lower-case) and maps roles to authorities. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public CustomUserPrincipal loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String login = usernameOrEmail == null ? "" : usernameOrEmail.trim().toLowerCase(Locale.ROOT);
        return users.findByUsername(login).or(() -> users.findByEmail(login))
                .map(CustomUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
