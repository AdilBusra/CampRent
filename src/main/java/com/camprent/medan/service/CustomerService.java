package com.camprent.medan.service;

import com.camprent.medan.repository.KategoriRepository;
import com.camprent.medan.entity.*;
import com.camprent.medan.repository.KeranjangRepository;
import com.camprent.medan.repository.PeralatanRepository;
import com.camprent.medan.repository.CustomerRepository; // Import repository Customer
import com.camprent.medan.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private PeralatanRepository peralatanRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private KeranjangRepository keranjangRepository;

    @Autowired
    private KategoriRepository kategoriRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    /**
     * Mengambil kategori berdasarkan keyword pencarian.
     * Jika keyword kosong, kembalikan semua kategori.
     */
    public List<Kategori> cariKategori(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return kategoriRepository.findAll();
        }
        return kategoriRepository.findByNamaKategoriContainingIgnoreCase(keyword);
    }

    public List<Kategori> getAllKategori() {
        return kategoriRepository.findAll();
    }

    public Customer getProfileByUsername(String username) {
        Optional<Customer> customerOpt = customerRepository.findByUserUsername(username);
        return customerOpt.orElse(null);
    }

    public List<Peralatan> getKatalogUtama() {
        return peralatanRepository.findByStokGreaterThan(0);
    }

    public List<Peralatan> getKatalogByKategori(Kategori kategori) {
        return peralatanRepository.findByKategoriAndStokGreaterThan(kategori, 0);
    }

    public List<Peralatan> cariAlatDiKatalog(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getKatalogUtama();
        }
        return peralatanRepository.findByNamaAlatContainingIgnoreCaseAndStokGreaterThanOrMerekContainingIgnoreCaseAndStokGreaterThan(
                keyword, 0, keyword, 0
        );
    }

    public void updateProfile(String username, String namaLengkap, String email, String nomorTelepon, String nik) {
        Optional<Customer> customerOpt = customerRepository.findByUserUsername(username);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setNamaLengkap(namaLengkap);
            customer.setNomorTelepon(nomorTelepon);
            customer.setNik(nik);

            if (customer.getUser() != null) {
                customer.getUser().setEmail(email);
            }
            customerRepository.save(customer);
        }
    }

    public List<KeranjangItem> getKeranjangCustomer(String username) {
        Customer customer = getProfileByUsername(username);
        return keranjangRepository.findByCustomer(customer);
    }

    public void tambahKeKeranjang(String username, Long peralatanId) {
        Customer customer = getProfileByUsername(username);
        Peralatan peralatan = peralatanRepository.findById(peralatanId)
                .orElseThrow(() -> new RuntimeException("Alat tidak ditemukan"));

        Optional<KeranjangItem> existingItem = keranjangRepository.findByCustomerAndPeralatan(customer, peralatan);

        if (existingItem.isPresent()) {
            KeranjangItem item = existingItem.get();
            item.setKuantitas(item.getKuantitas() + 1);
            keranjangRepository.save(item);
        } else {
            KeranjangItem item = new KeranjangItem();
            item.setCustomer(customer);
            item.setPeralatan(peralatan);
            item.setKuantitas(1);
            keranjangRepository.save(item);
        }
    }

    public void hapusDariKeranjang(Long itemId) {
        keranjangRepository.deleteById(itemId);
    }

    public void updateKuantitas(Long itemId, int jumlah) {
        KeranjangItem item = keranjangRepository.findById(itemId).orElse(null);
        if (item != null) {
            item.setKuantitas(Math.max(1, item.getKuantitas() + jumlah));
            keranjangRepository.save(item);
        }
    }

    // ==========================================
    // METHOD TAMBAHAN UNTUK DETAIL KATEGORI
    // ==========================================

    /**
     * Mengambil data objek Kategori berdasarkan ID-nya
     */
    public Optional<Kategori> getKategoriById(Long id) {
        return kategoriRepository.findById(id);
    }

    /**
     * Mengambil list peralatan berdasarkan ID Kategori yang stoknya ready (> 0)
     */
    public List<Peralatan> getPeralatanByKategoriId(Long kategoriId) {
        // 1. Cari dulu objek kategori-nya dari database
        Optional<Kategori> kategoriOpt = kategoriRepository.findById(kategoriId);

        if (kategoriOpt.isPresent()) {
            // 2. Jika ketemu, lempar objek kategori ke method findByKategoriAndStokGreaterThan yang sudah ada
            return peralatanRepository.findByKategoriAndStokGreaterThan(kategoriOpt.get(), 0);
        }

        // Jika kategori tidak ditemukan, kembalikan list kosong agar tidak error
        return java.util.Collections.emptyList();
    }
    // Tambahkan method ini di paling bawah CustomerService.java sebelum tanda penutup kelas '}'
    public Optional<Peralatan> getPeralatanById(Long id) {
        return peralatanRepository.findById(id);
    }

    public Optional<Store> getStoreById(Long id) {
        return storeRepository.findById(id);
    }

    public List<Peralatan> getPeralatanByStore(Store store) {
        return peralatanRepository.findByStore(store);
    }
}