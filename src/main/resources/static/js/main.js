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
    if (!modal) return;

    modal.classList.remove("hidden");
    modal.classList.add("flex");
}

function closeOfflineRentalModal() {
    const modal = document.getElementById("offlineRentalModal");
    if (!modal) return;

    modal.classList.add("hidden");
    modal.classList.remove("flex");
}

function openOfflineRentalModal() {
    const modal = document.getElementById("offlineRentalModal");

    modal.classList.remove("hidden");
    modal.classList.add("flex");
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

    const container =
        document.getElementById("rentalItemsContainer");

    const item = document.createElement("div");

    item.className =
        "rental-item grid grid-cols-12 gap-3";

    item.innerHTML = `

        <div class="col-span-8">

            <select
                class="equipment-select w-full rounded-xl border px-4 py-3">

                <option value="80000">Tent Dome Borneo 4 (Stock: 5)</option>
                <option value="70000">Carrier Eiger 60L (Stock: 3)</option>
                <option value="30000">Sleeping Bag (Stock: 10)</option>
                <option value="50000">Portable Stove (Stock: 8)</option>

            </select>

        </div>

        <div class="col-span-3">

            <input
                type="number"
                min="1"
                value="1"
                class="quantity-input w-full rounded-xl border px-4 py-3">

        </div>

        <div class="col-span-1">

            <button
                type="button"
                onclick="removeRentalItem(this)"
                class="h-12 w-full rounded-xl border border-red-200 text-red-500 hover:bg-red-50">
                ×
            </button>

        </div>
    `;

    container.appendChild(item);

    attachListeners();
    updateRentalSummary();
}

function removeRentalItem(button) {

    button.closest(".rental-item").remove();

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

        const qty = parseInt(quantityInput.value) || 0;
        const price = parseInt(equipment.value) || 0;
        const subtotal = price * qty * duration;

        dailyTotal += subtotal;
        itemCount += qty;

        const row = document.createElement("div");
        row.className = "flex justify-between gap-4";

        row.innerHTML = `
            <span>
                ${equipment.options[equipment.selectedIndex].text} × ${qty}
                <span class="text-gray-500">(${duration} day)</span>
            </span>

            <span>
                Rp${subtotal.toLocaleString("id-ID")}
            </span>
        `;

        summary.appendChild(row);
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