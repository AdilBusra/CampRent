package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
import com.camprent.medan.repository.StoreRepository;
import com.camprent.medan.service.PeralatanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/store")
public class StoreController {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PeralatanService peralatanService;

    @Autowired
    private com.camprent.medan.service.AdminService adminService;

    // Helper method agar kita tidak capek nulis ulang logika nyari toko
    private Store getLoggedInStore(Authentication auth) {
        return storeRepository.findByUserUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Data Toko tidak ditemukan!"));
    }

    // 1. Dashboard Toko
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        Store store = getLoggedInStore(auth);
        List<Peralatan> listAlat = peralatanService.getPeralatanByStore(store);

        model.addAttribute("store", store); // Wajib untuk Navbar
        model.addAttribute("totalEquipment", listAlat.size());
        model.addAttribute("totalStock", listAlat.stream().mapToInt(Peralatan::getStok).sum());

        model.addAttribute("activeRentals", 0);
        model.addAttribute("totalRevenue", 0);
        model.addAttribute("listTransaksi", new ArrayList<>());
        model.addAttribute("lowStockList", new ArrayList<>());

        return "store/dashboard";
    }

    // 4. Daftar Transaksi (URL: /store/transactions)
    @GetMapping("/transactions")
    public String listTransactions(Model model, Authentication auth) {
        Store store = getLoggedInStore(auth);

        model.addAttribute("store", store);      // KITA TAMBAHKAN INI AGAR NAVBAR AMAN

        return "store/transaction-list";
    }

    // 5. Profil Toko (URL: /store/profile)
    @GetMapping("/profile")
    public String storeProfile(Model model, Authentication auth) {
        Store store = getLoggedInStore(auth);

        model.addAttribute("store", store);      // KITA TAMBAHKAN INI AGAR NAVBAR AMAN

        return "store/profile";
    }
}