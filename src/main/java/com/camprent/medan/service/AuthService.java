package com.camprent.medan.service;

import com.camprent.medan.entity.User;
import com.camprent.medan.entity.Customer;
import com.camprent.medan.entity.Store;
import com.camprent.medan.repository.UserRepository;
import com.camprent.medan.repository.CustomerRepository;
import com.camprent.medan.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
// TAMBAHKAN 'implements UserDetailsService' di sini
public class AuthService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- METHOD UNTUK PROSES LOGIN (WAJIB ADA) ---
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Ambil data user dari database berdasarkan username inputan form login
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan dengan username: " + username));

        // Bungkus data Entity User kita ke dalam object UserDetails milik Spring Security
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole()) // Ini yang melempar role ADMIN, STORE, atau CUSTOMER
                .disabled(!user.getIsActive()) // Menahan akun jika belum di-approve admin
                .build();
    }

    // --- METHOD REGISTRASI (Kode lamamu di bawah ini jangan diubah) ---
    @Transactional
    public void registerNewUser(Map<String, String> formData) {
        String username = formData.get("username");

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username '" + username + "' sudah terdaftar!");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(formData.get("email"));
        user.setPassword(passwordEncoder.encode(formData.get("password")));

        String role = formData.get("role");
        user.setRole(role);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

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
            store.setStatusVerifikasi("PENDING");
            storeRepository.save(store);
        }
    }
}