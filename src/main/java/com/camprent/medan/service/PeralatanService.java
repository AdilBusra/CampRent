package com.camprent.medan.service;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
import com.camprent.medan.repository.PeralatanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PeralatanService {

    @Autowired
    private PeralatanRepository peralatanRepository;

    // Ambil semua peralatan milik satu toko
    public List<Peralatan> getPeralatanByStore(Store store) {
        return peralatanRepository.findByStore(store);
    }

    // Tambah atau update peralatan
    public Peralatan savePeralatan(Peralatan peralatan) {
        return peralatanRepository.save(peralatan);
    }

    // Hapus peralatan berdasarkan ID
    public void deletePeralatan(Long id) {
        peralatanRepository.deleteById(id);
    }

    // Cari peralatan berdasarkan ID
    public Optional<Peralatan> getPeralatanById(Long id) {
        return peralatanRepository.findById(id);
    }

    // Cari peralatan berdasarkan keyword
    public List<Peralatan> searchPeralatan(String keyword) {
        return peralatanRepository.findByNamaAlatContainingIgnoreCase(keyword);
    }
}
