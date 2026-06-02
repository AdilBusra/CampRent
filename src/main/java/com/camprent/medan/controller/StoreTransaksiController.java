package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.repository.PeralatanRepository;
import com.camprent.medan.repository.StoreRepository;
import com.camprent.medan.service.PeralatanService;
import com.camprent.medan.service.StoreTransaksiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/store/transaksi")
public class StoreTransaksiController {

    @Autowired
    private StoreTransaksiService storeTransaksiService;

    @Autowired
    private PeralatanService peralatanService;

    @Autowired
    private PeralatanRepository peralatanRepository;

    @Autowired
    private StoreRepository storeRepository;

    // ✅ PERBAIKAN UTAMA: Menampilkan list transaksi + list peralatan toko yang login
    @GetMapping
    public String listTransaksiStore(Model model, Authentication auth) {
        System.out.println("🔍 === START DEBUG ===");

        // 1. Check username
        String username = auth.getName();
        System.out.println("1️⃣ Username: " + username);

        // 2. Check Store
        var storeOptional = storeRepository.findByUserUsername(username);
        System.out.println("2️⃣ Store ditemukan? " + storeOptional.isPresent());

        if (storeOptional.isEmpty()) {
            System.out.println("❌ STORE TIDAK DITEMUKAN!");
            System.out.println("   Cek database: SELECT * FROM stores WHERE user_id = ?");
            System.out.println("   Cek users: SELECT * FROM users WHERE username = '" + username + "'");
        }

        Store store = storeOptional
                .orElseThrow(() -> new RuntimeException("Toko tidak ditemukan"));
        System.out.println("3️⃣ Store Name: " + store.getNamaToko());
        System.out.println("4️⃣ Store ID: " + store.getId());

        // 3. Check Peralatan
        List<Peralatan> alatList = peralatanService.getPeralatanByStore(store);
        System.out.println("5️⃣ Total Peralatan: " + alatList.size());

        if (alatList.isEmpty()) {
            System.out.println("❌ PERALATAN KOSONG!");
            System.out.println("   Cek database: SELECT * FROM peralatans WHERE store_id = " + store.getId());
        } else {
            alatList.forEach(alat -> {
                System.out.println("   ✅ " + alat.getNamaAlat() +
                        " (Harga: " + alat.getHargaSewaPerHari() +
                        ", Stok: " + alat.getStok() + ")");
            });
        }

        // 4. Check Transaksi
        List<Transaksi> transaksiList = storeTransaksiService.getTransaksiByStore(username);
        System.out.println("6️⃣ Total Transaksi: " + transaksiList.size());

        // Set model
        model.addAttribute("listAlat", alatList);
        model.addAttribute("listTransaksi", transaksiList);
        model.addAttribute("store", store);

        System.out.println("🔍 === END DEBUG ===");
        System.out.println("");

        return "store/transaction-list";
    }

    // Aksi ketika klik tombol "Serahkan Barang" (Ubah ke DIPAKAI)
    @PostMapping("/serahkan/{id}")
    public String prosesSerahBarang(@PathVariable("id") Long id) {
        storeTransaksiService.serahkanBarang(id);
        return "redirect:/store/transaksi";
    }

    // Aksi ketika klik tombol "Terima Pengembalian" (Ubah ke SELESAI / TERLAMBAT + Denda)
    @PostMapping("/kembalikan/{id}")
    public String prosesKembaliBarang(@PathVariable("id") Long id) {
        storeTransaksiService.kembalikanBarang(id);
        return "redirect:/store/transaksi";
    }

    // ✅ PERBAIKAN: Simpan offline rental dengan stock tracking
    @PostMapping("/save-offline")
    public String simpanOfflineRental(@RequestParam String customerName,
                                      @RequestParam String phoneNumber,
                                      @RequestParam String tanggalSewa,
                                      @RequestParam String tanggalKembali,
                                      @RequestParam List<Long> peralatanIds,
                                      @RequestParam List<Integer> kuantitas,
                                      Principal principal) {

        storeTransaksiService.createOfflineRental(principal.getName(), customerName,
                phoneNumber, tanggalSewa, tanggalKembali, peralatanIds, kuantitas);

        return "redirect:/store/transaksi";
    }
}