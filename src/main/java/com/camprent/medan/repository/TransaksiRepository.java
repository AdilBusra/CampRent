package com.camprent.medan.repository;

import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaksiRepository extends JpaRepository<Transaksi, Long> {

    // ✅ Original methods (jangan dihapus)
    List<Transaksi> findByStoreOrderByIdDesc(Store store);

    // ============================================================
    // ✅ CUSTOM QUERIES UNTUK BOOKING FEATURE
    // ============================================================

    /**
     * Find all PENDING bookings yang sudah expire (waktuExpire < sekarang)
     * Digunakan untuk scheduled task auto-expire
     */
    @Query("SELECT t FROM Transaksi t WHERE t.statusTransaksi = 'PENDING' AND t.waktuExpire IS NOT NULL AND t.waktuExpire < :now")
    List<Transaksi> findExpiredPendingBookings(@Param("now") LocalDateTime now);

    /**
     * Find all PENDING bookings untuk store tertentu
     * Digunakan untuk menampilkan pending orders di store dashboard
     */
    @Query("SELECT t FROM Transaksi t WHERE t.store.id = :storeId AND t.statusTransaksi = 'PENDING' ORDER BY t.waktuPemesanan DESC")
    List<Transaksi> findPendingBookingsByStore(@Param("storeId") Long storeId);

    /**
     * Find all DIPAKAI (in-progress) bookings untuk store tertentu
     */
    @Query("SELECT t FROM Transaksi t WHERE t.store.id = :storeId AND t.statusTransaksi = 'DIPAKAI' ORDER BY t.tanggalKembali ASC")
    List<Transaksi> findActiveRentalsByStore(@Param("storeId") Long storeId);

    /**
     * Find all TERLAMBAT bookings untuk customer tertentu
     * Untuk remind customer tentang keterlambatan
     */
    @Query("SELECT t FROM Transaksi t WHERE t.customer.id = :customerId AND t.statusTransaksi = 'TERLAMBAT' ORDER BY t.tanggalKembali DESC")
    List<Transaksi> findLateBookingsByCustomer(@Param("customerId") Long customerId);

    /**
     * Find all PENDING & DIPAKAI bookings untuk customer tertentu
     * Untuk halaman "My Booking" - menampilkan booking yang masih active
     */
    @Query("SELECT t FROM Transaksi t WHERE t.customer.id = :customerId AND (t.statusTransaksi = 'PENDING' OR t.statusTransaksi = 'DIPAKAI') ORDER BY t.waktuPemesanan DESC")
    List<Transaksi> findActiveBookingsByCustomer(@Param("customerId") Long customerId);

    /**
     * Find all ONLINE bookings (dari customer via web)
     * Digunakan untuk admin melihat online bookings vs offline
     */
    @Query("SELECT t FROM Transaksi t WHERE t.source = 'ONLINE' ORDER BY t.waktuPemesanan DESC")
    List<Transaksi> findAllOnlineBookings();

    /**
     * Find all OFFLINE bookings (walk-in customer)
     */
    @Query("SELECT t FROM Transaksi t WHERE t.source = 'OFFLINE' ORDER BY t.waktuPemesanan DESC")
    List<Transaksi> findAllOfflineBookings();

    /**
     * Count total bookings untuk customer
     * Digunakan di customer dashboard/profile
     */
    @Query("SELECT COUNT(t) FROM Transaksi t WHERE t.customer.id = :customerId")
    long countByCustomer(@Param("customerId") Long customerId);

    /**
     * Count PENDING bookings untuk store
     * Digunakan untuk badge/notification di store navbar
     */
    @Query("SELECT COUNT(t) FROM Transaksi t WHERE t.store.id = :storeId AND t.statusTransaksi = 'PENDING'")
    long countPendingByStore(@Param("storeId") Long storeId);

    /**
     * Get total revenue untuk store
     * Query untuk dashboard analytics
     */
    @Query("SELECT COALESCE(SUM(t.totalHarga), 0) FROM Transaksi t WHERE t.store.id = :storeId AND (t.statusTransaksi = 'SELESAI' OR t.statusTransaksi = 'TERLAMBAT')")
    java.math.BigDecimal getTotalRevenueByStore(@Param("storeId") Long storeId);
}