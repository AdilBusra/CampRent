package com.camprent.medan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "nama_toko", nullable = false)
    private String namaToko;

    @Column(nullable = false)
    private String alamat;

    @Column(name = "nomor_telepon", nullable = false)
    private String nomorTelepon;

    @Column(name = "status_verifikasi", nullable = false)
    private String statusVerifikasi = "PENDING"; // PENDING, VERIFIED, REJECTED
}