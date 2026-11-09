package com.memora.authentication.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String[] PUBLIC_GET_PATHS = {
        "/actuator/health", "/actuator/health/**", "/actuator/info",
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    private final SecurityProperties properties;

    public SecurityConfig(SecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * Delegating encoder: hashes are written with bcrypt and stored with an
     * {@code {bcrypt}} prefix, so a future migration to a stronger algorithm can
     * verify old hashes and re-encode on login without a flag day.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint)
            throws Exception {
        http
                // CSRF tokens protect cookie-authenticated requests. Ordinary API
                // calls here authenticate with a bearer header, which a cross-site
                // form cannot set. The one cookie-bearing endpoint,
                // /api/auth/refresh, is protected by SameSite=Lax on the cookie
                // itself — the browser will not attach it to a cross-site POST.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                    CorsConfigurationSource source = corsConfigurationSource();
                    if (source == null) {
                        cors.disable();
                    } else {
                        cors.configurationSource(source);
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS)
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.referrerPolicy(referrer ->
                        referrer.policy(org.springframework.security.web.header.writers
                                .ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));

        return http.build();
    }

    /**
     * CORS is only configured when explicit origins are supplied. In development
     * the Angular dev server proxies {@code /api}, so the browser sees one origin
     * and cross-origin rules never come into play — which is also the production
     * topology behind the reverse proxy.
     *
     * @return {@code null} when no origins are configured, meaning CORS stays off
     */
    private CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = properties.allowedOrigins();
        if (allowedOrigins.isEmpty()) {
            return null;
        }
        log.info("CORS enabled for origins: {}", allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Required for the refresh cookie to travel on a genuinely cross-origin
        // deployment; harmless otherwise.
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
