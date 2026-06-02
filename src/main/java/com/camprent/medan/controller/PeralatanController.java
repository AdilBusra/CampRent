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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/store") // Kita ubah prefix ke /store agar rute di bawahnya lebih fleksibel
public class PeralatanController {

    @Autowired
    private PeralatanService peralatanService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private KategoriRepository kategoriRepository;

    private Store getLoggedInStore(Authentication auth) {
        return storeRepository.findByUserUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Data Toko tidak ditemukan!"));
    }

    // 1. Menampilkan List Peralatan dengan Fitur Filter & Search (URL: /store/equipment)
    @GetMapping("/equipment")
    public String listPeralatan(Authentication auth,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "categoryId", required = false) Long categoryId,
                                Model model) {
        Store store = getLoggedInStore(auth);

        // Menggunakan method filter yang baru dibuat di Service
        List<Peralatan> filteredPeralatan = peralatanService.getPeralatanFiltered(store, keyword, categoryId);

        model.addAttribute("store", store);
        model.addAttribute("listPeralatan", filteredPeralatan);
        model.addAttribute("listKategori", kategoriRepository.findAll());

        // Kirim balik parameternya ke HTML agar nilai di form input/dropdown tidak reset otomatis
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);

        return "store/equipment-list";
    }

    // 2. Membuka Form Tambah Alat (URL: /store/peralatan/add)
    @GetMapping("/peralatan/add")
    public String addPeralatan(Model model, Authentication auth) {
        Store store = getLoggedInStore(auth);

        model.addAttribute("store", store);
        model.addAttribute("peralatan", new Peralatan());
        model.addAttribute("listKategori", kategoriRepository.findAll());

        return "store/equipment-form";
    }

    // 3. Simpan & Update Peralatan
    @PostMapping("/peralatan/save")
    public String savePeralatan(@ModelAttribute Peralatan peralatan,
                                @RequestParam("imageFile") MultipartFile imageFile,
                                Authentication auth) {
        Store store = getLoggedInStore(auth);

        if (peralatan.getId() != null) {
            Optional<Peralatan> existingPeralatan = peralatanService.getPeralatanById(peralatan.getId());
            if (existingPeralatan.isEmpty() || !existingPeralatan.get().getStore().getId().equals(store.getId())) {
                return "redirect:/store/equipment?error=AksesDitolak"; // Redirect ke /store/equipment
            }
        }

        try {
            peralatan.setStore(store);
            peralatanService.savePeralatanWithImage(peralatan, imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/store/equipment?error=GagalUpload";
        }

        return "redirect:/store/equipment"; // Redirect ke /store/equipment setelah sukses
    }

    // 4. Membuka Form Edit Peralatan (URL: /store/equipment/edit/{id})
    @GetMapping("/equipment/edit/{id}")
    public String editPeralatan(@PathVariable Long id, Model model, Authentication auth) {
        Store store = getLoggedInStore(auth);
        Optional<Peralatan> peralatanOptional = peralatanService.getPeralatanById(id);

        if (peralatanOptional.isPresent() && peralatanOptional.get().getStore().getId().equals(store.getId())) {
            model.addAttribute("store", store);
            model.addAttribute("peralatan", peralatanOptional.get());
            model.addAttribute("listKategori", kategoriRepository.findAll());
            return "store/equipment-form";
        } else {
            return "redirect:/store/equipment?error=AksesDitolak";
        }
    }

    // 5. Menghapus Peralatan (URL: /store/equipment/delete/{id})
    @PostMapping("/equipment/delete/{id}")
    public String deletePeralatan(@PathVariable Long id, Authentication auth) {
        Store store = getLoggedInStore(auth);
        Optional<Peralatan> peralatanOptional = peralatanService.getPeralatanById(id);

        if (peralatanOptional.isPresent() && peralatanOptional.get().getStore().getId().equals(store.getId())) {
            peralatanService.deletePeralatan(id);
        } else {
            return "redirect:/store/equipment?error=GagalHapus";
        }

        return "redirect:/store/equipment";
    }
}