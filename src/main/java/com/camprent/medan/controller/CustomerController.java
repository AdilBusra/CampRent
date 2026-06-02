package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Customer; // Import Customer
import com.camprent.medan.entity.Store;
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

    // Menampilkan halaman detail peralatan berdasarkan ID secara dinamis
    @GetMapping("/equipment/detail/{id}")
    public String detailPeralatan(@PathVariable("id") Long id, Model model) {
        // Ambil data peralatan dari database menggunakan service yang sudah ada
        java.util.Optional<com.camprent.medan.entity.Peralatan> peralatanOpt = customerService.getPeralatanById(id);

        if (peralatanOpt.isEmpty()) {
            return "redirect:/customer/katalog?error=AlatTidakDitemukan";
        }

        // Kirim objek peralatan ke dalam template HTML
        model.addAttribute("peralatan", peralatanOpt.get());
        return "customer/equipment-detail";
    }

    // Shortcut Book Now: Tambah ke keranjang lalu langsung arahkan ke halaman cart
    @GetMapping("/booking/{id}")
    public String instantBooking(@PathVariable("id") Long id, Principal principal) {
        // 1. Deteksi username customer yang sedang login
        String username = (principal != null) ? principal.getName() : "amelia";

        // 2. Manfaatkan fungsi tambah ke keranjang yang sudah kita buat di CustomerService kemarin
        customerService.tambahKeKeranjang(username, id);

        // 3. Setelah sukses masuk ke database keranjang, langsung lempar user ke halaman cart
        return "redirect:/customer/cart";
    }

    @PostMapping("/logout")
    public String manualLogout(HttpSession session) {
        session.invalidate(); // Menghapus semua data session login
        return "redirect:/login"; // Diarahkan kembali ke halaman login utama
    }

    @GetMapping("/store/detail/{id}")
    public String detailStore(@PathVariable("id") Long id, Model model) {
        // 1. Ambil data toko berdasarkan ID
        java.util.Optional<Store> storeOpt = customerService.getStoreById(id);

        if (storeOpt.isEmpty()) {
            return "redirect:/customer/katalog?error=TokoTidakDitemukan";
        }

        Store store = storeOpt.get();

        // 2. Ambil semua peralatan gunung yang dijual oleh toko ini saja
        List<Peralatan> listPeralatanToko = customerService.getPeralatanByStore(store);

        // 3. Masukkan ke model agar bisa dibaca Thymeleaf
        model.addAttribute("store", store);
        model.addAttribute("listKatalog", listPeralatanToko); // kita pakai nama listKatalog agar mudah dilooping

        return "customer/store-detail";
    }
}