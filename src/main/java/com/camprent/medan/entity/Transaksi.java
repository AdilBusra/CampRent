package com.camprent.medan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaksis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaksi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = true)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "tanggal_sewa", nullable = false)
    private LocalDate tanggalSewa;

    @Column(name = "tanggal_kembali", nullable = false)
    private LocalDate tanggalKembali;

    @Column(name = "total_harga", nullable = false)
    private BigDecimal totalHarga;

    @Column(name = "status_transaksi", nullable = false)
    private String statusTransaksi = "PENDING"; // PENDING, DIPAKAI, SELESAI, TERLAMBAT, EXPIRED, CANCELLED

    // Tambahkan kolom untuk Offline
    @Column(name = "nama_customer")
    private String namaCustomer;

    @Column(name = "no_hp_customer")
    private String noHpCustomer;

    @Column(name = "source")
    private String source; // "ONLINE" atau "OFFLINE"

    // ✅ FIELDS BARU: Untuk tracking booking lifetime
    @Column(name = "waktu_pemesanan", nullable = false, updatable = false)
    private LocalDateTime waktuPemesanan = LocalDateTime.now();

    @Column(name = "waktu_expire")
    private LocalDateTime waktuExpire;

    // ✅ Helper method: Calculate remaining minutes for booking
    public long getRemainingMinutes() {
        if (waktuExpire == null) {
            return 0;
        }
        long minutesLeft = java.time.temporal.ChronoUnit.MINUTES.between(
                LocalDateTime.now(),
                waktuExpire
        );
        return Math.max(0, minutesLeft);
    }

    // ✅ Helper method: Check if booking has expired
    public boolean isExpired() {
        if (waktuExpire == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(waktuExpire);
    }

    // ✅ Helper method: Check if still pending
    public boolean isPending() {
        return "PENDING".equals(statusTransaksi);
    }
}