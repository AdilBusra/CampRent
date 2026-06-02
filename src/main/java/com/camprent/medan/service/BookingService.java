package com.camprent.medan.service;

import com.camprent.medan.entity.*;
import com.camprent.medan.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private TransaksiRepository transaksiRepository;

    @Autowired
    private DetailTransaksiRepository detailTransaksiRepository;

    @Autowired
    private PeralatanRepository peralatanRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // Cari peralatan berdasarkan nama (untuk fitur search customer)
    public List<Peralatan> cariPeralatan(String keyword) {
        return peralatanRepository.findByNamaAlatContainingIgnoreCase(keyword);
    }

    // Cek apakah customer punya transaksi aktif di toko lain
    public boolean punyaTransaksiAktif(Customer customer) {
        List<String> statusAktif = List.of("PENDING", "DIPAKAI");
        List<Transaksi> transaksiAktif = transaksiRepository
                .findByCustomerAndStatusTransaksiIn(customer, statusAktif);
        return !transaksiAktif.isEmpty();
    }

    // Buat transaksi booking baru
    @Transactional
    public Transaksi buatBooking(Customer customer, Long peralatanId,
                                 Integer kuantitas, LocalDate tanggalSewa,
                                 LocalDate tanggalKembali) {

        // Cek aturan: tidak boleh booking jika masih ada transaksi aktif
        if (punyaTransaksiAktif(customer)) {
            throw new RuntimeException(
                    "Kamu masih punya booking aktif! " +
                            "Selesaikan dulu sebelum booking di toko lain.");
        }

        // Ambil data peralatan
        Peralatan peralatan = peralatanRepository.findById(peralatanId)
                .orElseThrow(() -> new RuntimeException("Peralatan tidak ditemukan!"));

        // Cek stok tersedia
        if (peralatan.getStok() < kuantitas) {
            throw new RuntimeException(
                    "Stok tidak mencukupi! Stok tersedia: " + peralatan.getStok());
        }

        // Hitung jumlah hari sewa
        long jumlahHari = ChronoUnit.DAYS.between(tanggalSewa, tanggalKembali);
        if (jumlahHari <= 0) {
            throw new RuntimeException(
                    "Tanggal kembali harus setelah tanggal sewa!");
        }

        // Hitung total harga: hargaSewaPerHari x kuantitas x jumlah hari
        BigDecimal totalHarga = peralatan.getHargaSewaPerHari()
                .multiply(BigDecimal.valueOf(kuantitas))
                .multiply(BigDecimal.valueOf(jumlahHari));

        // Buat transaksi baru
        Transaksi transaksi = new Transaksi();
        transaksi.setCustomer(customer);
        transaksi.setStore(peralatan.getStore());
        transaksi.setTanggalSewa(tanggalSewa);
        transaksi.setTanggalKembali(tanggalKembali);
        transaksi.setTotalHarga(totalHarga);
        transaksi.setStatusTransaksi("PENDING");
        transaksiRepository.save(transaksi);

        // Buat detail transaksi
        DetailTransaksi detail = new DetailTransaksi();
        detail.setTransaksi(transaksi);
        detail.setPeralatan(peralatan);
        detail.setKuantitas(kuantitas);
        detailTransaksiRepository.save(detail);

        // Kurangi stok peralatan
        peralatan.setStok(peralatan.getStok() - kuantitas);
        peralatanRepository.save(peralatan);

        return transaksi;
    }

    // Ambil semua transaksi milik customer
    public List<Transaksi> getTransaksiCustomer(Customer customer) {
        return transaksiRepository.findByCustomer(customer);
    }
}