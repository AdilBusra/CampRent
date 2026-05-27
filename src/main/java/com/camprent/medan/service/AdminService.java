package com.camprent.medan.service;

import com.camprent.medan.entity.Store;
import com.camprent.medan.repository.KategoriRepository;
import com.camprent.medan.repository.StoreRepository;
import com.camprent.medan.repository.TransaksiRepository;
import com.camprent.medan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    // Tambahkan di dalam AdminService.java kamu

    @Autowired
    private UserRepository userRepository; // Pastikan ini sudah di-inject di atas, jika belum tambahkan ini

    public long countTotalCustomer() {
        return userRepository.countByRole("CUSTOMER");
    }

    public long countVerifiedStore() {
        // Kita filter toko yang statusnya sudah VERIFIED
        return storeRepository.findByStatusVerifikasi("VERIFIED").size();
    }

    public long countPendingStore() {
        // Kita filter toko yang statusnya masih PENDING
        return storeRepository.findByStatusVerifikasi("PENDING").size();
    }

    public long countTotalTransaksi() {
        return transaksiRepository.count();
    }

    @Autowired
    private StoreRepository storeRepository;

    // Ambil semua toko
    public List<Store> getAllStores() {
        return storeRepository.findAll();
    }

    // Proses Validasi Toko (Ubah status PENDING -> VERIFIED / REJECTED)
    public boolean validasiToko(Long storeId, String statusBaru) {
        Optional<Store> storeOptional = storeRepository.findById(storeId);

        if (storeOptional.isPresent()) {
            Store store = storeOptional.get();
            // Validasi input agar hanya menerima VERIFIED atau REJECTED
            if ("VERIFIED".equals(statusBaru) || "REJECTED".equals(statusBaru)) {
                store.setStatusVerifikasi(statusBaru);
                storeRepository.save(store);
                return true;
            }
        }
        return false;
    }

    // Tambahkan di dalam file AdminService.java kamu

    @Autowired
    private KategoriRepository kategoriRepository;

    // Ambil semua kategori alat gunung
    public List<com.camprent.medan.entity.Kategori> getAllKategori() {
        return kategoriRepository.findAll();
    }

    // Tambah kategori baru dengan validasi agar tidak duplikat
    public boolean tambahKategori(String namaKategori) {
        if (namaKategori == null || namaKategori.trim().isEmpty()) {
            return false;
        }

        // Jika sudah ada nama yang sama, batalkan demi mematuhi aturan unique = true
        if (kategoriRepository.existsByNamaKategori(namaKategori)) {
            return false;
        }

        com.camprent.medan.entity.Kategori kategori = new com.camprent.medan.entity.Kategori();
        kategori.setNamaKategori(namaKategori);
        kategoriRepository.save(kategori);
        return true;
    }

    // Tambahkan di dalam file AdminService.java kamu

    @Autowired
    private TransaksiRepository transaksiRepository;

    // Ambil semua riwayat transaksi global untuk dipantau Admin
    public List<com.camprent.medan.entity.Transaksi> getAllTransaksiGlobal() {
        return transaksiRepository.findAll();
    }
}