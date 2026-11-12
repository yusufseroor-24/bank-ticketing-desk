package com.ga.bankdesk.config;

import com.ga.bankdesk.security.InternalKeyFilter;
import com.ga.bankdesk.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity
@RequiredArgsConstructor
@Configuration
//which route can be accessed without login and which needs token / jwtAuthFilter
public class SecurityConfiguration {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalKeyFilter internalKeyFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        //service-to-service calls (protected by the X-Internal-Key header)
                        .requestMatchers("/internal/**").hasRole("INTERNAL")
                        .anyRequest().authenticated()); //everything else needs Jwt

        //filters added and checked before reaching controllers
        http.addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
