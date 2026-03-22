// package com.trucdnd.gpu_hub_backend.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;

// import lombok.RequiredArgsConstructor;

// @Configuration
// @EnableWebSecurity
// @RequiredArgsConstructor
// public class SecurityConfig {

//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//         http
//             .csrf(csrf -> csrf.disable()) // Tắt chống giả mạo request (bắt buộc để chạy POST/PUT)
//             .authorizeHttpRequests(auth -> auth
//                 .anyRequest().permitAll() // Cho phép tất cả các API
//             )
//             .headers(headers -> headers.frameOptions(frame -> frame.disable())); // Tắt chặn Frame (cho H2 Console nếu cần)
            
//         return http.build();
//     }

//     @Bean
//     PasswordEncoder passwordEncoder() {
//         return new BCryptPasswordEncoder(12);
//     }
// }
