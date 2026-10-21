package com.shuimumu.fuluogame.config;

import com.shuimumu.fuluogame.interceptor.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, StringRedisTemplate stringRedisTemplate,
                                          CorsConfigurationSource corsConfigurationSource) throws Exception {
        TokenAuthenticationFilter tokenFilter = new TokenAuthenticationFilter(stringRedisTemplate);
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> response.setStatus(401))
                .accessDeniedHandler((request, response, accessDeniedException) -> response.setStatus(403))
            )
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(
                    "/user/login", "/user/login/password", "/user/sendCode",
                    "/post/list", "/post/search", "/search",
                    "/post/*/comments", "/post/*/counts", "/post/*/status",
                    "/post/batch-counts", "/post/batch-status",
                    "/post/comment/*/replies",
                    "/topic/**",
                    "/banners", "/announcements",
                    "/ai/**",
                    "/doc.html", "/swagger-ui/**", "/v3/api-docs/**",
                    "/error", "/webjars/**", "/favicon.ico"
                ).permitAll();
                auth.requestMatchers(r -> {
                    String uri = r.getRequestURI();
                    String method = r.getMethod();
                    return "GET".equals(method) && (
                        uri.matches("^/post/\\d+(/.*)?") ||
                        uri.matches("^/user/\\d+(/.*)?"));
                }).permitAll();
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
