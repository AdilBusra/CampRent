package com.camprent.medan.repository;

import com.camprent.medan.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Fungsi vital: Mencari profil Customer berdasarkan username akun Login-nya
    Optional<Customer> findByUserUsername(String username);
}
