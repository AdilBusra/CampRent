package com.camprent.medan.controller;

import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.service.TransaksiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/customer/booking")
public class BookingController {

    @Autowired
    private TransaksiService transaksiService;

    // ============================================================
    // 1. VALIDATION ENDPOINTS
    // ============================================================

    /**
     * ✅ AJAX: Validasi booking sebelum checkout
     *
     * Endpoint: GET /customer/booking/validate
     * Response: JSON
     *
     * Digunakan di cart.html untuk:
     * - Check apakah semua items dari 1 toko
     * - Disable/enable tombol checkout
     * - Show error message jika ada konflik
     */
    @GetMapping("/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateBooking(Authentication auth) {
        String username = auth != null ? auth.getName() : "guest";

        try {
            Map<String, Object> result = transaksiService.validateBookingBeforeCheckout(username);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("valid", false);
            errorResult.put("message", "Error: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    // ============================================================
    // 2. CHECKOUT & BOOKING CREATION
    // ============================================================

    /**
     * ✅ POST: Submit booking dari cart
     *
     * Endpoint: POST /customer/booking/checkout
     * Form Data:
     * - tanggalSewa: 2026-06-01
     * - tanggalKembali: 2026-06-05
     *
     * ✅ FLOW YANG BENAR:
     * 1. Validasi satu toko
     * 2. Hitung total harga
     * 3. Create Transaksi status PENDING
     * 4. Set waktuExpire = now + 2 jam
     * 5. ⚡ KURANGI STOK LANGSUNG
     * 6. Create DetailTransaksi
     * 7. Clear keranjang
     * 8. Redirect ke booking-success page
     */
    @PostMapping("/checkout")
    public String checkoutBooking(
            @RequestParam("tanggalSewa") String tanggalSewaStr,
            @RequestParam("tanggalKembali") String tanggalKembaliStr,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            String username = auth != null ? auth.getName() : null;

            if (username == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "❌ Anda harus login untuk melakukan booking!");
                return "redirect:/login";
            }

            // 1. Parse tanggal
            LocalDate tanggalSewa = LocalDate.parse(tanggalSewaStr);
            LocalDate tanggalKembali = LocalDate.parse(tanggalKembaliStr);

            // 2. Create booking via service
            // Service akan handle:
            // - Validasi 1 toko
            // - Buat Transaksi PENDING
            // - KURANGI STOK
            // - Clear cart
            Transaksi transaksi = transaksiService.createBookingFromCart(
                    username,
                    tanggalSewa,
                    tanggalKembali
            );

            // 3. Pass data ke success page
            model.addAttribute("bookingId", transaksi.getId());
            model.addAttribute("totalHarga", transaksi.getTotalHarga());
            model.addAttribute("storeNama", transaksi.getStore().getNamaToko());

            System.out.println("✅ Checkout success, redirect ke booking-success");

            return "redirect:/customer/booking/success?id=" + transaksi.getId();

        } catch (Exception e) {
            System.out.println("❌ Booking checkout error: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Booking gagal: " + e.getMessage());

            return "redirect:/customer/cart";
        }
    }

    /**
     * ✅ GET: Success page setelah booking dibuat
     *
     * Endpoint: GET /customer/booking/success?id=123
     *
     * Menampilkan:
     * - Booking ID
     * - Store name
     * - Total price
     * - 2-hour countdown timer ⏱️
     * - Message: "Harap ambil barang dalam 2 jam"
     */
    @GetMapping("/success")
    public String bookingSuccess(
            @RequestParam(value = "id", required = false) Long bookingId,
            Model model,
            Authentication auth) {

        try {
            if (bookingId == null) {
                return "redirect:/customer/dashboard?error=NoBookingId";
            }

            // Get booking details dari database
            Transaksi transaksi = transaksiService.getTransaksiById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking tidak ditemukan"));

            // Validasi: booking milik customer yang login?
            if (auth != null && transaksi.getCustomer() != null) {
                if (!transaksi.getCustomer().getUser().getUsername().equals(auth.getName())) {
                    return "redirect:/customer/dashboard?error=Unauthorized";
                }
            }

            model.addAttribute("bookingId", bookingId);
            model.addAttribute("totalHarga", transaksi.getTotalHarga());
            model.addAttribute("storeNama", transaksi.getStore().getNamaToko());
            model.addAttribute("tanggalSewa", transaksi.getTanggalSewa());
            model.addAttribute("tanggalKembali", transaksi.getTanggalKembali());

            return "customer/booking-success";

        } catch (Exception e) {
            return "redirect:/customer/dashboard?error=BookingNotFound";
        }
    }

    // ============================================================
    // 3. BOOKING DETAILS & STATUS MONITORING
    // ============================================================

    /**
     * ✅ AJAX: Get booking details & remaining time
     *
     * Endpoint: GET /customer/booking/details/{id}
     * Response: JSON dengan booking details
     */
    @GetMapping("/details/{id}")
    @ResponseBody
    public ResponseEntity<?> getBookingDetails(
            @PathVariable Long id,
            Authentication auth) {

        try {
            Transaksi transaksi = transaksiService.getTransaksiById(id)
                    .orElseThrow(() -> new RuntimeException("Booking tidak ditemukan"));

            // Security check
            if (auth != null && transaksi.getCustomer() != null) {
                if (!transaksi.getCustomer().getUser().getUsername().equals(auth.getName())) {
                    return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
                }
            }

            Map<String, Object> details = new HashMap<>();
            details.put("bookingId", transaksi.getId());
            details.put("status", transaksi.getStatusTransaksi());
            details.put("storeName", transaksi.getStore().getNamaToko());
            details.put("totalHarga", transaksi.getTotalHarga());
            details.put("tanggalSewa", transaksi.getTanggalSewa());
            details.put("tanggalKembali", transaksi.getTanggalKembali());
            details.put("remainingMinutes", transaksi.getRemainingMinutes());
            details.put("isExpired", transaksi.isExpired());

            return ResponseEntity.ok(details);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ AJAX: Get remaining time untuk booking
     *
     * Endpoint: GET /customer/booking/{id}/remaining-time
     * Response: JSON dengan sisa waktu dalam menit
     *
     * Digunakan untuk:
     * - Update countdown timer
     * - Check apakah expired
     * - Auto-refresh jika expired
     */
    @GetMapping("/{id}/remaining-time")
    @ResponseBody
    public ResponseEntity<?> getRemainingTime(
            @PathVariable Long id,
            Authentication auth) {

        try {
            Transaksi transaksi = transaksiService.getTransaksiById(id)
                    .orElseThrow(() -> new RuntimeException("Booking tidak ditemukan"));

            // Security check
            if (auth != null && transaksi.getCustomer() != null) {
                if (!transaksi.getCustomer().getUser().getUsername().equals(auth.getName())) {
                    return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("bookingId", id);
            response.put("status", transaksi.getStatusTransaksi());
            response.put("remainingMinutes", transaksi.getRemainingMinutes());
            response.put("isExpired", transaksi.isExpired());
            response.put("message", transaksi.isExpired() ? "Booking sudah expired" : "Booking masih valid");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // 4. HELPER METHOD
    // ============================================================

    /**
     * Helper untuk get transaksi by ID (baru di controller, bisa juga di service)
     */
    // Sebenarnya ini bisa diakses langsung dari service, tapi untuk clarity ditambah di sini
}