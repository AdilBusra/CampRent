package com.camprent.medan.controller;

import com.camprent.medan.entity.Store;
import com.camprent.medan.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private AdminService adminService;

    // 1. Dashboard Page
    // Ubah method dashboard di dalam HomeController.java menjadi seperti ini:

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(Model model) {
        // Ambil angka real-time dari Service
        model.addAttribute("totalCustomer", adminService.countTotalCustomer());
        model.addAttribute("verifiedStores", adminService.countVerifiedStore());
        model.addAttribute("pendingStores", adminService.countPendingStore());
        model.addAttribute("totalTransactions", adminService.countTotalTransaksi());

        return "admin/dashboard";
    }

    // 2. Halaman Validasi Toko (Menampilkan daftar toko)
    @GetMapping("/admin/stores")
    public String showStoreValidationPage(Model model) {
        List<Store> listToko = adminService.getAllStores();
        // Menyuntikkan listToko ke Thymeleaf model dengan nama variabel 'stores'
        model.addAttribute("stores", listToko);
        return "admin/stores";
    }

    // 3. Action Handler untuk tombol VERIFIED / REJECTED
    @PostMapping("/admin/stores/validasi")
    public String handleValidasiToko(@RequestParam("id") Long id,
                                     @RequestParam("status") String status) {
        adminService.validasiToko(id, status);
        // Setelah status diubah, refresh halaman kembali ke daftar toko
        return "redirect:/admin/stores";
    }

    // Tambahkan ini di dalam HomeController.java kamu

    // 4. Halaman Kelola Kategori Alat
    @GetMapping("/admin/categories")
    public String showCategoryPage(Model model) {
        model.addAttribute("categories", adminService.getAllKategori());
        return "admin/categories";
    }

    // 5. Proses Tambah Kategori Baru
    @PostMapping("/admin/categories/add")
    public String handleTambahKategori(@RequestParam("namaKategori") String namaKategori) {
        adminService.tambahKategori(namaKategori);
        return "redirect:/admin/categories";
    }

    // Tambahkan ini di dalam HomeController.java kamu

    @Autowired
    private com.camprent.medan.repository.TransaksiRepository transaksiRepository; // jika dibutuhkan import-nya

    // 6. Halaman Riwayat Transaksi Global (Read-Only)
    @GetMapping("/admin/transactions")
    public String showGlobalTransactionsPage(Model model) {
        model.addAttribute("transactions", adminService.getAllTransaksiGlobal());
        return "admin/transactions";
    }
}