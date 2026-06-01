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

    // Menampilkan semua orderan sewa yang masuk ke Toko tersebut
    @GetMapping
    public String listTransaksiStore(Model model, Authentication auth) {
        // 1. Ambil username yang sedang login
        String username = auth.getName();

        // 2. Samakan cara pencarian toko dengan yang ada di PeralatanController
        // Jika di PeralatanController kamu menggunakan repository atau service lain, ikuti cara itu.
        Store store = storeRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Toko tidak ditemukan"));

        // 3. Ambil daftar transaksi untuk tabel list
        model.addAttribute("listTransaksi", storeTransaksiService.getTransaksiByStore(username));

        // 4. KITA AMBIL SAMA PERSIS MENGGUNAKAN SERVICE YANG DIPAKAI DI EQUIPMENT LIST
        // Panggil peralatanService dengan parameter store yang sama seperti di halaman peralatan
        // Hapus filter store sementara waktu untuk uji coba jalur data Thymeleaf -> JS
        List<Peralatan> alatList = peralatanRepository.findAll();
        model.addAttribute("listAlat", alatList);

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

    // Tambahkan di dalam StoreTransaksiController.java

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
