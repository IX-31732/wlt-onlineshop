package com.onlineshop.onlineshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //使用Session认证，因此禁用CSRF保护
                .csrf(AbstractHttpConfigurer::disable)
                //配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //允许OPTIONS请求通过
                .authorizeHttpRequests(auth -> auth
                        //允许所有OPTIONS请求（预检请求）
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        //静态资源公开
                        .requestMatchers("/uploads/**").permitAll()
                        //上传接口公开
                        .requestMatchers("/api/upload/**").permitAll()
                        //首页和商品相关页面不需要登录
                        .requestMatchers("/", "/home", "/index").permitAll()
                        .requestMatchers("/products", "/product/**").permitAll()
                        .requestMatchers("/about").permitAll()
                        //API接口权限控制
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/goods/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/cart/**").permitAll()
                        .requestMatchers("/api/orders/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/merchant/stats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/merchant/**").permitAll()
                        //只有商家管理操作需要MERCHANT角色
                        .requestMatchers(HttpMethod.POST, "/api/merchant/**").hasRole("MERCHANT")
                        .requestMatchers(HttpMethod.PUT, "/api/merchant/**").hasRole("MERCHANT")
                        .requestMatchers(HttpMethod.DELETE, "/api/merchant/**").hasRole("MERCHANT")
                        .anyRequest().permitAll()
                )
                //Session管理配置
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()  //Session固定保护
                        .maximumSessions(1)                  //每个用户最多1个Session
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/api/auth/session-expired")  //添加Session过期处理
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8081"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization", "Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}