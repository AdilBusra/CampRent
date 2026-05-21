# Folder: Exception
Folder ini digunakan untuk menangani error kustom yang mungkin terjadi di aplikasi.
- **Isi**: Class *Exception* kustom (seperti `ResourceNotFoundException`) dan *Global Exception Handler* (yang menggunakan anotasi `@ControllerAdvice`).
- **Tujuan**: Memastikan aplikasi memberikan pesan error yang seragam dan informatif kepada user/frontend, bukan sekadar pesan error teknis yang membingungkan.