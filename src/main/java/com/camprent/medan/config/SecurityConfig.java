package com.camprent.medan.config;

import com.camprent.medan.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthService authService;

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
                // 4. PERBAIKAN: Gunakan hanya SATU formLogin configuration
                .formLogin(form -> form
                        .loginPage("/login")                          // Form login di halaman /login
                        .loginProcessingUrl("/login")                 // Spring Security process login dari URL ini
                        .defaultSuccessUrl("/success-login", true)    // Redirect ke AuthController untuk routing
                        .failureUrl("/login?error=true")              // Jika gagal, kembali ke /login?error=true
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
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(authService);  // Gunakan AuthService untuk load user details

        return http.build();
    }
}