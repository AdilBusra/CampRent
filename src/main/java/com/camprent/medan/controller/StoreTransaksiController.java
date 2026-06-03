package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.repository.PeralatanRepository;
import com.camprent.medan.repository.StoreRepository;
import com.camprent.medan.service.PeralatanService;
import com.camprent.medan.service.StoreTransaksiService;
import com.camprent.medan.service.TransaksiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/store")
public class StoreTransaksiController {

    @Autowired
    private StoreTransaksiService storeTransaksiService;

    @Autowired
    private TransaksiService transaksiService;

    @Autowired
    private PeralatanService peralatanService;

    @Autowired
    private PeralatanRepository peralatanRepository;

    @Autowired
    private StoreRepository storeRepository;

    // ✅ PERBAIKAN UTAMA: Menampilkan list transaksi + list peralatan toko yang login
    @GetMapping("/transaksi")
    public String listTransaksiStore(Model model, Authentication auth) {
        System.out.println("🔍 === START DEBUG ===");

        // 1. Check username
        String username = auth.getName();
        System.out.println("1️⃣ Username: " + username);

        // 2. Check Store
        var storeOptional = storeRepository.findByUserUsername(username);
        System.out.println("2️⃣ Store ditemukan? " + storeOptional.isPresent());

        if (storeOptional.isEmpty()) {
            System.out.println("❌ STORE TIDAK DITEMUKAN!");
            System.out.println("   Cek database: SELECT * FROM stores WHERE user_id = ?");
            System.out.println("   Cek users: SELECT * FROM users WHERE username = '" + username + "'");
        }

        Store store = storeOptional
                .orElseThrow(() -> new RuntimeException("Toko tidak ditemukan"));

        System.out.println("STORE ID = " + store.getId());
        System.out.println("STORE NAME = " + store.getNamaToko());
        System.out.println("3️⃣ Store Name: " + store.getNamaToko());
        System.out.println("4️⃣ Store ID: " + store.getId());

        // 3. Check Peralatan
        List<Peralatan> alatList = peralatanRepository.findByStore(store);

        System.out.println(alatList);
        System.out.println("5️⃣ Total Peralatan: " + alatList.size());

        if (alatList.isEmpty()) {
            System.out.println("❌ PERALATAN KOSONG!");
            System.out.println("   Cek database: SELECT * FROM peralatans WHERE store_id = " + store.getId());
        } else {
            alatList.forEach(alat -> {
                System.out.println("   ✅ " + alat.getNamaAlat() +
                        " (Harga: " + alat.getHargaSewaPerHari() +
                        ", Stok: " + alat.getStok() + ")");
            });
        }

        // 4. Check Transaksi
        List<Transaksi> transaksiList = storeTransaksiService.getTransaksiByStore(username);
        System.out.println("6️⃣ Total Transaksi: " + transaksiList.size());

        System.out.println("DEBUG LIST ALAT = " + alatList);
        System.out.println("DEBUG LIST TRANSAKSI = " + transaksiList);

// Set model
        model.addAttribute("listAlat", alatList);
        model.addAttribute("listTransaksi", transaksiList);
        model.addAttribute("store", store);

        System.out.println("MODEL listAlat = " + model.getAttribute("listAlat"));
        System.out.println("MODEL listTransaksi = " + model.getAttribute("listTransaksi"));

        System.out.println("🔍 === END DEBUG ===");
        System.out.println("");

        System.out.println("======================");
        System.out.println("LIST ALAT SIZE = " + alatList.size());

        for (Peralatan p : alatList) {
            System.out.println(
                    p.getId() + " | " +
                            p.getNamaAlat() + " | " +
                            p.getStok()
            );
        }
        System.out.println("======================");

        return "store/transaction-list";
    }

    // ============================================================
    // 🎯 STEP 2A: PICK - Customer ambil barang (PENDING → DIPAKAI)
    // ============================================================

    /**
     * ✅ POST: Store TERIMA/PICK booking
     *
     * Endpoint: POST /store/booking/{id}/pick
     *
     * Action:
     * - Status: PENDING → DIPAKAI
     * - ⚠️ Stock JANGAN diubah (sudah berkurang saat booking)
     * - Ini hanya confirm bahwa customer sudah ambil barang
     * - Timer berhenti
     * - Tombol PICK & CANCEL hilang, tombol RETURN muncul
     */
    @PostMapping("/booking/{id}/pick")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pickBooking(
            @PathVariable Long id,
            Authentication auth) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Validasi: user adalah store owner dari transaksi ini
            Transaksi transaksi = transaksiService.getTransaksiById(id)
                    .orElseThrow(() -> new RuntimeException("Booking tidak ditemukan"));

