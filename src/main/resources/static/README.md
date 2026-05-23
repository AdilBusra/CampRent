# Folder: Templates (UI Views)
Folder ini merupakan tempat menyimpan seluruh halaman antarmuka (User Interface) aplikasi CampRent.
- **Isi**: File berupa HTML yang menggunakan *template engine* **Thymeleaf** (misalnya: `login.html`, `dashboard.html`, `katalog.html`).
- **Tujuan**: Menampilkan data dinamis yang dikirim oleh tim Backend (Controller) agar bisa dilihat langsung oleh pengguna (Admin, Toko, atau Pelanggan).
- **Catatan Tim FE**: Gunakan atribut khusus Thymeleaf seperti `th:text`, `th:each`, atau `th:action` untuk menghubungkan komponen HTML dengan data dari Backend.