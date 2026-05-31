package com.camprent.medan.service;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Kategori;
import com.camprent.medan.repository.PeralatanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private PeralatanRepository peralatanRepository;

    /**
     * 1. Mengambil seluruh peralatan untuk katalog utama customer.
     * Hanya menampilkan alat yang stoknya lebih dari 0.
     */
    public List<Peralatan> getKatalogUtama() {
        return peralatanRepository.findByStokGreaterThan(0);
    }

    /**
     * 2. Memfilter peralatan berdasarkan kategori pilihan customer.
     * Memastikan barang yang tampil dalam kategori tersebut stoknya ready (> 0).
     */
    public List<Peralatan> getKatalogByKategori(Kategori kategori) {
        return peralatanRepository.findByKategoriAndStokGreaterThan(kategori, 0);
    }

    /**
     * 3. Mencari peralatan berdasarkan kata kunci nama alat atau merek.
     * Menggunakan fungsi gabungan case-insensitive yang sudah kita tambahkan di repository.
     */
    public List<Peralatan> cariAlatDiKatalog(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getKatalogUtama();
        }
        // Kita masukkan keyword ke parameter nama dan merek, serta mengunci batas stok minimal 0
        return peralatanRepository.findByNamaAlatContainingIgnoreCaseAndStokGreaterThanOrMerekContainingIgnoreCaseAndStokGreaterThan(
                keyword, 0, keyword, 0
        );
    }
}