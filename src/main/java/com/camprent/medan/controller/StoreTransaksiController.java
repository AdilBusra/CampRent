package com.camprent.medan.controller;

import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.service.StoreTransaksiService;
import org.springframework.beans.factory.annotation.Autowired;
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

    // Menampilkan semua orderan sewa yang masuk ke Toko tersebut
    @GetMapping
    public String listTransaksiStore(Model model, Principal principal) {
        List<Transaksi> listTransaksi = storeTransaksiService.getTransaksiByStore(principal.getName());
        model.addAttribute("listTransaksi", listTransaksi);
        return "store/transaksi-list"; // Mengarah ke halaman HTML Thymeleaf tim FE
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
}
