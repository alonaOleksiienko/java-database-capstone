// src/main/resources/static/js/pages/doctorDashboard.js

// --- Imports ---------------------------------------------------------------
// Adjust these import paths to match your project structure.
import { getAllAppointments } from '../api/appointments.js';
import { createPatientRow } from '../components/patientRow.js';

// --- Helpers ---------------------------------------------------------------

/**
 * Return today's date as YYYY-MM-DD (local timezone).
 */
function todayStr() {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * Render a single full-width table row with a message.
 */
function renderMessageRow(tbody, message) {
  const tr = document.createElement('tr');
  const td = document.createElement('td');
  td.colSpan = 5;
  td.textContent = message;
  td.className = 'muted';
  tr.appendChild(td);
  tbody.appendChild(tr);
}

/**
 * Safe text getter for nested fields.
 */
const safe = (v, fallback = '—') => (v === null || v === undefined || v === '' ? fallback : v);

// --- State ----------------------------------------------------------------

let selectedDate = todayStr();
/**
 * Backend expects literal string "null" when no filter is applied.
 * We'll store null in JS and convert to "null" when calling the API.
 */
let patientName = null;

// Token for authenticated requests (if your backend uses JWT)
const token = localStorage.getItem('token');

// --- Core ------------------------------------------------------------------

/**
 * Fetch and render appointments into the table based on selectedDate and patientName.
 */
async function loadAppointments() {
  const tbody = document.getElementById('patientTableBody');
  if (!tbody) return; // page doesn't have the table yet

  // Clear table
  tbody.innerHTML = '';

  try {
    // Convert null -> "null" as required by the backend
    const nameFilter = patientName && patientName.trim() ? patientName.trim() : 'null';

    const appointments = await getAllAppointments(selectedDate, nameFilter, token);

    if (!appointments || appointments.length === 0) {
      renderMessageRow(tbody, 'No Appointments found for selected date.');
      return;
    }

    // Build table rows
    appointments.forEach((appt) => {
      const patient = {
        id: safe(appt?.patient?.id, '—'),
        name: safe(appt?.patient?.name, '—'),
        phone: safe(appt?.patient?.phone, '—'),
        email: safe(appt?.patient?.email, '—'),
      };

      // createPatientRow(appointment, patient) -> returns <tr>...</tr>
      const tr = createPatientRow(appt, patient);
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error('Error loading appointments:', err);
    renderMessageRow(tbody, 'Error loading appointments. Try again later.');
  }
}

/**
 * Wire up search, today filter, and date picker.
 */
function attachControls() {
  const searchInput = document.getElementById('searchBar');
  const todayBtn = document.getElementById('todayButton');
  const datePicker = document.getElementById('datePicker');

  if (searchInput) {
    searchInput.addEventListener('input', async (e) => {
      const value = e.target.value.trim();
      patientName = value.length > 0 ? value : null; // null -> "null" at fetch time
      await loadAppointments();
    });
  }

  if (todayBtn) {
    todayBtn.addEventListener('click', async () => {
      selectedDate = todayStr();
      if (datePicker) datePicker.value = selectedDate;
      await loadAppointments();
    });
  }

  if (datePicker) {
    // Initialize with today's date on first load
    if (!datePicker.value) datePicker.value = selectedDate;

    datePicker.addEventListener('change', async (e) => {
      selectedDate = e.target.value || todayStr();
      await loadAppointments();
    });
  }
}

// --- Boot ------------------------------------------------------------------

window.addEventListener('DOMContentLoaded', async () => {
  // If you have a layout initializer, call it safely
  if (typeof renderContent === 'function') {
    try { renderContent(); } catch { /* no-op */ }
  }
  // Ensure header/footer are rendered if your project expects them
  if (typeof renderHeader === 'function') {
    try { renderHeader(); } catch { /* no-op */ }
  }
  if (typeof renderFooter === 'function') {
    try { renderFooter(); } catch { /* no-op */ }
  }

  attachControls();
  await loadAppointments();
});

