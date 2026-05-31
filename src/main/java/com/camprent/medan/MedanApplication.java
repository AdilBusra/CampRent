package com.camprent.medan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class MedanApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedanApplication.class, args);
    }

    /**
     * Meletakkan Bean PasswordEncoder di kelas utama memutus Circular Reference
     * karena akan dimuat paling awal secara mandiri oleh Spring Container.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}