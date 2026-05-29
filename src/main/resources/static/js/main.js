function toggleStoreSidebar() {
    const sidebar = document.getElementById("storeSidebar");
    const overlay = document.getElementById("sidebarOverlay");

    sidebar.classList.toggle("-translate-x-full");
    overlay.classList.toggle("hidden");
}

function openBookingDetailModal() {
    const modal = document.getElementById("bookingDetailModal");
    if (!modal) return;

    modal.classList.remove("hidden");
    modal.classList.add("flex");
}

function closeBookingDetailModal() {
    const modal = document.getElementById("bookingDetailModal");
    if (!modal) return;

    modal.classList.add("hidden");
    modal.classList.remove("flex");
}

function openEditProfileModal() {
    const modal = document.getElementById("editProfileModal");
    modal.classList.remove("hidden");
    modal.classList.add("flex");
}

function closeEditProfileModal() {
    const modal = document.getElementById("editProfileModal");
    modal.classList.add("hidden");
    modal.classList.remove("flex");
}