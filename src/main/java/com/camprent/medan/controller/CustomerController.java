package com.camprent.medan.controller;

import com.camprent.medan.entity.KeranjangItem;
import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Customer; // Import Customer
import com.camprent.medan.entity.Store;
import com.camprent.medan.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // Tampilkan Halaman Cart secara Dinamis
    @GetMapping("/cart")
    public String customerCart(Model model, Principal principal) {
        String username = (principal != null) ? principal.getName() : "amelia";

        List<KeranjangItem> listKeranjang = customerService.getKeranjangCustomer(username);
        model.addAttribute("listKeranjang", listKeranjang);

        return "customer/cart";
    }

    // Tambah Item ke Keranjang via AJAX (Tanpa Redirect)
    @PostMapping("/cart/add")
    @ResponseBody // <-- Tambahkan ini agar mengembalikan data/status, bukan nyari file HTML
    public org.springframework.http.ResponseEntity<String> tambahItem(@RequestParam("peralatanId") Long peralatanId, Principal principal) {
        String username = (principal != null) ? principal.getName() : "amelia";
        try {
            customerService.tambahKeKeranjang(username, peralatanId);
            return org.springframework.http.ResponseEntity.ok("Berhasil menambahkan ke keranjang!");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body("Gagal: " + e.getMessage());
        }
    }

    // Hapus Item dari Keranjang
    @PostMapping("/cart/delete/{id}")
    public String hapusItem(@PathVariable Long id) {
        customerService.hapusDariKeranjang(id);
        return "redirect:/customer/cart";
    }

    // Update Jumlah Kuantitas
    @PostMapping("/cart/update/{id}")
    public String updateKuantitas(@PathVariable Long id, @RequestParam("aksi") String aksi) {
        int jumlah = aksi.equals("tambah") ? 1 : -1;
        customerService.updateKuantitas(id, jumlah);
        return "redirect:/customer/cart";
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

    @GetMapping("/checkout")
    public String checkoutConfirmation(
            @RequestParam("selectedItems") List<Long> selectedItemIds,
            @RequestParam("rentalDate") String rentalDate,
            @RequestParam("returnDate") String returnDate,
            Model model, Principal principal) {

        // 1. Ambil data customer yang sedang login
        String username = (principal != null) ? principal.getName() : "amelia";

        // 2. Kamu bisa memproses list item keranjang berdasarkan ID yang dipilih saja
        // (Opsional: buat method baru di CustomerService untuk filter list item berdasarkan list ID ini)

        // Kirim data ke page konfirmasi booking final
        model.addAttribute("selectedItemIds", selectedItemIds);
        model.addAttribute("rentalDate", rentalDate);
        model.addAttribute("returnDate", returnDate);

        return "customer/checkout-confirmation"; // Buat file HTML baru ini jika diperlukan untuk review akhir
    }

}