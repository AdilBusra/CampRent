package com.camprent.medan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "kategories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Kategori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nama_kategori", nullable = false, unique = true)
    private String namaKategori;

    // ✅ BARU: Kolom untuk menyimpan nama file gambar kategori
    @Column(name = "gambar_kategori")
    private String gambarKategori; // Contoh: "tent-category.jpg"
}