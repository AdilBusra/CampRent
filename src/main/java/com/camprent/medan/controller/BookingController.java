package com.camprent.medan.controller;

import com.camprent.medan.entity.Customer;
import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Transaksi;
import com.camprent.medan.repository.CustomerRepository;
import com.camprent.medan.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customer/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CustomerRepository customerRepository;

    // Halaman utama booking — form pencarian barang
    @GetMapping
    public String halamanBooking(
            @RequestParam(required = false) String keyword,
            Model model, Authentication auth) {

        if (keyword != null && !keyword.isEmpty()) {
            List<Peralatan> hasilCari = bookingService.cariPeralatan(keyword);
            model.addAttribute("hasilCari", hasilCari);
            model.addAttribute("keyword", keyword);
        }

        return "customer/booking";
    }

    // Halaman form konfirmasi booking
    @GetMapping("/konfirmasi/{peralatanId}")
    public String formKonfirmasi(
            @PathVariable Long peralatanId,
            Model model, Authentication auth) {

        Customer customer = customerRepository
                .findByUserUsername(auth.getName());

        // Cek apakah customer punya transaksi aktif
        if (bookingService.punyaTransaksiAktif(customer)) {
            model.addAttribute("error",
                    "Kamu masih punya booking aktif! " +
                            "Selesaikan dulu sebelum booking baru.");
            return "customer/booking";
        }

        model.addAttribute("peralatanId", peralatanId);
        return "customer/konfirmasi-booking";
    }

    // Proses booking
    @PostMapping("/proses")
    public String prosesBooking(
            @RequestParam Long peralatanId,
            @RequestParam Integer kuantitas,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tanggalSewa,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate tanggalKembali,
            Model model, Authentication auth) {

        try {
            Customer customer = customerRepository
                    .findByUserUsername(auth.getName());

            Transaksi transaksi = bookingService.buatBooking(
                    customer, peralatanId, kuantitas,
                    tanggalSewa, tanggalKembali);

            return "redirect:/customer/booking/riwayat";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("peralatanId", peralatanId);
            return "customer/konfirmasi-booking";
        }
    }

    // Halaman riwayat booking customer
    @GetMapping("/riwayat")
    public String riwayatBooking(Model model, Authentication auth) {
        Customer customer = customerRepository
                .findByUserUsername(auth.getName());
        model.addAttribute("transaksiList",
                bookingService.getTransaksiCustomer(customer));
        return "customer/riwayat-booking";
    }
}
