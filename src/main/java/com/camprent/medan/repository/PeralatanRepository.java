package com.camprent.medan.repository;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeralatanRepository extends JpaRepository<Peralatan, Long> {

    List<Peralatan> findByStore(Store store);

    List<Peralatan> findByNamaAlatContainingIgnoreCase(String keyword);
}