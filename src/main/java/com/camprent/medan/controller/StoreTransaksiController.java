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

        System.out.println("STORE ID = " + store.getId());
        System.out.println("STORE NAME = " + store.getNamaToko());
        System.out.println("3️⃣ Store Name: " + store.getNamaToko());
        System.out.println("4️⃣ Store ID: " + store.getId());

        // 3. Check Peralatan
        List<Peralatan> alatList = peralatanRepository.findByStore(store);

        System.out.println(alatList);
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

        System.out.println("DEBUG LIST ALAT = " + alatList);
        System.out.println("DEBUG LIST TRANSAKSI = " + transaksiList);

// Set model
        model.addAttribute("listAlat", alatList);
        model.addAttribute("listTransaksi", transaksiList);
        model.addAttribute("store", store);

        System.out.println("MODEL listAlat = " + model.getAttribute("listAlat"));
        System.out.println("MODEL listTransaksi = " + model.getAttribute("listTransaksi"));

        System.out.println("🔍 === END DEBUG ===");
        System.out.println("");

        System.out.println("======================");
        System.out.println("LIST ALAT SIZE = " + alatList.size());

        for (Peralatan p : alatList) {
            System.out.println(
                    p.getId() + " | " +
                            p.getNamaAlat() + " | " +
                            p.getStok()
            );
        }
        System.out.println("======================");

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
    // ✅ PERBAIKAN: Mapping nama parameter dari HTML agar pas dengan Controller
    @PostMapping("/save-offline")
    public String simpanOfflineRental(@RequestParam("customerName") String customerName,
                                      @RequestParam("phoneNumber") String phoneNumber, // Pastikan isinya "phoneNumber" sesuai nama input di HTML kamu
                                      @RequestParam("tanggalSewa") String tanggalSewa,
                                      @RequestParam("tanggalKembali") String tanggalKembali,
                                      @RequestParam(value = "peralatanIds", required = false) List<Long> peralatanIds,
                                      @RequestParam(value = "kuantitas", required = false) List<Integer> kuantitas,
                                      Principal principal) {

        // Validasi pencegahan jika toko belum memilih alat camp sama sekali
        if (peralatanIds == null || peralatanIds.isEmpty()) {
            return "redirect:/store/transaksi?error=no_items";
        }

        // Panggil service untuk menyimpan data transaksi offline
        storeTransaksiService.createOfflineRental(principal.getName(), customerName,
                phoneNumber, tanggalSewa, tanggalKembali, peralatanIds, kuantitas);

        // Kembalikan ke halaman daftar transaksi toko
        return "redirect:/store/transaksi";
    }
}