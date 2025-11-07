// src/main/resources/static/js/pages/adminDashboard.js

// ------------------- Imports (adjust paths for your project) -------------------
import { getDoctors, filterDoctors, saveDoctor } from '../api/doctors.js';
import { createDoctorCard } from '../components/doctorCard.js';

// ------------------- Utilities -------------------
const $ = (sel) => document.querySelector(sel);

function clearEl(el) {
  if (el) el.innerHTML = '';
}

function showMessage(el, msg) {
  if (!el) return;
  el.innerHTML = `<div class="empty-state">${msg}</div>`;
}

// ------------------- Render / Load -------------------

/**
 * Fetch all doctors and render as cards.
 */
export async function loadDoctorCards() {
  const content = $('#content');
  if (!content) return;

  clearEl(content);

  try {
    const doctors = await getDoctors();
    if (!doctors || doctors.length === 0) {
      showMessage(content, 'No doctors found.');
      return;
    }
    renderDoctorCards(doctors);
  } catch (err) {
    console.error('Failed to load doctors', err);
    showMessage(content, 'Error loading doctors. Please try again later.');
  }
}

/**
 * Render a given list of doctors as cards.
 * @param {Array<Object>} doctors
 */
function renderDoctorCards(doctors) {
  const content = $('#content');
  if (!content) return;
  clearEl(content);

  doctors.forEach((doc) => {
    const card = createDoctorCard(doc);
    content.appendChild(card);
  });
}

// ------------------- Filtering -------------------

/**
 * Read UI filters and re-render the list accordingly.
 */
export async function filterDoctorsOnChange() {
  const content = $('#content');
  if (!content) return;

  const name = ($('#searchBar')?.value || '').trim() || null;
  const time = ($('#timeFilter')?.value || '').trim() || null;
  const specialty = ($('#specialtyFilter')?.value || '').trim() || null;

  clearEl(content);
  showMessage(content, 'Loading…');

  try {
    const doctors = await filterDoctors(name, time, specialty);
    if (!doctors || doctors.length === 0) {
      showMessage(content, 'No doctors found with the given filters.');
      return;
    }
    renderDoctorCards(doctors);
  } catch (err) {
    console.error('Filter error:', err);
    alert('Error applying filters. Please try again.');
    // Reload full list as a fallback
    await loadDoctorCards();
  }
}

// ------------------- Add Doctor -------------------

/**
 * Collect form values from modal and save a doctor.
 * Expects the modal to contain:
 *  #docName, #docEmail, #docPhone, #docPassword, #docSpecialty, #docAvailableTimes
 *  where available times is a comma-separated list (e.g., "09:00-10:00, 10:00-11:00")
 */
export async function adminAddDoctor() {
  const token = localStorage.getItem('token');
  if (!token) {
    alert('You must be logged in as Admin to add doctors.');
    return;
  }

  const name = $('#docName')?.value?.trim();
  const email = $('#docEmail')?.value?.trim();
  const phone = $('#docPhone')?.value?.trim();
  const password = $('#docPassword')?.value?.trim();
  const specialty = $('#docSpecialty')?.value?.trim();
  const timesRaw = $('#docAvailableTimes')?.value || '';
  const availableTimes = timesRaw
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0);

  // Basic checks
  if (!name || !email || !phone || !password || !specialty) {
    alert('Please fill in all required fields.');
    return;
  }

  const doctor = { name, email, phone, password, specialty, availableTimes };

  try {
    await saveDoctor(doctor, token);
    alert('Doctor added successfully!');
    // Close modal if your project provides closeModal()
    if (typeof closeModal === 'function') closeModal();
    // Reload list
    await loadDoctorCards();
  } catch (err) {
    console.error('Failed to save doctor:', err);
    alert('Failed to add doctor. Please check inputs and try again.');
  }
}

// ------------------- Events / Boot -------------------

/**
 * Attach listeners to Add Doctor button and filter controls.
 */
function attachListeners() {
  // Add Doctor
  const addBtn = $('#addDocBtn');
  if (addBtn) {
    addBtn.addEventListener('click', () => {
      if (typeof openModal === 'function') {
        openModal('addDoctor');
      } else {
        alert('Modal system not found. Implement openModal("addDoctor").');
      }
    });
  }

  // Filters
  const search = $('#searchBar');
  const timeFilter = $('#timeFilter');
  const specialtyFilter = $('#specialtyFilter');

  if (search) search.addEventListener('input', filterDoctorsOnChange);
  if (timeFilter) timeFilter.addEventListener('change', filterDoctorsOnChange);
  if (specialtyFilter) specialtyFilter.addEventListener('change', filterDoctorsOnChange);

  // If your modal form uses a submit button with id #saveDoctorBtn:
  const saveDoctorBtn = $('#saveDoctorBtn');
  if (saveDoctorBtn) {
    saveDoctorBtn.addEventListener('click', async (e) => {
      e.preventDefault();
      await adminAddDoctor();
    });
  }
}

window.addEventListener('DOMContentLoaded', async () => {
  // Optional layout bootstrapping if present in your project:
  if (typeof renderContent === 'function') {
    try { renderContent(); } catch { /* noop */ }
  }
  if (typeof renderHeader === 'function') {
    try { renderHeader(); } catch { /* noop */ }
  }
  if (typeof renderFooter === 'function') {
    try { renderFooter(); } catch { /* noop */ }
  }

  attachListeners();
  await loadDoctorCards();
});

