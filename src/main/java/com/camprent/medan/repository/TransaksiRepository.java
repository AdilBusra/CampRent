package com.camprent.medan.repository;

import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransaksiRepository extends JpaRepository<Transaksi, Long> {
    // Cukup gunakan findAll() bawaan JpaRepository untuk mengambil semua data

    // KODE BARU KAMU MASUK DI SINI:
    List<Transaksi> findByStoreOrderByIdDesc(Store store);
}