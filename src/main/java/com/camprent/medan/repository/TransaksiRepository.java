package com.camprent.medan.repository;

import com.camprent.medan.entity.Customer;
import com.camprent.medan.entity.Store;
import com.camprent.medan.entity.Transaksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransaksiRepository extends JpaRepository<Transaksi, Long> {

    // Cukup gunakan findAll() bawaan JpaRepository untuk mengambil semua data

    // Cek apakah customer punya transaksi aktif (untuk aturan 1 toko)
    List<Transaksi> findByCustomerAndStatusTransaksiIn(
            Customer customer, List<String> statusList);

    // Ambil semua transaksi milik customer
    List<Transaksi> findByCustomer(Customer customer);

    // Ambil semua transaksi milik store
    List<Transaksi> findByStore(Store store);
}