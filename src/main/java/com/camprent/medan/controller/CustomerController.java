package com.camprent.medan.controller;

import com.camprent.medan.entity.KeranjangItem;
import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Customer;
import com.camprent.medan.entity.Kategori;
import com.camprent.medan.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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
        if (principal != null) {
            Customer currentCustomer = customerService.getProfileByUsername(principal.getName());
            model.addAttribute("customer", currentCustomer);
        } else {
            Customer currentCustomer = customerService.getProfileByUsername("amelia");
            model.addAttribute("customer", currentCustomer);
        }

        // Mengembalikan ke file template view customer/my-booking.html atau sejenisnya di proyekmu
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

    @GetMapping("/categories/{categoryName}")
    public String viewCategoryDetail(@PathVariable("categoryName") String categoryName, Model model) {
        List<Peralatan> listPeralatanSesuaiKategori = customerService.getKatalogUtama().stream()
                .filter(p -> p.getKategori() != null && p.getKategori().getNamaKategori().equalsIgnoreCase(categoryName))
                .toList();

        model.addAttribute("listKatalog", listPeralatanSesuaiKategori);
        model.addAttribute("selectedCategory", categoryName);
        return "customer/category-detail";
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
}