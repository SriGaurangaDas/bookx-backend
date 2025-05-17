package com.bookx.bookx_backend.config;

import com.bookx.bookx_backend.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!test")
public class SecurityConfig {
    private  final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration authConfig) throws Exception{
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure custom authentication entry point for 401
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage()))
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/**","/actuator/health","/error").permitAll()
                        // Book API security (updated to /api/v1):
                        .requestMatchers(HttpMethod.POST, "/api/v1/books").authenticated()         // Create book
                        .requestMatchers(HttpMethod.PUT, "/api/v1/books/**").authenticated()      // Update book
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/books/**").authenticated()   // Delete book
                        .requestMatchers(HttpMethod.GET, "/api/v1/books", "/api/v1/books/**").permitAll() // Allow public read access to books

                        // BookListing API security (NEW):
                        .requestMatchers(HttpMethod.POST, "/api/v1/listings").authenticated()      // Create book listing
                        .requestMatchers(HttpMethod.PUT, "/api/v1/listings/**").authenticated()   // Update book listing
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/listings/**").authenticated()// Delete book listing
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings", "/api/v1/listings/**").permitAll() // Allow public read access to listings

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
