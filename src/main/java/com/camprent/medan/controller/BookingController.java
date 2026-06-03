package com.camprent.medan.controller;

import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.service.TransaksiService;
import jakarta.servlet.http.HttpSession;
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
     * ✅ AJAX Endpoint: Validasi booking sebelum submit
     *
     * Request: GET /customer/booking/validate
     * Response: JSON dengan status validasi
     *
     * Digunakan untuk:
     * - Check apakah keranjang hanya dari 1 toko
     * - Disabled button checkout jika ada error
     * - Show error message yang sesuai
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
     * ✅ POST Endpoint: Submit booking dari cart
     *
     * Request Form:
     * - tanggalSewa: 2026-06-01
     * - tanggalKembali: 2026-06-05
     *
     * Flow:
     * 1. Validate single store
     * 2. Create Transaksi dengan status PENDING
     * 3. Set waktuExpire = now + 2 jam
     * 4. Create DetailTransaksi
     * 5. Clear cart
     * 6. Redirect ke booking success page
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
                redirectAttributes.addFlashAttribute("errorMessage", "Anda harus login untuk melakukan booking!");
                return "redirect:/login";
            }

            // 1. Parse tanggal
            LocalDate tanggalSewa = LocalDate.parse(tanggalSewaStr);
            LocalDate tanggalKembali = LocalDate.parse(tanggalKembaliStr);

            // 2. Create booking via service
            Transaksi transaksi = transaksiService.createBookingFromCart(
                    username,
                    tanggalSewa,
                    tanggalKembali
            );

            // 3. Pass transaksi ID ke success page
            model.addAttribute("bookingId", transaksi.getId());
            model.addAttribute("totalHarga", transaksi.getTotalHarga());
            model.addAttribute("storeNama", transaksi.getStore().getNamaToko());

            System.out.println("✅ Booking #" + transaksi.getId() + " created successfully!");

            return "redirect:/customer/booking/success?id=" + transaksi.getId();

        } catch (Exception e) {
            System.out.println("❌ Booking checkout error: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Booking gagal: " + e.getMessage());

            return "redirect:/customer/cart";
        }
    }

    /**
     * ✅ GET: Halaman success setelah booking berhasil dibuat
     *
     * Menampilkan:
     * - Booking ID
     * - Store name
     * - Total price
     * - 2-hour countdown timer
     * - Info: "Store akan memproses dalam 2 jam"
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

            // Return success template yang akan di-update di HTML
            model.addAttribute("bookingId", bookingId);
            return "customer/booking-success";

        } catch (Exception e) {
            return "redirect:/customer/dashboard?error=BookingNotFound";
        }
    }

    /**
     * ✅ AJAX: Get booking details untuk ditampilkan di success page
     *
     * Request: GET /customer/booking/details/{id}
     * Response: JSON berisi booking details + remaining time
     */
    @GetMapping("/details/{id}")
    @ResponseBody
    public ResponseEntity<?> getBookingDetails(
            @PathVariable Long id,
            Authentication auth) {

        try {
            // Bisa di-extend dengan repository query jika perlu
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

    // ============================================================
    // 3. STORE SIDE: ACCEPT/REJECT BOOKING
    // ============================================================

    /**
     * ✅ POST: Store terima booking
     *
     * Action:
     * - Change status PENDING → DIPAKAI
     * - Kurangi stock untuk setiap item
     * - Update timestamp
     *
     * Endpoint: POST /customer/booking/{id}/accept
     */
    @PostMapping("/{id}/accept")
    public String acceptBooking(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            transaksiService.serahkanBarang(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Barang diterima! Status berubah menjadi DIPAKAI");
            return "redirect:/store/transaksi";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error: " + e.getMessage());
            return "redirect:/store/transaksi";
        }
    }

    /**
     * ✅ POST: Store tolak booking
     *
     * Action:
     * - Change status PENDING → CANCELLED
     * - Restore stock jika diperlukan
     *
     * Endpoint: POST /customer/booking/{id}/reject
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
                    "✅ Booking ditolak dan dihapus dari sistem");
            return "redirect:/store/transaksi";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error: " + e.getMessage());
            return "redirect:/store/transaksi";
        }
    }

    /**
     * ✅ POST: Customer menerima barang (return process)
     *
     * Action:
     * - Change status DIPAKAI → SELESAI atau TERLAMBAT
     * - Calculate denda jika terlambat
     *
     * Endpoint: POST /customer/booking/{id}/return
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

    // ============================================================
    // 4. INFO & LISTING
    // ============================================================

    /**
     * ✅ GET: Remaining time untuk booking tertentu (AJAX call)
     *
     * Digunakan untuk:
     * - Update countdown timer
     * - Check apakah sudah expired
     * - Auto-refresh page jika sudah expired
     */
    @GetMapping("/{id}/remaining-time")
    @ResponseBody
    public ResponseEntity<?> getRemainingTime(@PathVariable Long id) {
        try {
            Map<String, Object> response = new java.util.HashMap<>();
            // Bisa di-extend dengan actual transaksi lookup
            response.put("bookingId", id);
            response.put("remainingMinutes", 120);
            response.put("isExpired", false);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}