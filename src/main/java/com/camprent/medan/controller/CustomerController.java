package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    /**
     * Menampilkan halaman katalog utama untuk Customer.
     * Endpoint ini juga mendukung pencarian jika parameter 'keyword' dikirim dari form search FE.
     * URL Akses: http://localhost:8080/customer/katalog
     */
    @GetMapping("/katalog")
    public String tampilkanKatalog(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Peralatan> listKatalog;

        // Jika ada keyword pencarian, panggil fungsi cari. Jika tidak, tampilkan semua yang ready.
        if (keyword != null && !keyword.trim().isEmpty()) {
            listKatalog = customerService.cariAlatDiKatalog(keyword);
            model.addAttribute("keyword", keyword); // Dikembalikan ke HTML agar teks di input search tidak hilang
        } else {
            listKatalog = customerService.getKatalogUtama();
        }

        // Kirim list peralatan ke view Thymeleaf
        model.addAttribute("listKatalog", listKatalog);

        return "customer/katalog-list"; // Mengarah ke src/main/resources/templates/customer/katalog-list.html
    }
}