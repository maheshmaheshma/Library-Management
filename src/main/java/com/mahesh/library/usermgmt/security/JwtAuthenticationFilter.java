package com.mahesh.library.usermgmt.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads "Authorization: Bearer &lt;token&gt;" and, when the token is valid, puts the user in the SecurityContext.
 * An invalid token leaves the request anonymous, so protected endpoints answer 401 via CustomAuthEntryPoint.
 * Created in SecurityConfig rather than as a @Component, so Spring Boot does not also register it as a servlet filter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Read by CustomAuthEntryPoint so a 401 says why the token was rejected (it never echoes the token). */
    public static final String ERROR_ATTRIBUTE = "jwt.error";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String username = jwtService.parse(header.substring(7).trim()).getSubject();
                // Reload from the DB so disabled/locked accounts and role changes apply immediately.
                UserDetails user = userDetailsService.loadUserByUsername(username);
                if (user.isEnabled() && user.isAccountNonLocked()) {
                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    request.setAttribute(ERROR_ATTRIBUTE, "Account is disabled or locked");
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(ERROR_ATTRIBUTE, "Access token has expired");
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
                request.setAttribute(ERROR_ATTRIBUTE, "Invalid access token");
            }
        }
        chain.doFilter(request, response);
    }
}
