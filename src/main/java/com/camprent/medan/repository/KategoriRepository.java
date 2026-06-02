package com.camprent.medan.repository;

import com.camprent.medan.entity.Kategori;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KategoriRepository extends JpaRepository<Kategori, Long> {

    // Menyembuhkan error kompilasi pada AdminService
    boolean existsByNamaKategori(String namaKategori);

    // Fungsi pencarian nama kategori (Case Insensitive)
    List<Kategori> findByNamaKategoriContainingIgnoreCase(String namaKategori);
}