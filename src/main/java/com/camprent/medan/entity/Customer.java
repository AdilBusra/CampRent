package com.camprent.medan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "nama_lengkap", nullable = false)
    private String namaLengkap;

    @Column(name = "nomor_telepon", nullable = false)
    private String nomorTelepon;

    @Column(nullable = false, unique = true)
    private String nik;
}