            if (!transaksi.getStore().getUser().getUsername().equals(auth.getName())) {
                response.put("success", false);
                response.put("message", "❌ Unauthorized! Anda bukan owner dari booking ini");
                return ResponseEntity.status(403).body(response);
            }

            // 2. Validasi: status harus PENDING
            if (!"PENDING".equals(transaksi.getStatusTransaksi())) {
                response.put("success", false);
                response.put("message", "❌ Hanya booking dengan status PENDING yang bisa di-PICK!");
                response.put("currentStatus", transaksi.getStatusTransaksi());
                return ResponseEntity.badRequest().body(response);
            }

            // 3. Validasi: booking belum expire?
            if (transaksi.isExpired()) {
                response.put("success", false);
                response.put("message", "❌ Booking sudah expired! Tidak bisa di-PICK lagi");
                return ResponseEntity.badRequest().body(response);
            }

            // 4. Execute: Update status PENDING → DIPAKAI
            transaksiService.acceptPickup(id);

            // 5. Return response sukses
            response.put("success", true);
            response.put("message", "✅ Barang diterima! Status: PENDING → DIPAKAI");
            response.put("newStatus", "DIPAKAI");
            response.put("bookingId", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============================================================
    // 🎯 STEP 2B: CANCEL - Store tolak booking (PENDING → CANCELLED)
    // ============================================================

    /**
     * ✅ POST: Store TOLAK/CANCEL booking
     *
     * Endpoint: POST /store/booking/{id}/cancel
     *
     * Action:
     * - Status: PENDING → CANCELLED
     * - ⚠️ KEMBALIKAN STOK yang sudah berkurang
     * - Modal close otomatis
     * - Transaksi tidak ada di pending list lagi
     */
    @PostMapping("/booking/{id}/cancel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason,
            Authentication auth) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Validasi: user adalah store owner dari transaksi ini
            Transaksi transaksi = transaksiService.getTransaksiById(id)
                    .orElseThrow(() -> new RuntimeException("Booking tidak ditemukan"));

            if (!transaksi.getStore().getUser().getUsername().equals(auth.getName())) {
                response.put("success", false);
                response.put("message", "❌ Unauthorized! Anda bukan owner dari booking ini");
                return ResponseEntity.status(403).body(response);
            }

            // 2. Validasi: status harus PENDING
            if (!"PENDING".equals(transaksi.getStatusTransaksi())) {
                response.put("success", false);
                response.put("message", "❌ Hanya booking dengan status PENDING yang bisa di-CANCEL!");
                response.put("currentStatus", transaksi.getStatusTransaksi());
                return ResponseEntity.badRequest().body(response);
            }

            // 3. Execute: Reject booking (status PENDING → CANCELLED, stok kembali)
            String alasanDefault = reason != null && !reason.trim().isEmpty() ? reason : "Ditolak oleh store";
            transaksiService.rejectBooking(id, alasanDefault);

            // 4. Return response sukses
            response.put("success", true);
            response.put("message", "✅ Booking dibatalkan. Stok barang dikembalikan.");
            response.put("newStatus", "CANCELLED");
            response.put("bookingId", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============================================================
    // 🎯 STEP 3: RETURN - Customer return barang (DIPAKAI → SELESAI/TERLAMBAT)
    // ============================================================

    /**
     * ✅ POST: Store CONFIRM RETURN barang
     *
     * Endpoint: POST /store/booking/{id}/return
     *
     * Action:
     * - Status: DIPAKAI → SELESAI (jika tepat waktu)
     * - Status: DIPAKAI → TERLAMBAT (jika terlambat + hitung denda)
     * - ⚠️ KEMBALIKAN STOK barang ke inventory
     * - Transaksi masuk history (tidak di pending list lagi)
     */
    @PostMapping("/booking/{id}/return")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> returnBooking(
            @PathVariable Long id,
            Authentication auth) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Validasi: user adalah store owner dari transaksi ini
            Transaksi transaksi = transaksiService.getTransaksiById(id)
                    .orElseThrow(() -> new RuntimeException("Booking tidak ditemukan"));

            if (!transaksi.getStore().getUser().getUsername().equals(auth.getName())) {
                response.put("success", false);
                response.put("message", "❌ Unauthorized! Anda bukan owner dari booking ini");
                return ResponseEntity.status(403).body(response);
            }

            // 2. Validasi: status harus DIPAKAI
            if (!"DIPAKAI".equals(transaksi.getStatusTransaksi())) {
                response.put("success", false);
                response.put("message", "❌ Hanya booking dengan status DIPAKAI yang bisa di-RETURN!");
                response.put("currentStatus", transaksi.getStatusTransaksi());
                return ResponseEntity.badRequest().body(response);
            }

            // 3. Execute: Process return (status DIPAKAI → SELESAI/TERLAMBAT, stok kembali, denda hitung)
            transaksiService.kembalikanBarang(id);

            // 4. Get updated transaksi untuk return status & denda info
            Transaksi updatedTransaksi = transaksiService.getTransaksiById(id)
                    .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan setelah update"));

            // 5. Return response sukses
            response.put("success", true);
            response.put("message", "✅ Barang berhasil dikembalikan!");
            response.put("newStatus", updatedTransaksi.getStatusTransaksi());
            response.put("bookingId", id);
            response.put("totalHarga", updatedTransaksi.getTotalHarga());

            if ("TERLAMBAT".equals(updatedTransaksi.getStatusTransaksi())) {
                response.put("warning", "⚠️ Pengembalian TERLAMBAT! Denda sudah ditambahkan.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============================================================
    // 📋 BOOKING DETAIL (untuk modal)
    // ============================================================

    /**
     * ✅ GET: Get detail booking (AJAX untuk populate modal)
     *
     * Endpoint: GET /store/booking/{id}/detail
     * Response: JSON dengan detail lengkap
     */
    @GetMapping("/booking/{id}/detail")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getBookingDetail(
            @PathVariable Long id,
            Authentication auth) {

        Map<String, Object> response = new HashMap<>();

        try {
            Transaksi transaksi = transaksiService.getTransaksiById(id)
                    .orElseThrow(() -> new RuntimeException("Booking tidak ditemukan"));

            // Security check
            if (!transaksi.getStore().getUser().getUsername().equals(auth.getName())) {
                response.put("error", "Unauthorized");
                return ResponseEntity.status(403).body(response);
            }

            // Populate response
            response.put("id", transaksi.getId());
            response.put("source", transaksi.getSource());
            response.put("status", transaksi.getStatusTransaksi());
            response.put("customerName", transaksi.getSource().equals("ONLINE") ?
                    transaksi.getCustomer().getNamaLengkap() : transaksi.getNamaCustomer());
            response.put("customerPhone", transaksi.getSource().equals("ONLINE") ?
                    transaksi.getCustomer().getNomorTelepon() : transaksi.getNoHpCustomer());
            response.put("tanggalSewa", transaksi.getTanggalSewa());
            response.put("tanggalKembali", transaksi.getTanggalKembali());
            response.put("totalHarga", transaksi.getTotalHarga());
            response.put("remainingMinutes", transaksi.getRemainingMinutes());
            response.put("isExpired", transaksi.isExpired());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============================================================
    // ✅ OFFLINE RENTAL CREATION
    // ============================================================

    /**
     * ✅ PERBAIKAN: Simpan offline rental dengan stock tracking
     * ✅ PERBAIKAN: Mapping nama parameter dari HTML agar pas dengan Controller
     *
     * Endpoint: POST /store/transaksi/save-offline
     */
    @PostMapping("/transaksi/save-offline")
    public String simpanOfflineRental(@RequestParam("customerName") String customerName,
                                      @RequestParam("phoneNumber") String phoneNumber,
                                      @RequestParam("tanggalSewa") String tanggalSewa,
                                      @RequestParam("tanggalKembali") String tanggalKembali,
                                      @RequestParam(value = "peralatanIds", required = false) List<Long> peralatanIds,
                                      @RequestParam(value = "kuantitas", required = false) List<Integer> kuantitas,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {

        // Validasi pencegahan jika toko belum memilih alat camp sama sekali
        if (peralatanIds == null || peralatanIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Pilih minimal satu alat untuk rental offline!");
            return "redirect:/store/transaksi?error=no_items";
        }

        try {
            // Panggil service untuk menyimpan data transaksi offline
            storeTransaksiService.createOfflineRental(principal.getName(), customerName,
                    phoneNumber, tanggalSewa, tanggalKembali, peralatanIds, kuantitas);

            redirectAttributes.addFlashAttribute("successMessage", "✅ Rental offline berhasil disimpan!");
            return "redirect:/store/transaksi";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Error: " + e.getMessage());
            return "redirect:/store/transaksi";
        }
    }
}