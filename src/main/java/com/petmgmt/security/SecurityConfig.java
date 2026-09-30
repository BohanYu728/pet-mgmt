package com.petmgmt.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.HttpMethod;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableScheduling
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) { this.jwtAuthenticationFilter = jwtAuthenticationFilter; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        http.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .cors(cors -> {})
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> response.sendError(401)))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/demo/status", "/api/auth/csrf", "/api/auth/login", "/api/auth/register", "/api/auth/merchant/register",
                    "/auth/login", "/auth/register", "/auth/merchant/register").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("admin:manage")
                .requestMatchers(HttpMethod.GET, "/api/shop/api/products/**", "/api/shop/products/**", "/api/hospitals/**").permitAll()
                .requestMatchers("/api/**", "/auth/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/dashboard", "/profile", "/pets", "/pets/add", "/pets/{id}",
                    "/health/pet/{id}", "/photos", "/logs", "/reminders", "/ai/breed", "/ai/emotion",
                    "/ai/health-report", "/ai/recommend", "/consultation", "/hospitals", "/shop",
                    "/shop/cart", "/shop/orders", "/admin", "/admin/users", "/admin/pets",
                    "/admin/merchants", "/merchant", "/merchant/products", "/merchant/orders")
                    .permitAll()
                .requestMatchers("/", "/about", "/css/**", "/js/**", "/images/**", "/uploads/**",
                    "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors-origin:http://localhost:5173,http://127.0.0.1:5173}") String originList) {
        List<String> origins = Arrays.stream(originList.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (origins.isEmpty() || origins.stream().anyMatch(value -> value.contains("*")))
            throw new IllegalArgumentException("CORS_ORIGIN must list explicit origins without wildcards");
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
