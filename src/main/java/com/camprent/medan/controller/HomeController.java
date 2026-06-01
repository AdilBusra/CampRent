package com.camprent.medan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Kamu bisa pakai ini untuk landing page utama "/" jika dibutuhkan nanti
    @GetMapping("/")
    public String index() {
        return "index"; // atau sesuaikan dengan nama file landing page kalian
    }
}