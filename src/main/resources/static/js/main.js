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

function openOfflineRentalModal() {
    const modal = document.getElementById("offlineRentalModal");
    const selectElement = document.getElementById("modalPeralatanSelect");

    if (!modal) return;

    if (selectElement) {
        selectElement.innerHTML = '<option value="" disabled selected>-- Pilih Alat Camping --</option>';

        const listAlat = window.dataPeralatanToko || [];

        listAlat.forEach(alat => {
            const opt = document.createElement("option");
            opt.value = alat.id;
            opt.text = `${alat.namaAlat} (Stock: ${alat.stok})`;
            opt.setAttribute("data-price", alat.hargaSewaPerHari);
            opt.setAttribute("data-name", alat.namaAlat);
            opt.setAttribute("data-stock", alat.stok);
            selectElement.appendChild(opt);
        });
    }

    modal.classList.remove("hidden");
    modal.classList.add("flex");

    updateRentalSummary();
}

function closeOfflineRentalModal() {
    const modal = document.getElementById("offlineRentalModal");
    if (!modal) return;

    modal.classList.add("hidden");
    modal.classList.remove("flex");
}

function closeOfflineRentalModal() {
    const modal = document.getElementById("offlineRentalModal");

    modal.classList.add("hidden");
    modal.classList.remove("flex");
}

function openOfflineDetailModal() {
    const modal = document.getElementById("offlineDetailModal");

    modal.classList.remove("hidden");
    modal.classList.add("flex");
}

function closeOfflineDetailModal() {
    const modal = document.getElementById("offlineDetailModal");

    modal.classList.add("hidden");
    modal.classList.remove("flex");
}

function addRentalItem() {
    const container = document.getElementById("rentalItemsContainer");

    // Ambil referensi dari select pertama sebagai template
    const firstSelect = document.querySelector(".equipment-select");

    const item = document.createElement("div");
    item.className = "rental-item grid grid-cols-12 gap-3 mt-3"; // Tambah mt-3 biar rapi

    // Kita buat select-nya sama persis dengan yang pertama
    item.innerHTML = `
        <div class="col-span-8">
            <select name="peralatanIds" class="equipment-select w-full rounded-xl border px-4 py-3" onchange="updateRentalSummary()">
                ${firstSelect.innerHTML}
            </select>
        </div>
        <div class="col-span-3">
            <input type="number" name="kuantitas" min="1" value="1" oninput="updateRentalSummary()" class="quantity-input w-full rounded-xl border px-4 py-3">
        </div>
        <div class="col-span-1">
            <button type="button" onclick="removeRentalItem(this)" class="h-12 w-full rounded-xl border border-red-200 text-red-500 hover:bg-red-50">×</button>
        </div>
    `;

    container.appendChild(item);
    updateRentalSummary();
}

function removeRentalItem(btn) {
    btn.closest('.rental-item').remove();
    updateRentalSummary();
}

function updateRentalSummary() {
    let dailyTotal = 0;
    let itemCount = 0;

    const summary = document.getElementById("summaryItems");
    const totalRental = document.getElementById("totalRental");
    const durationText = document.getElementById("rentalDurationText");

    if (!summary || !totalRental) return;

    summary.innerHTML = "";

    const rentalDateInput = document.getElementById("offlineRentalDate");
    const returnDateInput = document.getElementById("offlineReturnDate");

    let duration = 1;

    if (rentalDateInput && returnDateInput && rentalDateInput.value && returnDateInput.value) {
        const rentalDate = new Date(rentalDateInput.value);
        const returnDate = new Date(returnDateInput.value);

        const diffTime = returnDate - rentalDate;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        duration = diffDays > 0 ? diffDays : 1;

        if (durationText) {
            durationText.innerText = duration + " day(s)";
        }
    } else {
        if (durationText) {
            durationText.innerText = "Select rental and return date";
        }
    }

    document.querySelectorAll(".rental-item").forEach(item => {
        const equipment = item.querySelector(".equipment-select");
        const quantityInput = item.querySelector(".quantity-input");

        // VALIDASI YANG LEBIH AMAN:
        if (!equipment || equipment.selectedIndex === -1 || equipment.value === "") return;

        const selectedOption = equipment.options[equipment.selectedIndex];
        if (!selectedOption) return; // Mencegah crash jika option kosong

        const maxStock = parseInt(selectedOption.getAttribute("data-stock")) || 0;
        const price = parseInt(selectedOption.getAttribute("data-price")) || 0;
        const name = selectedOption.getAttribute("data-name") || "Item";

        // 2. Ambil nilai qty DULU
        let qty = parseInt(quantityInput.value) || 0;

        // 3. Baru validasi stoknya
        if (qty > maxStock) {
            qty = maxStock;
            quantityInput.value = maxStock;
            alert("Stok hanya tersisa " + maxStock + "!");
        }

        // 4. Hitung subtotal dan total
        const subtotal = price * qty * duration;
        dailyTotal += subtotal;
        itemCount += qty;

        // 5. Tampilkan ke summary
        if (qty > 0) {
            const row = document.createElement("div");
            row.className = "flex justify-between gap-4";
            row.innerHTML = `
                <span>
                    ${name} × ${qty}
                    <span class="text-gray-500">(${duration} day)</span>
                </span>
                <span>Rp${subtotal.toLocaleString("id-ID")}</span>
            `;
            summary.appendChild(row);
        }
    });
    const itemCountText = document.getElementById("itemCountText");

    if (itemCountText) {
        itemCountText.innerText = itemCount + " item(s) selected";
    }

    totalRental.innerText = "Rp" + dailyTotal.toLocaleString("id-ID");
}

function attachListeners() {

    document
        .querySelectorAll(".equipment-select")
        .forEach(el => {

            el.onchange =
                updateRentalSummary;

        });

    document
        .querySelectorAll(".quantity-input")
        .forEach(el => {

            el.oninput =
                updateRentalSummary;

        });
}

document.addEventListener(
    "DOMContentLoaded",
    function () {

        attachListeners();
        updateRentalSummary();

    }
);