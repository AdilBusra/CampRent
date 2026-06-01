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
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class StoreTransaksiService {

    @Autowired
    private TransaksiRepository transaksiRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired private PeralatanRepository peralatanRepository; // Pastikan ini sudah ada!

    @Autowired
    private DetailTransaksiRepository detailTransaksiRepository;

    // 1. Ambil list transaksi khusus untuk store yang sedang login
    public List<Transaksi> getTransaksiByStore(String username) {
        Store store = storeRepository.findByUserUsername(username).orElse(null);
        return transaksiRepository.findByStoreOrderByIdDesc(store);
    }

    // 2. Update status saat customer mengambil alat camping (PENDING -> DIPAKAI)
    @Transactional
    public void serahkanBarang(Long transaksiId) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        if ("PENDING".equals(transaksi.getStatusTransaksi())) {
            transaksi.setStatusTransaksi("DIPAKAI");
            transaksiRepository.save(transaksi);
        }
    }

    // 3. CORE LOGIC: Update status saat pengembalian barang + HITUNG DENDA OTOMATIS
    @Transactional
    public void kembalikanBarang(Long transaksiId) {
        Transaksi transaksi = transaksiRepository.findById(transaksiId)
                .orElseThrow(() -> new IllegalArgumentException("Transaksi tidak ditemukan"));

        if (!"DIPAKAI".equals(transaksi.getStatusTransaksi()) && !"TERLAMBAT".equals(transaksi.getStatusTransaksi())) {
            throw new IllegalStateException("Transaksi tidak dalam status disewa!");
        }

        LocalDate hariIni = LocalDate.now();
        LocalDate batasKembali = transaksi.getTanggalKembali();

        // Cek apakah pengembaliannya terlambat dari tenggat waktu
        if (hariIni.isAfter(batasKembali)) {
            // Hitung selisih hari keterlambatan
            long hariTerlambat = ChronoUnit.DAYS.between(batasKembali, hariIni);

            // Ambil semua detail item dalam transaksi ini untuk menjumlahkan denda kerusakannya/hari
            List<DetailTransaksi> details = detailTransaksiRepository.findByTransaksi(transaksi);
            BigDecimal totalDendaAkumulasi = BigDecimal.ZERO;

            for (DetailTransaksi detail : details) {
                // Rumus PBO: Denda per alat x Kuantitas x Jumlah Hari Terlambat
                BigDecimal dendaPerItem = detail.getPeralatan().getDendaKerusakan()
                        .multiply(BigDecimal.valueOf(detail.getKuantitas()))
                        .multiply(BigDecimal.valueOf(hariTerlambat));

                totalDendaAkumulasi = totalDendaAkumulasi.add(dendaPerItem);
            }

            // Tambahkan denda ke total harga transaksi akhir
            transaksi.setTotalHarga(transaksi.getTotalHarga().add(totalDendaAkumulasi));
            transaksi.setStatusTransaksi("TERLAMBAT");
        } else {
            transaksi.setStatusTransaksi("SELESAI");
        }

        transaksiRepository.save(transaksi);
    }

    // Tambahkan di dalam StoreTransaksiService.java

    @Transactional
    public void createOfflineRental(String username, String customerName, String phone,
                                    String tglSewa, String tglKembali,
                                    List<Long> ids, List<Integer> qtys) {

        Store store = storeRepository.findByUserUsername(username).orElseThrow();

        Transaksi tr = new Transaksi();
        tr.setStore(store);
        tr.setNamaCustomer(customerName);
        tr.setNoHpCustomer(phone);
        tr.setSource("OFFLINE");
        tr.setTanggalSewa(LocalDate.parse(tglSewa));
        tr.setTanggalKembali(LocalDate.parse(tglKembali));
        tr.setStatusTransaksi("DIPAKAI");

        // 1. Inisialisasi total harga
        BigDecimal total = BigDecimal.ZERO;

        // Simpan transaksi dulu supaya dapat ID-nya
        tr = transaksiRepository.save(tr);

        // 2. Loop untuk simpan detail dan kurangi stok
        for(int i = 0; i < ids.size(); i++) {
            Peralatan alat = peralatanRepository.findById(ids.get(i)).orElseThrow();

            // Kurangi stok
            alat.setStok(alat.getStok() - qtys.get(i));
            peralatanRepository.save(alat);

            // Hitung total (Harga sewa x jumlah)
            // Pastikan nama method di Peralatan sesuai (misal: getHargaSewaPerHari)
            BigDecimal subtotal = alat.getHargaSewaPerHari().multiply(BigDecimal.valueOf(qtys.get(i)));
            total = total.add(subtotal);

            // 3. Simpan Detail Transaksi
            DetailTransaksi detail = new DetailTransaksi();
            detail.setTransaksi(tr);
            detail.setPeralatan(alat);
            detail.setKuantitas(qtys.get(i));
            detailTransaksiRepository.save(detail);
        }

        // 4. Update total harga ke transaksi
        tr.setTotalHarga(total);
        transaksiRepository.save(tr);
    }
}
