package com.mahesh.library.usermgmt.config;

import com.mahesh.library.usermgmt.security.CustomAccessDeniedHandler;
import com.mahesh.library.usermgmt.security.CustomAuthEntryPoint;
import com.mahesh.library.usermgmt.security.CustomUserDetailsService;
import com.mahesh.library.usermgmt.security.JwtAuthenticationFilter;
import com.mahesh.library.usermgmt.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC = {
            "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error"};
    private static final String[] CATALOGUE = {
            "/api/v1/books", "/api/v1/books/**", "/api/v1/authors", "/api/v1/authors/**",
            "/api/v1/categories", "/api/v1/categories/**"};

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, CustomUserDetailsService userDetailsService,
                                    CustomAuthEntryPoint entryPoint, CustomAccessDeniedHandler deniedHandler) throws Exception {
        // CSRF attacks ride on credentials the browser attaches automatically (cookies). This API is stateless and
        // reads the JWT only from the Authorization header, which a cross-site page cannot make the browser send,
        // so CSRF protection adds nothing here and is disabled.
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                // URL rules are the coarse first line; @PreAuthorize on controller methods is the second.
                // The first matching rule wins, so specific rules come before broad ones.
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        // user and role administration
                        .requestMatchers("/api/v1/users/me", "/api/v1/users/me/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*").authenticated() // own record or staff: @PreAuthorize
                        .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
                        // catalogue: anyone signed in may read, only staff may change
                        .requestMatchers(HttpMethod.GET, CATALOGUE).authenticated()
                        .requestMatchers(CATALOGUE).hasAnyRole("MANAGER", "ADMIN")
                        // members: staff only, except a user's own linked member record
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/me").authenticated()
                        .requestMatchers("/api/v1/members", "/api/v1/members/**").hasAnyRole("MANAGER", "ADMIN")
                        // borrow/return: USER for their own member record (checked by @memberAccess), or staff
                        .requestMatchers(HttpMethod.POST, "/api/v1/borrow").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/return/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/borrow/member/*/active").authenticated()
                        .requestMatchers("/api/v1/borrow", "/api/v1/borrow/**").hasAnyRole("MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false); // tokens travel in a header, not cookies
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
