/* header.js
 * Renders the top header bar depending on current page, role and session.
 * Exposes: renderHeader(), logout(), logoutPatient()
 * Safe to include on every page (no hard dependencies).
 */

(function () {
  /** Utility: safe global function call if it exists */
  const tryCall = (fnName, ...args) => {
    const fn = window[fnName];
    if (typeof fn === "function") return fn(...args);
    // no-op if not present
  };

  /** Main render function */
  window.renderHeader = function renderHeader() {
    const headerDiv = document.getElementById("header");
    if (!headerDiv) return;

    const path = window.location.pathname || "";
    const onRoot =
      path.endsWith("/") ||
      path.endsWith("/index") ||
      path.endsWith("/index.html");

    // On the root (landing) page we reset the role for a clean start
    if (onRoot) {
      localStorage.removeItem("userRole");
      localStorage.removeItem("token");
      headerDiv.innerHTML = `
        <header class="header">
          <div class="logo-section">
            <img src="../assets/images/logo/logo.png" alt="Hospital CMS Logo" class="logo-img">
            <span class="logo-title">Hospital CMS</span>
          </div>
        </header>`;
      return;
    }

    const role = localStorage.getItem("userRole"); // 'admin' | 'doctor' | 'patient' | 'loggedPatient'
    const token = localStorage.getItem("token");

    // If a privileged role is present but token is missing — treat as expired
    if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
      localStorage.removeItem("userRole");
      alert("Session expired or invalid login. Please log in again.");
      window.location.href = "/";
      return;
    }

    let headerContent = `
      <header class="header">
        <div class="logo-section">
          <img src="../assets/images/logo/logo.png" alt="Hospital CMS Logo" class="logo-img">
          <span class="logo-title">Hospital CMS</span>
        </div>
        <nav class="header-actions">`;

    // Role specific actions
    if (role === "admin") {
      headerContent += `
        <button id="addDocBtn" class="adminBtn" type="button">Add Doctor</button>
        <a href="#" id="logoutLink">Logout</a>`;
    } else if (role === "doctor") {
      headerContent += `
        <button id="doctorHomeBtn" class="adminBtn" type="button">Home</button>
        <a href="#" id="logoutLink">Logout</a>`;
    } else if (role === "loggedPatient") {
      headerContent += `
        <button id="homeBtn" class="adminBtn" type="button">Home</button>
        <button id="apptBtn" class="adminBtn" type="button">Appointments</button>
        <a href="#" id="logoutPatientLink">Logout</a>`;
    } else {
      // Visitor / unauthenticated patient
      headerContent += `
        <button id="patientLogin" class="adminBtn" type="button">Login</button>
        <button id="patientSignup" class="adminBtn" type="button">Sign Up</button>`;
    }

    headerContent += `</nav></header>`;

    headerDiv.innerHTML = headerContent;
    attachHeaderButtonListeners();
  };

  /** Wire up dynamic buttons after render */
  function attachHeaderButtonListeners() {
    // Admin: Add Doctor -> open modal if available
    const addDocBtn = document.getElementById("addDocBtn");
    if (addDocBtn) {
      addDocBtn.addEventListener("click", () => {
        // openModal('addDoctor') if defined, otherwise navigate to a fallback page
        if (typeof window.openModal === "function") {
          window.openModal("addDoctor");
        } else {
          // Fallback to a page if your app uses dedicated route
          window.location.href = "/pages/admin-add-doctor.html";
        }
      });
    }

    // Doctor: Home
    const doctorHomeBtn = document.getElementById("doctorHomeBtn");
    if (doctorHomeBtn) {
      doctorHomeBtn.addEventListener("click", () => {
        // call selectRole('doctor') if present, otherwise go to doctor dashboard
        if (typeof window.selectRole === "function") {
          window.selectRole("doctor");
        } else {
          window.location.href = "/pages/doctorDashboard.html";
        }
      });
    }

    // Logged Patient: Home & Appointments
    const homeBtn = document.getElementById("homeBtn");
    if (homeBtn) {
      homeBtn.addEventListener("click", () => {
        window.location.href = "/pages/loggedPatientDashboard.html";
      });
    }
    const apptBtn = document.getElementById("apptBtn");
    if (apptBtn) {
      apptBtn.addEventListener("click", () => {
        window.location.href = "/pages/patientAppointments.html";
      });
    }

    // Patient (unauthenticated): Login / Sign Up
    const loginBtn = document.getElementById("patientLogin");
    if (loginBtn) {
      loginBtn.addEventListener("click", () => {
        if (typeof window.openModal === "function") {
          window.openModal("patientLogin");
        } else {
          window.location.href = "/pages/patient-login.html";
        }
      });
    }
    const signupBtn = document.getElementById("patientSignup");
    if (signupBtn) {
      signupBtn.addEventListener("click", () => {
        if (typeof window.openModal === "function") {
          window.openModal("patientSignup");
        } else {
          window.location.href = "/pages/patient-signup.html";
        }
      });
    }

    // Logout links
    const logoutLink = document.getElementById("logoutLink");
    if (logoutLink) logoutLink.addEventListener("click", (e) => { e.preventDefault(); logout(); });

    const logoutPatientLink = document.getElementById("logoutPatientLink");
    if (logoutPatientLink) logoutPatientLink.addEventListener("click", (e) => { e.preventDefault(); logoutPatient(); });
  }

  /** Admin/Doctor generic logout */
  window.logout = function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    window.location.href = "/";
  };

  /** Patient-specific logout */
  window.logoutPatient = function logoutPatient() {
    localStorage.removeItem("token");
    localStorage.setItem("userRole", "patient"); // keep patient context if you want to show patient login/sign-up
    window.location.href = "/";
  };

  // Auto render on load
  document.addEventListener("DOMContentLoaded", () => {
    try {
      window.renderHeader();
    } catch {
      // swallow render errors to avoid breaking the page
    }
  });
})();

