package com.camprent.medan.repository;

import com.camprent.medan.entity.Transaksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransaksiRepository extends JpaRepository<Transaksi, Long> {
    // Cukup gunakan findAll() bawaan JpaRepository untuk mengambil semua data
}