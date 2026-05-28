package com.camprent.medan.controller;

import com.camprent.medan.entity.Peralatan;
import com.camprent.medan.entity.Store;
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

    // Halaman daftar peralatan milik toko
    @GetMapping
    public String listPeralatan(Authentication auth, Model model) {
        Store store = storeRepository.findByUserUsername(auth.getName());
        model.addAttribute("peralatanList", peralatanService.getPeralatanByStore(store));
        model.addAttribute("peralatan", new Peralatan());
        return "store/peralatan";
    }

    // Simpan peralatan baru atau update
    @PostMapping("/save")
    public String savePeralatan(@ModelAttribute Peralatan peralatan,
                                Authentication auth) {
        Store store = storeRepository.findByUserUsername(auth.getName());
        peralatan.setStore(store);
        peralatanService.savePeralatan(peralatan);
        return "redirect:/store/peralatan";
    }

    // Hapus peralatan
    @GetMapping("/delete/{id}")
    public String deletePeralatan(@PathVariable Long id) {
        peralatanService.deletePeralatan(id);
        return "redirect:/store/peralatan";
    }

    // Form edit peralatan
    @GetMapping("/edit/{id}")
    public String editPeralatan(@PathVariable Long id, Model model, Authentication auth) {
        Optional<Peralatan> peralatan = peralatanService.getPeralatanById(id);
        Store store = storeRepository.findByUserUsername(auth.getName());
        peralatan.ifPresent(p -> model.addAttribute("peralatan", p));
        model.addAttribute("peralatanList", peralatanService.getPeralatanByStore(store));
        return "store/peralatan";
    }
}
