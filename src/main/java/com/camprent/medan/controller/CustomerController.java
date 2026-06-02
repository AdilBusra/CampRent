package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Customer; // Import Customer
import com.camprent.medan.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal; // Untuk mendeteksi user login
import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/dashboard")
    public String customerDashboard(Model model, Principal principal) {
        // 1. Ambil data katalog alat gunung
        List<Peralatan> listKatalog = customerService.getKatalogUtama();
        model.addAttribute("listKatalog", listKatalog);

        // 2. Ambil data user yang sedang login untuk dipasang di Welcome Message
        if (principal != null) {
            Customer currentCustomer = customerService.getProfileByUsername(principal.getName());
            model.addAttribute("customer", currentCustomer);
        } else {
            // Fallback jika belum setup Spring Security (Ganti "amelia" dengan username di databasemu)
            Customer currentCustomer = customerService.getProfileByUsername("amelia");
            model.addAttribute("customer", currentCustomer);
        }

        return "customer/dashboard";
    }

    @GetMapping("/profile")
    public String customerProfile(Model model, Principal principal) {
        Customer currentCustomer;

        if (principal != null) {
            currentCustomer = customerService.getProfileByUsername(principal.getName());
        } else {
            // Fallback mock up jika belum pakai Spring Security
            currentCustomer = customerService.getProfileByUsername("amelia");
        }

        // Kirim data customer ke profile.html
        model.addAttribute("customer", currentCustomer);
        return "customer/profile";
    }

    // ... method lainnya biarkan tetap sama

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

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("namaLengkap") String namaLengkap,
                                @RequestParam("email") String email,
                                @RequestParam("nomorTelepon") String nomorTelepon,
                                @RequestParam("nik") String nik,
                                Principal principal) {

        // Tentukan username akun yang sedang login
        String username;
        if (principal != null) {
            username = principal.getName();
        } else {
            username = "amelia"; // Fallback mockup jika belum pasang Spring Security
        }

        // Panggil service untuk eksekusi update ke DB
        customerService.updateProfile(username, namaLengkap, email, nomorTelepon, nik);

        // Setelah sukses save, redirect kembali ke halaman profile agar datanya langsung ter-refresh
        return "redirect:/customer/profile";
    }

    @PostMapping("/logout")
    public String manualLogout(HttpSession session) {
        session.invalidate(); // Menghapus semua data session login
        return "redirect:/login"; // Diarahkan kembali ke halaman login utama
    }
}