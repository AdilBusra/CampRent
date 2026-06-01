package com.camprent.medan.controller;

import com.camprent.medan.entity.Store;
import com.camprent.medan.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // 1. Dashboard Page dengan Data Real-Time
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalCustomer", adminService.countTotalCustomer());
        model.addAttribute("verifiedStores", adminService.countVerifiedStore());
        model.addAttribute("pendingStores", adminService.countPendingStore());
        model.addAttribute("totalTransactions", adminService.countTotalTransaksi());
        return "admin/dashboard";
    }

    // 2. Halaman Validasi Daftar Toko
    @GetMapping("/stores")
    public String kelolaToko(Model model) {
        List<Store> listToko = adminService.getAllStores();
        model.addAttribute("stores", listToko);
        return "admin/stores";
    }

    // 3. Action Handler untuk tombol VERIFIED / REJECTED
    @PostMapping("/stores/validasi")
    public String handleValidasiToko(@RequestParam("id") Long id,
                                     @RequestParam("status") String status) {
        adminService.validasiToko(id, status);
        return "redirect:/admin/stores";
    }

    // 4. Halaman Kelola Kategori Alat
    @GetMapping("/categories")
    public String kelolaKategori(Model model) {
        model.addAttribute("categories", adminService.getAllKategori());
        return "admin/categories";
    }

    // 5. Proses Tambah Kategori Baru
    @PostMapping("/categories/add")
    public String handleTambahKategori(@RequestParam("namaKategori") String namaKategori) {
        adminService.tambahKategori(namaKategori);
        return "redirect:/admin/categories";
    }

    // 6. Halaman Riwayat Transaksi Global
    @GetMapping("/transactions")
    public String pantauTransaksi(Model model) {
        model.addAttribute("transactions", adminService.getAllTransaksiGlobal());
        return "admin/transactions";
    }
}