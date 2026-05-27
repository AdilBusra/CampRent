package com.camprent.medan.controller;

import com.camprent.medan.entity.User;
import com.camprent.medan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public String handleLogin(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              Model model) {

        // 1. Cari user di database berdasarkan username
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 2. Validasi password (sementara plain-text sesuai data di seeder)
            if (user.getPassword().equals(password)) {

                // 3. Cek apakah akun aktif
                if (!user.getIsActive()) {
                    model.addAttribute("error", "Akun Anda dinonaktifkan!");
                    return "index"; // Kembali ke landing page sambil bawa pesan eror
                }

                // 4. Logika Pengalihan Halaman berdasarkan ROLE
                if ("ADMIN".equals(user.getRole())) {
                    return "redirect:/admin/dashboard"; // Lempar ke halaman admin
                } else if ("STORE".equals(user.getRole())) {
                    return "redirect:/store/dashboard";
                } else if ("CUSTOMER".equals(user.getRole())) {
                    return "redirect:/catalog";
                }
            }
        }

        // Jika username tidak ketemu atau password salah
        model.addAttribute("error", "Username atau Password salah!");
        return "index";
    }

    // Tambahkan ini di bagian paling bawah LoginController.java kamu, Dil!

    @org.springframework.web.bind.annotation.GetMapping("/login")
    public String showLoginPage(@org.springframework.web.bind.annotation.RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        // Jika URL mengandung parameter ?logout, kirim pesan notifikasi ke halaman utama
        if (logout != null) {
            model.addAttribute("message", "Anda telah berhasil keluar dari sistem.");
        }

        // Kembalikan ke landing page utama (index.html)
        return "index";
    }
}