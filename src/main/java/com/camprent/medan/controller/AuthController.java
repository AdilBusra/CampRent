package com.camprent.medan.controller;

import com.camprent.medan.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Menerima kiriman data dari Form Registrasi Dinamis Modal HTML
     */
    @PostMapping("/register")
    public String prosesRegistrasi(@RequestParam Map<String, String> formData,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            // 1. Jalankan logika bisnis pendaftaran ke database
            authService.registerNewUser(formData);

            // 2. Ambil parameter mentah untuk proses otentikasi otomatis
            String username = formData.get("username");
            String password = formData.get("password");
            String role = formData.get("role");

            // 3. Eksekusi Auto-Login secara programmatik
            request.login(username, password);

            // 4. Pengalihan langsung (Redirect) tanpa meminta login ulang
            if ("ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            } else if ("STORE".equals(role)) {
                return "redirect:/store/dashboard";
            } else {
                return "redirect:/customer/dashboard";
            }

        } catch (Exception e) {
            // Jika gagal (misal username kembar), tangkap erornya dan balikkan ke halaman utama dengan pesan gagal
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login?error_register";
        }
    }

    @GetMapping("/success-login")
    public String redirectAfterLogin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        // Membaca authorities/role dari user yang berhasil login
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        boolean isStore = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STORE") || a.getAuthority().equals("STORE"));

        if (isAdmin) {
            return "redirect:/admin/dashboard"; // Arahkan ke dashboard admin
        } else if (isStore) {
            return "redirect:/store/dashboard"; // Arahkan ke dashboard tokomu
        } else {
            return "redirect:/customer/dashboard"; // Arahkan ke landing page customer/fitur utama
        }
    }
}