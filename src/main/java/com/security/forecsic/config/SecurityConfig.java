package com.security.forecsic.config;

import com.security.forecsic.service.CustomUserDetailsService;
import com.security.forecsic.utilits.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow all CORS preflight OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public authentication endpoints
                        .requestMatchers("/auth/**").permitAll()

                        // Public catalog & certificate verification endpoints
                        .requestMatchers("/api/public/**").permitAll()

                        // Public research paper submission & tracking endpoints
                        .requestMatchers(HttpMethod.POST, "/api/research-papers/submit", "/api/research-papers/submit-with-file").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/research-papers/track/**").permitAll()

                        // Author private submissions endpoint
                        .requestMatchers(HttpMethod.GET, "/api/research-papers/my-submissions").authenticated()

                        // Standalone paper document uploads require authentication
                        .requestMatchers(HttpMethod.POST, "/api/research-papers/upload").authenticated()

                        // Research paper moderation and full repository inspection require privileged roles
                        .requestMatchers(HttpMethod.GET, "/api/research-papers", "/api/research-papers/*").hasAnyRole("ADMIN", "PUBLISHER", "EDITOR")

                        // Public blog reading endpoints
                        .requestMatchers(HttpMethod.GET, "/api/blogs", "/api/blogs/*", "/blogpost", "/blogpost/*").permitAll()

                        // Admin blog moderation endpoints require privileged roles
                        .requestMatchers(HttpMethod.GET, "/api/blogs/admin/**", "/blogpost/admin/**").hasAnyRole("ADMIN", "PUBLISHER", "EDITOR")

                        // Publisher & Admin Portal endpoints
                        .requestMatchers("/api/publisher/**").hasAnyRole("PUBLISHER", "ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "PUBLISHER", "EDITOR")

                        // Any other requests require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Authentication token is missing or invalid. Please log in.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Forbidden: You do not have permission for this action.\"}");
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
