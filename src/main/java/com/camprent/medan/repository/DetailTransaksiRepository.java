package com.camprent.medan.repository;

import com.camprent.medan.entity.DetailTransaksi;
import com.camprent.medan.entity.Transaksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetailTransaksiRepository extends JpaRepository<DetailTransaksi, Long> {
    // Fungsi otomatis untuk mencari item apa saja yang disewa dalam satu nota transaksi
    List<DetailTransaksi> findByTransaksi(Transaksi transaksi);
}
