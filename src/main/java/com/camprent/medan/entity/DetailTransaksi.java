package com.camprent.medan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "detail_transaksis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetailTransaksi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transaksi_id", nullable = false)
    private Transaksi transaksi;

    @ManyToOne
    @JoinColumn(name = "peralatan_id", nullable = false)
    private Peralatan peralatan;

    @Column(nullable = false)
    private Integer kuantitas;
}