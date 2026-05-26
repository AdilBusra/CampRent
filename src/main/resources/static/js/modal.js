function openModal(modalId) {
    const overlay = document.getElementById('modalOverlay');
    const targetModal = document.getElementById(modalId);

    // 1. Reset state: pastikan semua card modal lain tertutup
    document.getElementById('loginModal').classList.add('hidden');
    document.getElementById('signupModal').classList.add('hidden');

    // 2. Tampilkan kontainer luar (overlay) & card target
    overlay.classList.remove('pointer-events-none');
    targetModal.classList.remove('hidden');

    // 3. Trigger efek animasi dissolve (Fade In) dengan setTimeout agar browser sempat me-render state perubahan class
    setTimeout(() => {
        overlay.classList.remove('opacity-0');
        overlay.classList.add('opacity-100');
        targetModal.classList.remove('scale-95');
        targetModal.classList.add('scale-100');
    }, 20);
}

function closeAllModals() {
    const overlay = document.getElementById('modalOverlay');
    const loginModal = document.getElementById('loginModal');
    const signupModal = document.getElementById('signupModal');

    // 1. Memicu animasi fade out (durasi diatur oleh class duration-[800ms] di HTML)
    overlay.classList.remove('opacity-100');
    overlay.classList.add('opacity-0');
    loginModal.classList.remove('scale-100');
    loginModal.classList.add('scale-95');
    signupModal.classList.remove('scale-100');
    signupModal.classList.add('scale-95');

    // 2. Kunci pointer-events & sembunyikan element setelah animasi 800ms selesai berjalan
    setTimeout(() => {
        overlay.classList.add('pointer-events-none');
        loginModal.classList.add('hidden');
        signupModal.classList.add('hidden');
    }, 800);
}

// Menutup modal secara otomatis jika user mengklik area background hijau transparan luar card
onst overlayElement = document.getElementById('modalOverlay');
if (overlayElement) {
    overlayElement.addEventListener('click', function(e) {
        if (e.target === this) {
            closeAllModals();
        }
    });
}
});