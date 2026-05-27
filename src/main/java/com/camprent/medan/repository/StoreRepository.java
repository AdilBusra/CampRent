package com.camprent.medan.repository;

import com.camprent.medan.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    // Mencari daftar toko berdasarkan status verifikasinya
    List<Store> findByStatusVerifikasi(String statusVerifikasi);
}