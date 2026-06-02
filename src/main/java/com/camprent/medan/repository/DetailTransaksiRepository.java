package com.camprent.medan.repository;

import com.camprent.medan.entity.DetailTransaksi;
import com.camprent.medan.entity.Transaksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetailTransaksiRepository extends JpaRepository<DetailTransaksi, Long> {

    // Ambil semua detail item dari satu transaksi
    List<DetailTransaksi> findByTransaksi(Transaksi transaksi);

    // Hapus semua detail item ketika transaksi dibatalkan
    void deleteByTransaksi(Transaksi transaksi);
}
