package com.camprent.medan.service;

import com.camprent.medan.entity.*;
import com.camprent.medan.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
public class TransaksiService {

    @Autowired
    private TransaksiRepository transaksiRepository;

    @Autowired
    private DetailTransaksiRepository detailTransaksiRepository;

    @Autowired
    private KeranjangRepository keranjangRepository;

    @Autowired
    private PeralatanRepository peralatanRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // ✅ CONFIG: Booking expiry time in minutes
    private static final long BOOKING_EXPIRY_MINUTES = 120; // 2 jam

    // ============================================================
    // 0. HELPER METHODS
    // ============================================================

    /**
     * ✅ BARU: Get transaksi by ID (untuk detail modal & validation)
     */
    public Optional<Transaksi> getTransaksiById(Long id) {
        return transaksiRepository.findById(id);
    }

    // ============================================================
    // 1. BOOKING VALIDATION & CREATION
    // ============================================================

    /**
     * ✅ Validasi: Customer hanya booking dari 1 toko
     *
     * @param username - Customer yang sedang login
     * @return Map berisi status validasi
     */
    public Map<String, Object> validateBookingBeforeCheckout(String username) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Ambil customer
            Customer customer = customerRepository.findByUserUsername(username)
                    .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));

            // 2. Ambil semua item di keranjang
            List<KeranjangItem> keranjang = keranjangRepository.findByCustomer(customer);

            if (keranjang.isEmpty()) {
                result.put("valid", false);
                result.put("message", "Keranjang Anda kosong!");
                return result;
            }

            // 3. Kumpulkan semua store_id dari items
            java.util.Set<Long> storeIds = new java.util.HashSet<>();
            for (KeranjangItem item : keranjang) {
                storeIds.add(item.getPeralatan().getStore().getId());
            }

            // 4. Validasi: hanya 1 toko?
            if (storeIds.size() > 1) {
                result.put("valid", false);
                result.put("message",
                        "❌ Booking gagal! Anda hanya bisa menyewa dari 1 toko yang sama dalam sekali transaksi. " +
                                "Silakan hapus item dari toko lain.");
                result.put("conflictingStores", storeIds.size());
                return result;
            }

            // 5. Valid - return store ID
            Long storeId = storeIds.iterator().next();
            result.put("valid", true);
            result.put("storeId", storeId);
            result.put("itemCount", keranjang.size());
            result.put("message", "✅ Validasi satu toko berhasil!");

            return result;

        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * ✅ CORE LOGIC: Buat Transaksi dari Cart
     *
     * ✅ FLOW YANG BENAR:
     * 1. Validasi satu toko
     * 2. Hitung total harga
     * 3. Create Transaksi status PENDING
     * 4. Set waktuExpire = now + 2 jam
     * 5. ⚡ KURANGI STOCK LANGSUNG (PERBEDAAN DARI SEBELUMNYA!)
     * 6. Create DetailTransaksi
     * 7. Clear keranjang
     */
    @Transactional
    public Transaksi createBookingFromCart(String username, LocalDate tanggalSewa, LocalDate tanggalKembali) {
        // 1. Validasi customer & keranjang
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer tidak ditemukan"));

        List<KeranjangItem> keranjang = keranjangRepository.findByCustomer(customer);

        if (keranjang.isEmpty()) {
            throw new RuntimeException("Keranjang Anda kosong!");
        }

        // 2. Validasi satu toko
        Map<String, Object> validationResult = validateBookingBeforeCheckout(username);
        if (!(Boolean) validationResult.get("valid")) {
            throw new RuntimeException((String) validationResult.get("message"));
        }

        Long storeId = (Long) validationResult.get("storeId");
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Toko tidak ditemukan"));

        // 3. Validasi tanggal
        if (tanggalSewa.isAfter(tanggalKembali) || tanggalSewa.equals(tanggalKembali)) {
            throw new RuntimeException("Tanggal sewa harus sebelum tanggal kembali!");
        }

        // 4. Hitung total harga
        BigDecimal totalHarga = BigDecimal.ZERO;
        long hariSewa = ChronoUnit.DAYS.between(tanggalSewa, tanggalKembali);

        for (KeranjangItem item : keranjang) {
            BigDecimal subtotal = item.getPeralatan().getHargaSewaPerHari()
                    .multiply(BigDecimal.valueOf(item.getKuantitas()))
                    .multiply(BigDecimal.valueOf(hariSewa));
            totalHarga = totalHarga.add(subtotal);
        }

        // 5. Create Transaksi baru dengan status PENDING
        Transaksi transaksi = new Transaksi();
        transaksi.setCustomer(customer);
        transaksi.setStore(store);
        transaksi.setTanggalSewa(tanggalSewa);
        transaksi.setTanggalKembali(tanggalKembali);
        transaksi.setTotalHarga(totalHarga);
        transaksi.setStatusTransaksi("PENDING"); // ✅ Status PENDING (bukan DIPAKAI)
        transaksi.setSource("ONLINE");
        transaksi.setWaktuPemesanan(LocalDateTime.now());

        // ✅ Set waktuExpire = sekarang + 2 jam
        transaksi.setWaktuExpire(LocalDateTime.now().plusMinutes(BOOKING_EXPIRY_MINUTES));

        Transaksi savedTransaksi = transaksiRepository.save(transaksi);

        // 6. ⚡ KURANGI STOCK LANGSUNG (PERBEDAAN DARI SEBELUMNYA)
        System.out.println("⚡ [STEP 6] Kurangi stok barang yang dibooking...");
        for (KeranjangItem item : keranjang) {
            Peralatan alat = item.getPeralatan();
            int stokSebelum = alat.getStok();

            // Validasi stok sebelum kurangi
            if (alat.getStok() < item.getKuantitas()) {
                throw new RuntimeException("Stok untuk '" + alat.getNamaAlat() + "' tidak mencukupi!");
            }

            // KURANGI STOCK SEKARANG
            alat.setStok(alat.getStok() - item.getKuantitas());
            peralatanRepository.save(alat);

            System.out.println("   ✅ " + alat.getNamaAlat() +
                    ": " + stokSebelum + " → " + alat.getStok());
        }

        // 7. Create DetailTransaksi untuk setiap item
        for (KeranjangItem item : keranjang) {
            DetailTransaksi detail = new DetailTransaksi();
            detail.setTransaksi(savedTransaksi);
            detail.setPeralatan(item.getPeralatan());
            detail.setKuantitas(item.getKuantitas());
            detailTransaksiRepository.save(detail);
        }

        // 8. Clear keranjang
        keranjangRepository.deleteByCustomer(customer);

        // Log
        System.out.println("\n✅ ═════════════════════════════════════════");
        System.out.println("✅ BOOKING BERHASIL DIBUAT!");
        System.out.println("✅ ═════════════════════════════════════════");
        System.out.println("   Booking ID: #" + savedTransaksi.getId());
        System.out.println("   Toko: " + store.getNamaToko());
        System.out.println("   Total: Rp" + totalHarga);
        System.out.println("   Status: PENDING (Menunggu customer ambil dalam 2 jam)");
        System.out.println("   Timer Expire: " + savedTransaksi.getWaktuExpire());
        System.out.println("✅ ═════════════════════════════════════════\n");

        return savedTransaksi;
    }

    // ============================================================
    // 2. BOOKING ACTIONS (STORE SIDE)
    // ============================================================

    /**
     * ✅ REFACTORED: Store terima barang (customer ambil)
     *
     * BERBEDA DARI SEBELUMNYA:
     * - LAMA: PENDING → DIPAKAI + KURANGI STOCK
     * - BARU: PENDING → DIPAKAI (JANGAN KURANGI STOCK, SUDAH BERKURANG!)
     */
    @Transactional
    public void acceptPickup(Long transaksiId) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        if (!"PENDING".equals(transaksi.getStatusTransaksi())) {
            throw new IllegalStateException("Hanya transaksi PENDING yang bisa diterima!");
        }

        // ⚠️ PENTING: JANGAN KURANGI STOCK DI SINI!
        // Stock SUDAH berkurang saat customer booking dibuat!
        // Di sini hanya confirm bahwa customer sudah ambil barangnya

        // HANYA update status
        transaksi.setStatusTransaksi("DIPAKAI");
        transaksiRepository.save(transaksi);

        System.out.println("✅ Barang diterima! Status: PENDING → DIPAKAI");
        System.out.println("   Stock TETAP berkurang (sudah berkurang saat booking)");
    }

    /**
     * ✅ BARU: Store REJECT/CANCEL booking
     *
     * Action:
     * - Status: PENDING → CANCELLED
     * - ⚠️ KEMBALIKAN STOK yang sudah berkurang saat booking
     */
    @Transactional
    public void rejectBooking(Long transaksiId, String alasan) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        if (!"PENDING".equals(transaksi.getStatusTransaksi())) {
            throw new IllegalStateException("Hanya booking PENDING yang bisa ditolak!");
        }

        // ⚠️ KEMBALIKAN STOK yang sudah berkurang
        System.out.println("🔄 Kembalikan stok yang sudah berkurang...");
        List<DetailTransaksi> details = detailTransaksiRepository.findByTransaksi(transaksi);
        for (DetailTransaksi detail : details) {
            Peralatan alat = detail.getPeralatan();
            int stokSebelum = alat.getStok();

            // KEMBALIKAN STOCK
            alat.setStok(alat.getStok() + detail.getKuantitas());
            peralatanRepository.save(alat);

            System.out.println("   ✅ " + alat.getNamaAlat() +
                    ": " + stokSebelum + " → " + alat.getStok());
        }

        // Update status
        transaksi.setStatusTransaksi("CANCELLED");
        transaksiRepository.save(transaksi);

        System.out.println("✅ Booking dibatalkan/ditolak");
        System.out.println("   Alasan: " + alasan);
    }

    /**
     * ✅ Saat pengembalian, hitung denda jika terlambat
     * DIPAKAI → SELESAI (tepat waktu)
     * DIPAKAI → TERLAMBAT (lewat + hitung denda)
     */
    @Transactional
    public void kembalikanBarang(Long transaksiId) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        if (!"DIPAKAI".equals(transaksi.getStatusTransaksi())) {
            throw new IllegalStateException("Transaksi harus dalam status DIPAKAI!");
        }

        LocalDate hariIni = LocalDate.now();
        LocalDate batasKembali = transaksi.getTanggalKembali();

        // ⚠️ KEMBALIKAN STOK
        System.out.println("🔄 Kembalikan stok barang...");
        List<DetailTransaksi> details = detailTransaksiRepository.findByTransaksi(transaksi);
        for (DetailTransaksi detail : details) {
            Peralatan alat = detail.getPeralatan();
            int stokSebelum = alat.getStok();

            alat.setStok(alat.getStok() + detail.getKuantitas());
            peralatanRepository.save(alat);

            System.out.println("   ✅ " + alat.getNamaAlat() +
                    ": " + stokSebelum + " → " + alat.getStok());
        }

        // Cek apakah terlambat
        if (hariIni.isAfter(batasKembali)) {
            // Hitung selisih hari keterlambatan
            long hariTerlambat = ChronoUnit.DAYS.between(batasKembali, hariIni);

            // Hitung denda
            BigDecimal totalDendaAkumulasi = BigDecimal.ZERO;

            for (DetailTransaksi detail : details) {
                // Rumus: Denda per alat × Kuantitas × Jumlah Hari Terlambat
                BigDecimal dendaPerItem = detail.getPeralatan().getDendaKerusakan()
                        .multiply(BigDecimal.valueOf(detail.getKuantitas()))
                        .multiply(BigDecimal.valueOf(hariTerlambat));

                totalDendaAkumulasi = totalDendaAkumulasi.add(dendaPerItem);
            }

            // Tambahkan denda ke total harga
            transaksi.setTotalHarga(transaksi.getTotalHarga().add(totalDendaAkumulasi));
            transaksi.setStatusTransaksi("TERLAMBAT");

            System.out.println("⚠️ Pengembalian TERLAMBAT!");
            System.out.println("   Hari terlambat: " + hariTerlambat);
            System.out.println("   Denda tambahan: Rp" + totalDendaAkumulasi);
        } else {
            transaksi.setStatusTransaksi("SELESAI");
            System.out.println("✅ Pengembalian tepat waktu! Status: DIPAKAI → SELESAI");
        }

        transaksiRepository.save(transaksi);
    }

    // ============================================================
    // 3. AUTO-EXPIRE MECHANISM (Scheduled Task)
    // ============================================================

    /**
     * ✅ SCHEDULED TASK: Auto-expire booking yang sudah 2 jam
     *
     * Jalankan setiap 5 menit untuk:
     * - Find PENDING bookings dengan waktuExpire < sekarang
     * - Change status: PENDING → EXPIRED
     * - ⚠️ KEMBALIKAN STOK yang sudah berkurang
     */
    @Transactional
    @Scheduled(fixedRate = 300000) // 5 menit = 300000 ms
    public void checkAndExpireBookings() {
        LocalDateTime now = LocalDateTime.now();

        // Find all PENDING bookings yang sudah expire
        List<Transaksi> expiredBookings = transaksiRepository.findAll().stream()
                .filter(t -> "PENDING".equals(t.getStatusTransaksi()))
                .filter(t -> t.getWaktuExpire() != null && now.isAfter(t.getWaktuExpire()))
                .toList();

        if (!expiredBookings.isEmpty()) {
            System.out.println("\n⏰ ═══════════════════════════════════════════");
            System.out.println("⏰ [AUTO-EXPIRE CHECK] Menemukan " + expiredBookings.size() + " booking yang EXPIRED");
            System.out.println("⏰ ═══════════════════════════════════════════");

            for (Transaksi transaksi : expiredBookings) {
                System.out.println("\n🔄 Processing expired booking #" + transaksi.getId());

                // ⚠️ KEMBALIKAN STOK
                List<DetailTransaksi> details = detailTransaksiRepository.findByTransaksi(transaksi);
                for (DetailTransaksi detail : details) {
                    Peralatan alat = detail.getPeralatan();
                    int stokSebelum = alat.getStok();

                    alat.setStok(alat.getStok() + detail.getKuantitas());
                    peralatanRepository.save(alat);

                    System.out.println("   ✅ " + alat.getNamaAlat() +
                            ": " + stokSebelum + " → " + alat.getStok());
                }

                // Update status
                transaksi.setStatusTransaksi("EXPIRED");
                transaksiRepository.save(transaksi);

                System.out.println("   ❌ Status: PENDING → EXPIRED");
                System.out.println("   Alasan: Customer tidak ambil dalam 2 jam");
                System.out.println("   Expired at: " + transaksi.getWaktuExpire());
            }

            System.out.println("⏰ ═══════════════════════════════════════════\n");
        }
    }

    // ============================================================
    // 4. QUERY HELPER METHODS
    // ============================================================

    /**
     * Get transaksi untuk customer tertentu
     */
    public List<Transaksi> getTransaksiByCustomer(String username) {
        Customer customer = customerRepository.findByUserUsername(username).orElse(null);
        if (customer == null) return java.util.Collections.emptyList();

        return transaksiRepository.findAll().stream()
                .filter(t -> customer.getId().equals(t.getCustomer() != null ? t.getCustomer().getId() : null))
                .toList();
    }

    /**
     * Get transaksi untuk store tertentu
     */
    public List<Transaksi> getTransaksiByStore(String username) {
        Store store = storeRepository.findByUserUsername(username).orElse(null);
        if (store == null) return java.util.Collections.emptyList();

        return transaksiRepository.findByStoreOrderByIdDesc(store);
    }

    /**
     * Get semua transaksi (admin)
     */
    public List<Transaksi> getAllTransaksi() {
        return transaksiRepository.findAll();
    }

    /**
     * Get detail items dari transaksi
     */
    public List<DetailTransaksi> getDetailTransaksi(Long transaksiId) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId).orElse(null);
        if (transaksi == null) return java.util.Collections.emptyList();

        return detailTransaksiRepository.findByTransaksi(transaksi);
    }
}