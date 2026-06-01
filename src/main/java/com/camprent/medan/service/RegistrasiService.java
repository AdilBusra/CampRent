package com.camprent.medan.service;

import com.camprent.medan.entity.Customer;
import com.camprent.medan.entity.Store;
import com.camprent.medan.entity.User;
import com.camprent.medan.repository.CustomerRepository;
import com.camprent.medan.repository.StoreRepository;
import com.camprent.medan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegistrasiService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // Registrasi akun Store baru
    public void daftarStore(String username, String password, String email,
                            String namaToko, String alamat, String nomorTelepon) {
        // Buat akun User dulu
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole("STORE");
        user.setIsActive(false); // Belum aktif, tunggu verifikasi Admin
        userRepository.save(user);

        // Otomatis buat profil Store
        Store store = new Store();
        store.setUser(user);
        store.setNamaToko(namaToko);
        store.setAlamat(alamat);
        store.setNomorTelepon(nomorTelepon);
        store.setStatusVerifikasi("PENDING");
        storeRepository.save(store);
    }

    // Registrasi akun Customer baru
    public void daftarCustomer(String username, String password, String email,
                               String namaLengkap, String nomorTelepon, String nik) {
        // Buat akun User dulu
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setRole("CUSTOMER");
        user.setIsActive(true); // Langsung aktif tanpa verifikasi
        userRepository.save(user);

        // Otomatis buat profil Customer
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setNamaLengkap(namaLengkap);
        customer.setNomorTelepon(nomorTelepon);
        customer.setNik(nik);
        customerRepository.save(customer);
    }
}