package com.camprent.medan.controller;

import com.camprent.medan.entity.*;
import com.camprent.medan.service.CustomerService;
import com.camprent.medan.service.TransaksiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TransaksiService transaksiService;


    /**
     * Rute halaman Home/Dashboard utama milik Customer
     */
    @GetMapping("/dashboard")
    public String customerDashboard(Model model, Principal principal) {
        List<Peralatan> listKatalog = customerService.getKatalogUtama();
        model.addAttribute("listKatalog", listKatalog);

        if (principal != null) {
            Customer currentCustomer = customerService.getProfileByUsername(principal.getName());
            model.addAttribute("customer", currentCustomer);
        } else {
            Customer currentCustomer = customerService.getProfileByUsername("amelia");
            model.addAttribute("customer", currentCustomer);
        }
        return "customer/dashboard";
    }

    /**
     * Rute halaman Catalog - Mengarah tepat ke file customer/katalog-list.html
     */
    @GetMapping({"/katalog", "/catalog"})
    public String customerCatalog(@RequestParam(value = "keyword", required = false) String keyword, Model model, Principal principal) {
        List<Peralatan> listKatalog = customerService.getKatalogUtama();

        if (keyword != null && !keyword.trim().isEmpty()) {
            listKatalog = listKatalog.stream()
                    .filter(p -> p.getNamaAlat().toLowerCase().contains(keyword.toLowerCase().trim()) ||
                            p.getMerek().toLowerCase().contains(keyword.toLowerCase().trim()))
                    .toList();
            model.addAttribute("keyword", keyword);
        }

        model.addAttribute("listKatalog", listKatalog);

        if (principal != null) {
            Customer currentCustomer = customerService.getProfileByUsername(principal.getName());
            model.addAttribute("customer", currentCustomer);
        } else {
            Customer currentCustomer = customerService.getProfileByUsername("amelia");
            model.addAttribute("customer", currentCustomer);
        }

        return "customer/katalog-list";
    }

    /**
     * BARU & AMAN: Menangani rute /customer/my-booking agar tidak memicu error 404 lagi.
     * Mengarahkan langsung ke halaman riwayat pemesanan/nota transaksi milik customer.
     */
    @GetMapping("/my-booking")
    public String customerMyBooking(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : null;
        // ✅ ADD THIS BLOCK
        if (username != null) {
            List<Transaksi> bookings = transaksiService.getTransaksiByCustomer(username);
            model.addAttribute("listBooking", bookings);
        } else {
            model.addAttribute("listBooking", java.util.Collections.emptyList());
        }

        return "customer/my-booking";
    }

    @GetMapping("/categories")
    public String customerCategories(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Kategori> listKategori;
        List<Kategori> semuaKategori = customerService.getKatalogUtama().stream()
                .map(Peralatan::getKategori)
                .filter(k -> k != null)
                .distinct()
                .toList();

        if (keyword != null && !keyword.trim().isEmpty()) {
            listKategori = semuaKategori.stream()
                    .filter(k -> k.getNamaKategori().toLowerCase().contains(keyword.toLowerCase().trim()))
                    .toList();
            model.addAttribute("keyword", keyword);
        } else {
            listKategori = semuaKategori;
        }

        model.addAttribute("listKategori", listKategori);
        return "customer/categories";
    }

    @GetMapping("/profile")
    public String customerProfile(Model model, Principal principal) {
        String username = (principal != null) ? principal.getName() : "amelia";
        Customer currentCustomer = customerService.getProfileByUsername(username);
        model.addAttribute("customer", currentCustomer);
        return "customer/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("namaLengkap") String namaLengkap,
                                @RequestParam("email") String email,
                                @RequestParam("nomorTelepon") String nomorTelepon,
                                @RequestParam("nik") String nik,
                                Principal principal) {
        String username = (principal != null) ? principal.getName() : "amelia";
        customerService.updateProfile(username, namaLengkap, email, nomorTelepon, nik);
        return "redirect:/customer/profile";
    }

    // Menampilkan halaman detail peralatan berdasarkan ID secara dinamis
    @GetMapping("/equipment/detail/{id}")
    public String detailPeralatan(@PathVariable("id") Long id, Model model) {
        java.util.Optional<com.camprent.medan.entity.Peralatan> peralatanOpt = customerService.getPeralatanById(id);

        if (peralatanOpt.isEmpty()) {
            return "redirect:/customer/katalog?error=AlatTidakDitemukan";
        }

        model.addAttribute("peralatan", peralatanOpt.get());
        return "customer/equipment-detail";
    }

    // Shortcut Book Now: Tambah ke keranjang lalu langsung arahkan ke halaman cart
    @GetMapping("/booking/{id}")
    public String instantBooking(@PathVariable("id") Long id, Principal principal) {
        String username = (principal != null) ? principal.getName() : "amelia";
        customerService.tambahKeKeranjang(username, id);
        return "redirect:/customer/cart";
    }

    @PostMapping("/logout")
    public String manualLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/store/detail/{id}")
    public String detailStore(@PathVariable("id") Long id, Model model) {
        java.util.Optional<Store> storeOpt = customerService.getStoreById(id);

        if (storeOpt.isEmpty()) {
            return "redirect:/customer/katalog?error=TokoTidakDitemukan";
        }

        Store store = storeOpt.get();
        List<Peralatan> listPeralatanToko = customerService.getPeralatanByStore(store);

        model.addAttribute("store", store);
        model.addAttribute("listKatalog", listPeralatanToko);

        return "customer/store-detail";
    }

    @GetMapping("/cart")
    public String viewCart(Model model, Principal principal) {
        String username = (principal != null) ? principal.getName() : "amelia";
        List<KeranjangItem> listKeranjang = customerService.getKeranjangCustomer(username);
        model.addAttribute("listKeranjang", listKeranjang);
        return "customer/cart";
    }

    // ==========================================
    // METHOD UNTUK OPERASI AJAK / BACKGROUND CART
    // ==========================================

    /**
     * BARU: Menangani request penambahan item ke keranjang belanja melalui AJAX (Fetch).
     * Mencegah halaman berpindah rute/refresh saat tombol "Add to Cart" ditekan.
     * URL: POST /customer/cart/add/{id}
     */
    @PostMapping("/cart/add/{id}")
    @ResponseBody
    public ResponseEntity<?> AJAXAddToCart(@PathVariable("id") Long id, Principal principal) {
        // Ambil data user yang sedang login
        String username = (principal != null) ? principal.getName() : "amelia";

        // Eksekusi penambahan ke database via service
        customerService.tambahKeKeranjang(username, id);

        // Berikan respon HTTP 200 OK kosong supaya dibaca sukses oleh JavaScript front-end
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // METHOD TAMBAHAN UNTUK DELETE & UPDATE CART
    // ==========================================

    /**
     * Menangani aksi hapus item dari keranjang belanja
     * URL: POST /customer/cart/delete/{id}
     */
    @PostMapping("/cart/delete/{id}")
    public String deleteCartItem(@PathVariable("id") Long id) {
        customerService.hapusDariKeranjang(id);
        return "redirect:/customer/cart";
    }

    /**
     * Menangani aksi tambah/kurang kuantitas item di keranjang belanja
     * URL: POST /customer/cart/update/{id}
     */
    @PostMapping("/cart/update/{id}")
    public String updateCartItemQuantity(@PathVariable("id") Long id, @RequestParam("aksi") String aksi) {
        if ("tambah".equalsIgnoreCase(aksi)) {
            customerService.updateKuantitas(id, 1);
        } else if ("kurang".equalsIgnoreCase(aksi)) {
            customerService.updateKuantitas(id, -1);
        }
        return "redirect:/customer/cart";
    }

    @GetMapping("/categories/{id}")
    public String categoryDetail(@PathVariable("id") Long id, Model model) {
        // 1. Ambil data kategori untuk menampilkan nama kategori di header
        Kategori kategori = customerService.getKategoriById(id) // Pastikan method ini ada di CustomerService
                .orElseThrow(() -> new RuntimeException("Kategori tidak ditemukan"));

        // 2. Ambil list peralatan yang memiliki kategori tersebut
        // Anda bisa memanfaatkan relasi atau membuat method baru di service, contoh:
        List<Peralatan> listKatalog = customerService.getPeralatanByKategoriId(id);

        // 3. Masukkan ke model agar bisa dibaca Thymeleaf
        model.addAttribute("categoryName", kategori.getNamaKategori());
        model.addAttribute("listKatalog", listKatalog);

        return "customer/category-detail"; // Mengarah ke template category-detail.html
    }
}