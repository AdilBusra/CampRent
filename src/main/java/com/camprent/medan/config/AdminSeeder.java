package com.camprent.medan.config;

import com.camprent.medan.entity.User;
import com.camprent.medan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder; // 1. Tambahkan import ini
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 2. Suntikkan PasswordEncoder di sini

    @Override
    public void run(String... args) throws Exception {
        // 1. Cek berapa jumlah user dengan role "ADMIN" saat ini di database
        long adminCount = userRepository.countByRole("ADMIN");

        // 2. Jika jumlah admin masih kurang dari 4, kita suntikkan data admin default
        if (adminCount == 0) {
            System.out.println("=== [SEEDER] Membuat 4 Akun Admin Default CampRent ===");

            // Kamu bisa sesuaikan username, password, dan email default di bawah ini
            createDefaultAdmin("admin1", "admin123", "admin1@camprent.com");
            createDefaultAdmin("admin2", "admin123", "admin2@camprent.com");
            createDefaultAdmin("admin3", "admin123", "admin3@camprent.com");
            createDefaultAdmin("admin4", "admin123", "admin4@camprent.com");

            System.out.println("=== [SEEDER] Sukses Mengunci 4 Akun Admin di Database ===");
        } else {
            System.out.println("=== [SEEDER] Akun Admin Sudah Terkunci di Database (Total: " + adminCount + ") ===");
        }
    }

    private void createDefaultAdmin(String username, String password, String email) {
        User admin = new User();
        admin.setUsername(username);

        // 3. KUNCI PERBAIKAN: Password mentah dibungkus dengan passwordEncoder.encode()
        admin.setPassword(passwordEncoder.encode(password));

        admin.setEmail(email);
        admin.setRole("ADMIN"); // Sesuai aturan entity: "ADMIN"
        admin.setIsActive(true);

        userRepository.save(admin);
    }
}