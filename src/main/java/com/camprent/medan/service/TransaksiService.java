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
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

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
    // 1. BOOKING VALIDATION & CREATION
    // ============================================================

    /**
     * ✅ PERBAIKAN UTAMA: Validasi bahwa customer hanya booking dari 1 toko
     *
     * @param username - Customer yang sedang login
     * @return Map berisi:
     *         - "valid": true/false
     *         - "storeId": Long (store yang menjadi tujuan booking)
     *         - "message": String (error message jika ada)
     *         - "itemCount": int (jumlah item yang akan di-booking)
     */
    public Map<String, Object> validateBookingBeforeCheckout(String username) {
        Map<String, Object> result = new HashMap<>();

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

        // 3. Kumpulkan semua store_id dari items di keranjang
        java.util.Set<Long> storeIds = new java.util.HashSet<>();
        for (KeranjangItem item : keranjang) {
            storeIds.add(item.getPeralatan().getStore().getId());
        }

        // 4. Cek apakah ada lebih dari 1 toko
        if (storeIds.size() > 1) {
            result.put("valid", false);
            result.put("message", "Booking gagal! Anda hanya bisa menyewa dari 1 toko yang sama dalam sekali transaksi. Silakan hapus item dari toko lain.");
            result.put("conflictingStores", storeIds.size());
            return result;
        }

        // 5. Jika hanya 1 toko, return storeId
        Long storeId = storeIds.iterator().next();
        result.put("valid", true);
        result.put("storeId", storeId);
        result.put("itemCount", keranjang.size());
        result.put("message", "Validasi satu toko berhasil ✓");

        return result;
    }

    /**
     * ✅ CORE LOGIC: Buat Transaksi dari Cart Item
     *
     * Flow:
     * 1. Validasi satu toko
     * 2. Hitung total harga
     * 3. Create Transaksi dengan status PENDING
     * 4. Set waktuExpire = sekarang + 2 jam
     * 5. Create DetailTransaksi untuk setiap item
     * 6. Jangan ubah stock (hanya berubah saat DIPAKAI)
     * 7. Clear keranjang setelah sukses
     *
     * @param username - Customer yang login
     * @param tanggalSewa - Tanggal mulai sewa
     * @param tanggalKembali - Tanggal pengembalian
     * @return Transaksi yang sudah dibuat
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

        // 5. Create Transaksi baru
        Transaksi transaksi = new Transaksi();
        transaksi.setCustomer(customer);
        transaksi.setStore(store);
        transaksi.setTanggalSewa(tanggalSewa);
        transaksi.setTanggalKembali(tanggalKembali);
        transaksi.setTotalHarga(totalHarga);
        transaksi.setStatusTransaksi("PENDING");
        transaksi.setSource("ONLINE");
        transaksi.setWaktuPemesanan(LocalDateTime.now());

        // ✅ Set waktuExpire = sekarang + 2 jam
        transaksi.setWaktuExpire(LocalDateTime.now().plusMinutes(BOOKING_EXPIRY_MINUTES));

        Transaksi savedTransaksi = transaksiRepository.save(transaksi);

        // 6. Create DetailTransaksi untuk setiap item
        for (KeranjangItem item : keranjang) {
            DetailTransaksi detail = new DetailTransaksi();
            detail.setTransaksi(savedTransaksi);
            detail.setPeralatan(item.getPeralatan());
            detail.setKuantitas(item.getKuantitas());
            detailTransaksiRepository.save(detail);
        }

        // 7. ✅ PENTING: Jangan update stock saat PENDING, hanya saat DIPAKAI!

        // 8. Clear keranjang
        keranjangRepository.deleteByCustomer(customer);

        System.out.println("✅ Booking berhasil dibuat!");
        System.out.println("   ID: " + savedTransaksi.getId());
        System.out.println("   Store: " + store.getNamaToko());
        System.out.println("   Total: Rp" + totalHarga);
        System.out.println("   Expire: " + savedTransaksi.getWaktuExpire());

        return savedTransaksi;
    }

    // ============================================================
    // 2. BOOKING ACTIONS (STORE SIDE)
    // ============================================================

    /**
     * ✅ PERBAIKAN: Saat store terima barang, update status PENDING → DIPAKAI
     * DAN kurangi stock untuk item-item yang disewa
     */
    @Transactional
    public void serahkanBarang(Long transaksiId) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        if (!"PENDING".equals(transaksi.getStatusTransaksi())) {
            throw new IllegalStateException("Hanya transaksi PENDING yang bisa diterima!");
        }

        // ✅ Update stock pada saat ini (DIPAKAI)
        List<DetailTransaksi> details = detailTransaksiRepository.findByTransaksi(transaksi);
        for (DetailTransaksi detail : details) {
            Peralatan alat = detail.getPeralatan();
            alat.setStok(alat.getStok() - detail.getKuantitas());
            peralatanRepository.save(alat);
        }

        // Update status
        transaksi.setStatusTransaksi("DIPAKAI");
        transaksiRepository.save(transaksi);

        System.out.println("✅ Barang diterima & stock berkurang untuk transaksi #" + transaksiId);
    }

    /**
     * ✅ PERBAIKAN: Saat pengembalian, hitung denda jika terlambat
     * DIPAKAI → SELESAI (jika tepat waktu)
     * DIPAKAI → TERLAMBAT (jika lewat + tambah denda)
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

        // Cek apakah terlambat
        if (hariIni.isAfter(batasKembali)) {
            // Hitung selisih hari keterlambatan
            long hariTerlambat = ChronoUnit.DAYS.between(batasKembali, hariIni);

            // Ambil semua detail item untuk menghitung denda
            List<DetailTransaksi> details = detailTransaksiRepository.findByTransaksi(transaksi);
            BigDecimal totalDendaAkumulasi = BigDecimal.ZERO;

            for (DetailTransaksi detail : details) {
                // Rumus: Denda per alat x Kuantitas x Jumlah Hari Terlambat
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
            System.out.println("✅ Pengembalian tepat waktu, transaksi SELESAI");
        }

        transaksiRepository.save(transaksi);
    }

    /**
     * ✅ BARU: Store bisa REJECT booking jika ada masalah
     * PENDING → CANCELLED
     * Stock dikembalikan jika sudah berubah
     */
    @Transactional
    public void rejectBooking(Long transaksiId, String alasan) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        if (!"PENDING".equals(transaksi.getStatusTransaksi())) {
            throw new IllegalStateException("Hanya booking PENDING yang bisa ditolak!");
        }

        transaksi.setStatusTransaksi("CANCELLED");
        transaksiRepository.save(transaksi);

        System.out.println("❌ Booking ditolak: " + alasan);
    }

    // ============================================================
    // 3. AUTO-EXPIRE MECHANISM (Scheduled Task)
    // ============================================================

    /**
     * ✅ SCHEDULED TASK: Jalankan setiap 5 menit untuk check booking expired
     *
     * Logic:
     * - Find all PENDING bookings dengan waktuExpire < sekarang
     * - Change status PENDING → EXPIRED
     * - Log untuk audit
     */
    @Transactional
    @Scheduled(fixedRate = 300000) // Jalankan setiap 5 menit (300 detik)
    public void checkAndExpireBookings() {
        LocalDateTime now = LocalDateTime.now();

        // Find all PENDING bookings yang sudah expire
        List<Transaksi> expiredBookings = transaksiRepository.findAll().stream()
                .filter(t -> "PENDING".equals(t.getStatusTransaksi()))
                .filter(t -> t.getWaktuExpire() != null && now.isAfter(t.getWaktuExpire()))
                .toList();

        if (!expiredBookings.isEmpty()) {
            System.out.println("⏰ [AUTO-EXPIRE CHECK] Menemukan " + expiredBookings.size() + " booking yang expired");

            for (Transaksi transaksi : expiredBookings) {
                transaksi.setStatusTransaksi("EXPIRED");
                transaksiRepository.save(transaksi);

                System.out.println("❌ EXPIRED - Booking #" + transaksi.getId()
                        + " dari " + transaksi.getStore().getNamaToko()
                        + " (Expired at: " + transaksi.getWaktuExpire() + ")");
            }
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
     * Get semua transaksi untuk admin
     */
    public List<Transaksi> getAllTransaksi() {
        return transaksiRepository.findAll();
    }

    /**
     * Get detail items dari satu transaksi
     */
    public List<DetailTransaksi> getDetailTransaksi(Long transaksiId) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId).orElse(null);
        if (transaksi == null) return java.util.Collections.emptyList();

        return detailTransaksiRepository.findByTransaksi(transaksi);
    }
}