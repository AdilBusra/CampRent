package com.camprent.medan.repository;

import com.camprent.medan.entity.Kategori;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KategoriRepository extends JpaRepository<Kategori, Long> {
    // Mengecek apakah nama kategori sudah ada atau belum (karena di aturan database harus unique)
    boolean existsByNamaKategori(String namaKategori);
}