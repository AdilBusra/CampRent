function openModal(modalId) {
    const overlay = document.getElementById('modalOverlay');
    const targetModal = document.getElementById(modalId);

    if(!overlay || !targetModal) return;

    document.getElementById('loginModal').classList.add('hidden');
    document.getElementById('signupModal').classList.add('hidden');

    overlay.classList.remove('pointer-events-none');
    targetModal.classList.remove('hidden');

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

    if(!overlay) return;

    overlay.classList.remove('opacity-100');
    overlay.classList.add('opacity-0');
    if(loginModal) { loginModal.classList.remove('scale-100'); loginModal.classList.add('scale-95'); }
    if(signupModal) { signupModal.classList.remove('scale-100'); signupModal.classList.add('scale-95'); }

    setTimeout(() => {
        overlay.classList.add('pointer-events-none');
        if(loginModal) loginModal.classList.add('hidden');
        if(signupModal) signupModal.classList.add('hidden');
    }, 800);
}

window.addEventListener('DOMContentLoaded', () => {
    const overlay = document.getElementById('modalOverlay');
    if (overlay) {
        overlay.addEventListener('click', function(e) {
            if (e.target === this) {
                closeAllModals();
            }
        });
    }
});