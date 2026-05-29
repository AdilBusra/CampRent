package com.camprent.medan.controller;

import com.camprent.medan.entity.Store;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.User;
import com.camprent.medan.entity.Customer;
import com.camprent.medan.entity.Transaksi;

import java.util.ArrayList;

@Controller
public class StoreViewController {

    @GetMapping("/store/dashboard")
    public String dashboard(Model model) {
        Store store = new Store();
        store.setNamaToko("Mountain Adventure");

        model.addAttribute("store", store);
        model.addAttribute("totalEquipment", 24);
        model.addAttribute("totalStock", 86);
        model.addAttribute("activeRentals", 7);
        model.addAttribute("totalRevenue", 1500000);

        model.addAttribute("listTransaksi", new ArrayList<>());
        model.addAttribute("lowStockList", new ArrayList<>());

        return "store/dashboard";
    }

    @GetMapping("/store/equipment")
    public String equipmentList(Model model) {
        Store store = new Store();
        store.setNamaToko("Mountain Adventure");

        model.addAttribute("store", store);
        model.addAttribute("listPeralatan", new ArrayList<>());
        model.addAttribute("listKategori", new ArrayList<>());

        return "store/equipment-list";
    }

    @GetMapping("/store/transactions")
    public String transactions(Model model) {

        Store store = new Store();
        store.setNamaToko("Mountain Adventure");

        List<Transaksi> listTransaksi = new ArrayList<>();

        Customer customer = new Customer();
        customer.setNamaLengkap("Amelia Putri");

        Transaksi transaksi = new Transaksi();
        transaksi.setCustomer(customer);
        transaksi.setTanggalSewa(LocalDate.now());
        transaksi.setTanggalKembali(LocalDate.now().plusDays(3));
        transaksi.setTotalHarga(new BigDecimal("250000"));
        transaksi.setStatusTransaksi("DIPAKAI");

        listTransaksi.add(transaksi);

        model.addAttribute("store", store);
        model.addAttribute("listTransaksi", listTransaksi);

        return "store/transaction-list";
    }

    @GetMapping("/store/profile")
    public String storeProfile(Model model) {
        Store store = new Store();
        store.setNamaToko("Mountain Adventure");
        store.setAlamat("Jl. Setia Budi, Medan");
        store.setNomorTelepon("0812-3456-7890");
        store.setStatusVerifikasi("VERIFIED");

        User user = new User();
        user.setEmail("store@camprent.com");
        store.setUser(user);

        model.addAttribute("store", store);

        return "store/profile";
    }

    @GetMapping("/store/preview/peralatan/add")
    public String previewEquipmentForm(Model model) {
        Store store = new Store();
        store.setNamaToko("Mountain Adventure");

        Peralatan peralatan = new Peralatan();

        model.addAttribute("store", store);
        model.addAttribute("peralatan", peralatan);
        model.addAttribute("listKategori", new ArrayList<>());

        return "store/equipment-form";
    }

}