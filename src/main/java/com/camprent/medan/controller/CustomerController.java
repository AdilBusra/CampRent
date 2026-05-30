package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/dashboard")
    public String customerDashboard(Model model) {
        List<Peralatan> listKatalog = customerService.getKatalogUtama();
        model.addAttribute("listKatalog", listKatalog);
        return "customer/dashboard";
    }

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

    @GetMapping("/categories")
    public String customerCategories() {
        return "customer/categories";
    }

    @GetMapping("/equipment/{id}")
    public String equipmentDetail(@PathVariable Long id) {
        return "customer/equipment-detail";
    }

    @GetMapping("/cart")
    public String customerCart() {
        return "customer/cart";
    }

    @GetMapping("/my-booking")
    public String myBooking() {
        return "customer/my-booking";
    }

    @GetMapping("/store/{id}")
    public String customerStoreDetail(@PathVariable Long id) {
        return "customer/store-detail";
    }

    @GetMapping("/categories/{categoryName}")
    public String customerCategoryDetail(@PathVariable String categoryName, Model model) {
        model.addAttribute("categoryName", categoryName);
        return "customer/category-detail";
    }

    @GetMapping("/booking-success")
    public String bookingSuccess() {
        return "customer/booking-success";
    }

    @GetMapping("/profile")
    public String customerProfile() {
        return "customer/profile";
    }
}