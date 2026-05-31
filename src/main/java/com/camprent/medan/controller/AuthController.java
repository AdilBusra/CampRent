package com.camprent.medan.controller;

import com.camprent.medan.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
}