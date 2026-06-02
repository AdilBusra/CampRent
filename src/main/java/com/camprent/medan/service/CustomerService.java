package com.camprent.medan.service;

import com.camprent.medan.entity.KeranjangItem;
import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Kategori;
import com.camprent.medan.entity.Customer; // Import entity Customer
import com.camprent.medan.repository.KeranjangRepository;
import com.camprent.medan.repository.PeralatanRepository;
import com.camprent.medan.repository.CustomerRepository; // Import repository Customer
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private PeralatanRepository peralatanRepository;

    @Autowired
    private CustomerRepository customerRepository; // Tambahkan ini

    @Autowired
    private KeranjangRepository keranjangRepository;
    /**
     * Mengambil data lengkap Customer berdasarkan username akun login.
     */
    public Customer getProfileByUsername(String username) {
        Optional<Customer> customerOpt = customerRepository.findByUserUsername(username);
        return customerOpt.orElse(null);
        // Note: Idealnya lempar exception jika tidak ketemu, atau return default object.
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

    /**
     * Menyimpan perubahan data profile customer ke database.
     */
    public void updateProfile(String username, String namaLengkap, String email, String nomorTelepon, String nik) {
        Optional<Customer> customerOpt = customerRepository.findByUserUsername(username);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();

            // 1. Update data di tabel Customers
            customer.setNamaLengkap(namaLengkap);
            customer.setNomorTelepon(nomorTelepon);
            customer.setNik(nik);

            // 2. Update data di tabel Users (Email dan Username menempel di entity User)
            if (customer.getUser() != null) {
                customer.getUser().setEmail(email);
                // Catatan: Biasanya username tidak diubah karena menjadi primary key / id login,
                // jadi kita cukup update email-nya saja.
            }

            // 3. Simpan perubahan ke database
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

        // Cek apakah barang sudah ada di keranjang, jika ada tinggal tambah kuantitas
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
}