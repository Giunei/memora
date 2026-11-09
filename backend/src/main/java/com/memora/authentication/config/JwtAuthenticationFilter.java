package com.memora.authentication.config;

import com.memora.authentication.service.JwtService;
import com.memora.shared.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates the security context from the {@code Authorization: Bearer} header.
 *
 * <p>An absent, malformed or expired token is not an error here — the request
 * simply continues unauthenticated and the entry point answers 401 if the target
 * endpoint required a principal. That keeps the "try to refresh on 401" client
 * logic uniform across every cause.
 *
 * <p>Consequence worth knowing: the access token is self-contained, so a role
 * change or suspension takes effect only when the token expires (15 minutes by
 * default). Immediate revocation would require a per-request lookup, which is
 * the cost this design deliberately avoids.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null
                && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.verify(header.substring(BEARER_PREFIX.length())).ifPresent(payload -> {
                List<SimpleGrantedAuthority> authorities = payload.roles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.authority()))
                        .toList();
                var authentication = new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(payload.userId(), payload.email()), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
