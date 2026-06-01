package com.camprent.medan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // 1. Izinkan akses ke H2 Console, halaman login, register, dan aset statis tanpa login
                        .requestMatchers("/h2-console/**", "/login", "/register", "/registrasi/**", "/css/**", "/js/**", "/images/**").permitAll()

                        // 2. Batasi rute dashboard hanya untuk pengguna yang sudah login sesuai role-nya
                        .requestMatchers("/store/**").hasAnyAuthority("STORE")
                        .requestMatchers("/admin/**").hasAnyAuthority("ADMIN")
                        .requestMatchers("/customer/**").hasAuthority("CUSTOMER")

                        // 3. Sisa request lainnya (seperti landing page customer) wajib login dulu
                        .anyRequest().authenticated()
                )
                // 4. Konfigurasi Form Login agar mengarah ke Cara B (/success-login)
                .formLogin(form -> form
                        .loginPage("/login") // Menunjuk ke view template login kalian
                        .defaultSuccessUrl("/success-login", true) // Lempar ke AuthController perantara
                        .permitAll()
                )
                // 5. Fitur Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}