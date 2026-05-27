package com.camprent.medan.repository;

import com.camprent.medan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Fungsi untuk mencari user berdasarkan username saat login nanti
    Optional<User> findByUsername(String username);

    // Fungsi untuk menghitung berapa banyak akun dengan role tertentu di database
    long countByRole(String role);
}