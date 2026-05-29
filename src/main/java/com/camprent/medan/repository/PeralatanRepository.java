package com.camprent.medan.repository;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
import com.camprent.medan.entity.Kategori; // Tambahkan import ini
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface





















































PeralatanRepository extends JpaRepository<Peralatan, Long> {

    // === FITUR UNTUK SISI STORE (Bawaan Kawanmu - JANGAN DIHAPUS) ===
    List<Peralatan> findByStore(Store store);
    List<Peralatan> findByNamaAlatContainingIgnoreCase(String keyword);
    long countByStore(Store store); // Ini yang kita tambahkan kemarin untuk hitung total alat


    // === FITUR TAMBAHAN UNTUK SISI CUSTOMER (Tambahkan di bawah sini) ===

    // 1. Menampilkan katalog utama customer (Hanya alat yang stoknya > 0)
    List<Peralatan> findByStokGreaterThan(Integer stok);

    // 2. Filter katalog berdasarkan Kategori (Hanya yang stoknya > 0)
    List<Peralatan> findByKategoriAndStokGreaterThan(Kategori kategori, Integer stok);

    // 3. Pencarian gabungan nama/merek yang stoknya ready (Case-Insensitive)
    List<Peralatan> findByNamaAlatContainingIgnoreCaseAndStokGreaterThanOrMerekContainingIgnoreCaseAndStokGreaterThan(
            String namaKeyword, Integer stokNama, String merekKeyword, Integer stokMerek
    );
}