package com.camprent.medan.repository;

import com.camprent.medan.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Cari profil customer berdasarkan username user-nya
    Customer findByUserUsername(String username);
}
