package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
import com.camprent.medan.repository.KategoriRepository;
import com.camprent.medan.repository.StoreRepository;
import com.camprent.medan.service.PeralatanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/store/peralatan")
public class PeralatanController {

    @Autowired
    private PeralatanService peralatanService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private KategoriRepository kategoriRepository;

    @GetMapping("/add")
    public String addPeralatan(Model model, Authentication auth) {
        Store store = storeRepository.findByUserUsername(auth.getName());

        model.addAttribute("peralatan", new Peralatan());

        // Sekarang baris ini aktif, mengirim data kategori asli ke HTML dropdown
        model.addAttribute("listKategori", kategoriRepository.findAll());

        return "store/peralatan-form";
    }

    // 1. Ambil Data List Peralatan Toko (Aman)
    @GetMapping
    public String listPeralatan(Authentication auth, Model model) {
        Store store = storeRepository.findByUserUsername(auth.getName());
        model.addAttribute("peralatanList", peralatanService.getPeralatanByStore(store));
        model.addAttribute("peralatan", new Peralatan());
        return "store/peralatan";
    }

    // 2. Simpan & Update Peralatan (Ditambahkan Proteksi)
    @PostMapping("/save")
    public String savePeralatan(@ModelAttribute Peralatan peralatan, Authentication auth) {
        Store store = storeRepository.findByUserUsername(auth.getName());

        // Proteksi Edit: Jika ini proses update (ID tidak kosong), pastikan barang itu milik toko ini
        if (peralatan.getId() != null) {
            Optional<Peralatan> existingPeralatan = peralatanService.getPeralatanById(peralatan.getId());
            if (existingPeralatan.isEmpty() || !existingPeralatan.get().getStore().getId().equals(store.getId())) {
                return "redirect:/store/peralatan?error=AksesDitolak";
            }
        }

        peralatan.setStore(store);
        peralatanService.savePeralatan(peralatan);
        return "redirect:/store/peralatan";
    }

    // 3. Hapus Peralatan (Diberikan Kunci Validasi Toko)
    @PostMapping("/delete/{id}")
    public String deletePeralatan(@PathVariable Long id, Authentication auth) {
        Store store = storeRepository.findByUserUsername(auth.getName());
        Optional<Peralatan> peralatanOptional = peralatanService.getPeralatanById(id);

        if (peralatanOptional.isPresent() && peralatanOptional.get().getStore().getId().equals(store.getId())) {
            peralatanService.deletePeralatan(id);
        } else {
            return "redirect:/store/peralatan?error=GagalHapus";
        }

        return "redirect:/store/peralatan";
    }

    // 4. Form Edit Peralatan (Diberikan Kunci Validasi Toko)
    @GetMapping("/edit/{id}")
    public String editPeralatan(@PathVariable Long id, Model model, Authentication auth) {
        Store store = storeRepository.findByUserUsername(auth.getName());
        Optional<Peralatan> peralatanOptional = peralatanService.getPeralatanById(id);

        // HANYA boleh diedit jika barangnya ada DAN id tokonya klop
        if (peralatanOptional.isPresent() && peralatanOptional.get().getStore().getId().equals(store.getId())) {
            model.addAttribute("peralatan", peralatanOptional.get());
        } else {
            return "redirect:/store/peralatan?error=AksesDitolak";
        }

        model.addAttribute("peralatanList", peralatanService.getPeralatanByStore(store));
        return "store/peralatan";
    }
}