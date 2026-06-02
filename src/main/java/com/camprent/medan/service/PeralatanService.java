package com.camprent.medan.service;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
import com.camprent.medan.repository.PeralatanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile; // Tambahkan import ini

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // Tambahkan import ini

@Service
public class PeralatanService {

    @Autowired
    private PeralatanRepository peralatanRepository;

    // Lokasi folder tempat menyimpan gambar secara fisik di dalam proyek
    private final String UPLOAD_DIR = "src/main/resources/static/uploads/peralatan/";

    public List<Peralatan> getPeralatanByStore(Store store) {
        return peralatanRepository.findByStore(store);
    }

    // UPDATE: Method save dimodifikasi agar bisa memproses file gambar dari HTML
    public Peralatan savePeralatanWithImage(Peralatan peralatan, MultipartFile imageFile) throws IOException {

        if (imageFile != null && !imageFile.isEmpty()) {
            // 1. Ambil ekstensi asli file (misal: .jpg / .png)
            String originalName = imageFile.getOriginalFilename();
            String fileExtension = originalName.substring(originalName.lastIndexOf("."));

            // 2. Buat nama file unik acak (UUID) supaya kalau ada nama file sama, tidak saling menimpa
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // 3. Pastikan folder tujuan sudah dibuat di komputer
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 4. Salin file fisik dari browser ke folder static proyek
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 5. Simpan HANYA STRING NAMA FILE-nya saja ke kolom database yang baru kita buat
            peralatan.setFotoPeralatan(uniqueFileName);

        } else if (peralatan.getId() != null) {
            // KONDISI EDIT: Jika sedang edit data dan tidak upload foto baru, pakai foto lama dari DB
            Optional<Peralatan> oldData = peralatanRepository.findById(peralatan.getId());
            oldData.ifPresent(p -> peralatan.setFotoPeralatan(p.getFotoPeralatan()));
        } else {
            // KONDISI BARU: Jika tambah barang baru tanpa foto, kasih foto default
            peralatan.setFotoPeralatan("default-equipment.jpg");
        }

        return peralatanRepository.save(peralatan);
    }

    public void deletePeralatan(Long id) {
        peralatanRepository.deleteById(id);
    }

    public Optional<Peralatan> getPeralatanById(Long id) {
        return peralatanRepository.findById(id);
    }

    public List<Peralatan> searchPeralatan(String keyword) {
        return peralatanRepository.findByNamaAlatContainingIgnoreCase(keyword);
    }

    // UPDATE TAMBAHAN: Logika filter pencarian untuk sisi Store
    public List<Peralatan> getPeralatanFiltered(Store store, String keyword, Long categoryId) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasCategory = categoryId != null;

        if (hasKeyword && hasCategory) {
            return peralatanRepository.findByStoreAndNamaAlatContainingIgnoreCaseAndKategoriId(store, keyword, categoryId);
        } else if (hasKeyword) {
            return peralatanRepository.findByStoreAndNamaAlatContainingIgnoreCase(store, keyword);
        } else if (hasCategory) {
            return peralatanRepository.findByStoreAndKategoriId(store, categoryId);
        } else {
            return peralatanRepository.findByStore(store);
        }
    }
}