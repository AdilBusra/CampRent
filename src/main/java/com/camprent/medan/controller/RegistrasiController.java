package com.camprent.medan.controller;

import com.camprent.medan.service.RegistrasiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/registrasi")
public class RegistrasiController {

    @Autowired
    private RegistrasiService registrasiService;

    // Halaman pilih jenis registrasi
    @GetMapping
    public String halamanRegistrasi() {
        return "registrasi/pilih-role";
    }

    // Halaman form registrasi Store
    @GetMapping("/store")
    public String formRegistrasiStore() {
        return "registrasi/form-store";
    }

    // Halaman form registrasi Customer
    @GetMapping("/customer")
    public String formRegistrasiCustomer() {
        return "registrasi/form-customer";
    }

    // Proses registrasi Store
    @PostMapping("/store")
    public String prosesRegistrasiStore(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam String namaToko,
            @RequestParam String alamat,
            @RequestParam String nomorTelepon,
            Model model) {
        try {
            registrasiService.daftarStore(username, password, email,
                    namaToko, alamat, nomorTelepon);
            return "redirect:/login?registrasiSucces";
        } catch (Exception e) {
            model.addAttribute("error", "Registrasi gagal: " + e.getMessage());
            return "registrasi/form-store";
        }
    }

    // Proses registrasi Customer
    @PostMapping("/customer")
    public String prosesRegistrasiCustomer(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam String namaLengkap,
            @RequestParam String nomorTelepon,
            @RequestParam String nik,
            Model model) {
        try {
            registrasiService.daftarCustomer(username, password, email,
                    namaLengkap, nomorTelepon, nik);
            return "redirect:/login?registrasiSucces";
        } catch (Exception e) {
            model.addAttribute("error", "Registrasi gagal: " + e.getMessage());
            return "registrasi/form-customer";
        }
    }
}
