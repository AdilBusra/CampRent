package com.camprent.medan.repository;

import com.camprent.medan.entity.KeranjangItem;
import com.camprent.medan.entity.Customer;
import com.camprent.medan.entity.Peralatan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeranjangRepository extends JpaRepository<KeranjangItem, Long> {
    List<KeranjangItem> findByCustomer(Customer customer);
    Optional<KeranjangItem> findByCustomerAndPeralatan(Customer customer, Peralatan peralatan);
    void deleteByCustomer(Customer customer);
}