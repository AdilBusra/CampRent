package com.camprent.medan.service;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.entity.DetailTransaksi;
import com.camprent.medan.entity.Store;
import com.camprent.medan.repository.TransaksiRepository;
import com.camprent.medan.repository.DetailTransaksiRepository;
import com.camprent.medan.repository.StoreRepository;
import com.camprent.medan.repository.PeralatanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StoreTransaksiService {

    @Autowired
    private TransaksiRepository transaksiRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PeralatanRepository peralatanRepository;

    @Autowired
    private DetailTransaksiRepository detailTransaksiRepository;

    /**
     * 1. Ambil list transaksi khusus untuk store yang sedang login
     */
    public List<Transaksi> getTransaksiByStore(String username) {
        Store store = storeRepository.findByUserUsername(username).orElse(null);
        return transaksiRepository.findByStoreOrderByIdDesc(store);
    }

    /**
     * 2. Membuat Transaksi Offline (Walk-in Customer)
     * Tanpa memerlukan Customer ID / Akun Dummy (Customer diset ke null)
     */
    @Transactional
    public void createOfflineRental(String username, String customerName, String phone,
                                    String tglSewa, String tglKembali,
                                    List<Long> ids, List<Integer> qtys) {

        // Cari data toko berdasarkan user store yang sedang login
        Store store = storeRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Data toko tidak ditemukan!"));

        LocalDate rentDate = LocalDate.parse(tglSewa);
        LocalDate returnDate = LocalDate.parse(tglKembali);

        // Hitung selisih hari sewa
        long durasiHari = ChronoUnit.DAYS.between(rentDate, returnDate);
        if (durasiHari <= 0) {
            durasiHari = 1; // Proteksi minimal sewa adalah 1 hari
        }

        // Inisialisasi object Transaksi baru sesuai struktur entity kamu
        Transaksi tr = new Transaksi();
        tr.setStore(store);

        // 💡 KUNCI UTAMA: Karena nullable = true, kita bisa set null dengan aman di sini
        tr.setCustomer(null);

        tr.setNamaCustomer(customerName);
        tr.setNoHpCustomer(phone);
        tr.setSource("OFFLINE");
        tr.setTanggalSewa(rentDate);
        tr.setTanggalKembali(returnDate);
        tr.setStatusTransaksi("DIPAKAI"); // Transaksi offline walk-in langsung berstatus DIPAKAI
        tr.setWaktuPemesanan(LocalDateTime.now());
        tr.setWaktuExpire(null); // Offline tidak memerlukan batas waktu kedaluwarsa transfer booking

        // Inisialisasi awal total harga sebelum loop item
        BigDecimal totalHargaSewa = BigDecimal.ZERO;

        // Simpan transaksi pertama kali demi mendapatkan generated ID untuk relasi detail transaksi
        tr = transaksiRepository.save(tr);

        // Loop item alat kamp yang disewa offline
        for (int i = 0; i < ids.size(); i++) {
            Long peralatanId = ids.get(i);
            Integer kuantitasSewa = qtys.get(i);

            Peralatan alat = peralatanRepository.findById(peralatanId)
                    .orElseThrow(() -> new RuntimeException("Peralatan dengan ID " + peralatanId + " tidak ditemukan!"));

            // Validasi ketersediaan stok barang toko fisik
            if (alat.getStok() < kuantitasSewa) {
                throw new RuntimeException("Stok untuk alat '" + alat.getNamaAlat() + "' tidak mencukupi!");
            }

            // Kurangi stok inventory toko secara real-time
            alat.setStok(alat.getStok() - kuantitasSewa);
            peralatanRepository.save(alat);

            // Hitung subtotal: (Harga sewa per hari * kuantitas unit * jumlah hari)
            BigDecimal subtotalAlat = alat.getHargaSewaPerHari()
                    .multiply(BigDecimal.valueOf(kuantitasSewa))
                    .multiply(BigDecimal.valueOf(durasiHari));

            totalHargaSewa = totalHargaSewa.add(subtotalAlat);

            // Masukkan record ke tabel pivot detail_transaksis
            DetailTransaksi detail = new DetailTransaksi();
            detail.setTransaksi(tr);
            detail.setPeralatan(alat);
            detail.setKuantitas(kuantitasSewa);
            detailTransaksiRepository.save(detail);
        }

        // Update nominal total_harga riil hasil akumulasi perhitungan ke transaksi utama
        tr.setTotalHarga(totalHargaSewa);
        transaksiRepository.save(tr);
    }

    /**
     * 3. Aksi Serahkan Barang (Khusus pesanan ONLINE yang di-pickup)
     */
    @Transactional
    public void serahkanBarang(Long transaksiId) {
        Transaksi tr = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan!"));

        if ("PENDING".equals(tr.getStatusTransaksi())) {
            tr.setStatusTransaksi("DIPAKAI");
            transaksiRepository.save(tr);
        }
    }

    /**
     * 4. Aksi Terima Pengembalian Barang (Berlaku untuk ONLINE maupun OFFLINE)
     * Mengembalikan kuantitas alat otomatis ke inventory toko asal
     */
    @Transactional
    public void kembalikanBarang(Long transaksiId) {
        Transaksi tr = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan!"));

        // Lakukan pengembalian jika statusnya barang sedang dibawa/dipakai
        if ("DIPAKAI".equals(tr.getStatusTransaksi()) || "TERLAMBAT".equals(tr.getStatusTransaksi())) {

            List<DetailTransaksi> details = detailTransaksiRepository.findByTransaksi(tr);

            // Kembalikan stok item ke database
            for (DetailTransaksi detail : details) {
                Peralatan alat = detail.getPeralatan();
                alat.setStok(alat.getStok() + detail.getKuantitas());
                peralatanRepository.save(alat);
            }

            // Tandai status sewa telah selesai penuh
            tr.setStatusTransaksi("SELESAI");
            transaksiRepository.save(tr);
        }
    }
}