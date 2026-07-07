package com.example.resort.configuration;

import com.example.resort.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private final String[]  PUBLIC_ENDPOINTS = {
            "/auth/token",
            "/auth/introspect",
            "/auth/logout",
            "/auth/refresh"

    };

    private final CustomJWTDecoder customJWTDecoder;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity)
        throws Exception
    {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .cors(cors ->cors.configurationSource(corsConfigurationSource()))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        //  Public
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()

                        //  USER
                        .requestMatchers(HttpMethod.GET, "/rooms", "/rooms/**").hasAnyRole("USER", "STAFF", "HOUSEKEEPING", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/bookings", "/Booking").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/bookings/my-bookings", "/Booking/my-bookings").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/bookings/status", "/Booking/status").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/bookings/{bookingId}", "/Booking/{bookingId}").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/customers", "/Customer").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/customers/my-profile", "/Customer/my-profile").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/customers/my-profile", "/Customer/my-profile").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/myinfo").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/role").hasAnyRole("STAFF", "ADMIN")

                        //  STAFF
                        .requestMatchers(HttpMethod.GET, "/bookings", "/Booking").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/bookings/*/check-in", "/Booking/*/check-in").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/bookings/*/check-out", "/Booking/*/check-out").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/bookings/**", "/Booking/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/customers", "/Customer").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/customers/**", "/Customer/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/rooms").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/rooms/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/rooms/**").hasAnyRole("STAFF", "ADMIN")

                        // Housekeeping
                        .requestMatchers(HttpMethod.GET, "/cleaning-tasks/my-tasks").hasAnyRole("HOUSEKEEPING", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/cleaning-tasks/*/start").hasAnyRole("HOUSEKEEPING", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/cleaning-tasks/*/complete").hasAnyRole("HOUSEKEEPING", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/cleaning-tasks").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/cleaning-tasks/*/verify").hasAnyRole("STAFF", "ADMIN")

                        // Payment endpoints
                        .requestMatchers(HttpMethod.POST, "/payments").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/payments/booking/**").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/payments/*/confirm-cash").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/payments/vnpay-callback").permitAll() // VNPay gọi vào

                        //  ADMIN
                        .requestMatchers("/users/**").hasRole("ADMIN")

                        .requestMatchers("/audit-log/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(customJWTDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(new JWTAuthenticationEntryPoint())
                );
        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource()
    {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE","PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("");
        converter.setAuthoritiesClaimName("scope");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
