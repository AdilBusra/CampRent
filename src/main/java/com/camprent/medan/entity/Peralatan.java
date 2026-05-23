package com.camprent.medan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "peralatans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Peralatan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne
    @JoinColumn(name = "kategori_id", nullable = false)
    private Kategori kategori;

    @Column(name = "nama_alat", nullable = false)
    private String namaAlat;

    @Column(nullable = false)
    private String merek;

    @Column(nullable = false)
    private Integer stok;

    @Column(name = "harga_sewa_per_hari", nullable = false)
    private BigDecimal hargaSewaPerHari;

    @Column(name = "denda_kerusakan", nullable = false)
    private BigDecimal dendaKerusakan;

    @Column(columnDefinition = "TEXT")
    private String deskripsi;
}