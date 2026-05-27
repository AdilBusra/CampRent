function toggleStoreSidebar() {
    const sidebar = document.getElementById("storeSidebar");
    const overlay = document.getElementById("sidebarOverlay");

    sidebar.classList.toggle("-translate-x-full");
    overlay.classList.toggle("hidden");
}