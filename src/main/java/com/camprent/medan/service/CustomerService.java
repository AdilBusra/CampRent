package com.camprent.medan.service;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Kategori;
import com.camprent.medan.entity.Customer; // Import entity Customer
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
}