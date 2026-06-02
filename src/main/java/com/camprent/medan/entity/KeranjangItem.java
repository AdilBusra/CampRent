package com.camprent.medan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "keranjang_items")
@Getter
@Setter
public class KeranjangItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peralatan_id", nullable = false)
    private Peralatan peralatan;

    @Column(nullable = false)
    private Integer kuantitas = 1;
}