package com.camprent.medan.service;

import com.camprent.medan.entity.User;
import com.camprent.medan.entity.Customer;
import com.camprent.medan.entity.Store;
import com.camprent.medan.repository.UserRepository;
import com.camprent.medan.repository.CustomerRepository;
import com.camprent.medan.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void registerNewUser(Map<String, String> formData) {
        String username = formData.get("username");

        // 1. Validasi jika username sudah dipakai
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username '" + username + "' sudah terdaftar!");
        }

        // 2. Simpan ke Akun Utama (User.java)
        User user = new User();
        user.setUsername(username);
        user.setEmail(formData.get("email"));
        // Enkripsi password bawaan Spring Security agar aman di database
        user.setPassword(passwordEncoder.encode(formData.get("password")));

        String role = formData.get("role");
        user.setRole(role); // "CUSTOMER" atau "STORE"
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        // 3. Percabangan Logika Profil sesuai Role pilihan di Form Dinamis
        if ("CUSTOMER".equals(role)) {
            Customer customer = new Customer();
            customer.setUser(savedUser);
            customer.setNamaLengkap(formData.get("namaLengkap"));
            customer.setNomorTelepon(formData.get("nomorTeleponCustomer"));
            customer.setNik(formData.get("nik"));
            customerRepository.save(customer);
        } else if ("STORE".equals(role)) {
            Store store = new Store();
            store.setUser(savedUser);
            store.setNamaToko(formData.get("namaToko"));
            store.setNomorTelepon(formData.get("nomorTeleponStore"));
            store.setAlamat(formData.get("alamat"));
            store.setStatusVerifikasi("PENDING"); // Toko baru otomatis PENDING butuh verifikasi Admin
            storeRepository.save(store);
        }
    }
}