package com.camprent.medan.repository;

import com.camprent.medan.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; // 1. Tambahkan import ini

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    // Sudah ada dari ketua - JANGAN DIUBAH
    List<Store> findByStatusVerifikasi(String statusVerifikasi);

    // Tambahan baru untuk Backend (Dibuat Optional agar aman dari NullPointer)
    Optional<Store> findByUserUsername(String username);
}