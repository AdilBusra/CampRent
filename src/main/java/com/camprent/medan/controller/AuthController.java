package com.camprent.medan.controller;

import com.camprent.medan.service.AuthService;
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
    public String prosesRegistrasi(@RequestParam Map<String, String> formData, RedirectAttributes redirectAttributes) {
        try {
            // Jalankan logika bisnis pendaftaran
            authService.registerNewUser(formData);

            // Jika sukses, kirim notifikasi ke halaman login
            redirectAttributes.addFlashAttribute("successMessage", "Registrasi berhasil! Silakan masuk ke akun Anda.");
            return "redirect:/login?success";

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
            return "redirect:/store/dashboard"; // Arahkan ke dashboard tokomu yang tadi eror
        } else {
            return "redirect:/customer/dashboard"; // Arahkan ke landing page customer/fitur utama
        }
    }
}