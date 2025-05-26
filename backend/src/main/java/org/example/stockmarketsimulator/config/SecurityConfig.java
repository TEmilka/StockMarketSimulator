package org.example.stockmarketsimulator.config;

import java.util.List;

import org.example.stockmarketsimulator.security.CustomAccessDeniedHandler;
import org.example.stockmarketsimulator.security.CustomUserDetailsService;
import org.example.stockmarketsimulator.security.JwtAuthenticationFilter;
import org.example.stockmarketsimulator.security.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true); // Jeśli korzystasz z autoryzacji przez cookie lub JWT w headerze

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService, JwtUtils jwtUtils) {
        return new JwtAuthenticationFilter(userDetailsService, jwtUtils);
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"" + authException.getMessage() + "\", \"status\": 401}");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   CustomUserDetailsService userDetailsService,
                                                   CustomAccessDeniedHandler accessDeniedHandler,
                                                   AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // <- DODAJ TO!
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/*.js", "/*.css", "/*.ico", "/assets/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/api/docs/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/api/docs-ui", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**", "/api/swagger-ui/index.html").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/notify").permitAll()
                        .requestMatchers("/api/assets").permitAll()
                        .requestMatchers("/api/users/{userId}/wallet/details").authenticated()
                        .requestMatchers("/api/users/{userId}/wallet/add").authenticated()
                        .requestMatchers("/api/users/{userId}/wallet/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/users/{id}/add-funds**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/users/{id}").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/assets/{id}/history").permitAll()
                        .requestMatchers("/user-wallet/**").permitAll()
                        .requestMatchers("/add-asset").permitAll()
                        .requestMatchers("/add-user").permitAll()
                        .requestMatchers("/api/users/{userId}/wallet/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handler -> {
                    handler.accessDeniedHandler(accessDeniedHandler);
                    handler.authenticationEntryPoint(authenticationEntryPoint);
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}