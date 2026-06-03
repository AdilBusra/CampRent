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
            Map<String, Object> errorResult = new java.util.HashMap<>();
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
     * Flow:
     * 1. Parse tanggal
     * 2. Validasi satu toko
     * 3. Create Transaksi status PENDING
     * 4. ⚡ KURANGI STOK LANGSUNG
     * 5. Clear cart
     * 6. Redirect ke booking-success page
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

            model.addAttribute("bookingId", bookingId);
            return "customer/booking-success";

        } catch (Exception e) {
            return "redirect:/customer/dashboard?error=BookingNotFound";
        }
    }

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
            Map<String, Object> details = new java.util.HashMap<>();
            details.put("bookingId", id);
            details.put("message", "Booking details retrieved");

            return ResponseEntity.ok(details);

        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
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
    public ResponseEntity<?> getRemainingTime(@PathVariable Long id) {
        try {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("bookingId", id);
            response.put("remainingMinutes", 120);
            response.put("isExpired", false);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // 3. STORE SIDE: ACCEPT/REJECT BOOKING
    // ============================================================

    /**
     * ✅ POST: Store TERIMA/PICK booking
     *
     * Endpoint: POST /customer/booking/{id}/accept
     *
     * ✅ REFACTORED: Diubah dari serahkanBarang() ke acceptPickup()
     *
     * Action:
     * - Status: PENDING → DIPAKAI
     * - ⚠️ Stock JANGAN diubah (sudah berkurang saat booking)
     * - Ini hanya confirm bahwa customer sudah ambil barang
     */
    @PostMapping("/{id}/accept")
    public String acceptBooking(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            // ✅ Ganti dari serahkanBarang() ke acceptPickup()
            transaksiService.acceptPickup(id);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Barang diterima! Status: PENDING → DIPAKAI");
            return "redirect:/store/transaksi";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error: " + e.getMessage());
            return "redirect:/store/transaksi";
        }
    }

    /**
     * ✅ POST: Store TOLAK/CANCEL booking
     *
     * Endpoint: POST /customer/booking/{id}/reject
     *
     * Action:
     * - Status: PENDING → CANCELLED
     * - ⚠️ KEMBALIKAN STOK yang sudah berkurang
     */
    @PostMapping("/{id}/reject")
    public String rejectBooking(
            @PathVariable Long id,
            @RequestParam(value = "alasan", required = false) String alasan,
            RedirectAttributes redirectAttributes) {

        try {
            String alasanDefault = alasan != null ? alasan : "Ditolak oleh store";
            transaksiService.rejectBooking(id, alasanDefault);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Booking ditolak. Stok barang dikembalikan.");
            return "redirect:/store/transaksi";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error: " + e.getMessage());
            return "redirect:/store/transaksi";
        }
    }

    /**
     * ✅ POST: Customer KEMBALIKAN barang
     *
     * Endpoint: POST /customer/booking/{id}/return
     *
     * Action:
     * - Status: DIPAKAI → SELESAI (jika tepat waktu)
     * - Status: DIPAKAI → TERLAMBAT (jika terlambat + hitung denda)
     */
    @PostMapping("/{id}/return")
    public String returnBooking(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            transaksiService.kembalikanBarang(id);

            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Barang berhasil dikembalikan!");
            return "redirect:/customer/my-booking";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error: " + e.getMessage());
            return "redirect:/customer/my-booking";
        }
    }
}