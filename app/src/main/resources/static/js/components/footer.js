/*
  footer.js
  Dynamically renders the footer section across all pages.
*/

function renderFooter() {
  // 1. Select the footer element
  const footer = document.getElementById("footer");

  // 2. Insert footer HTML structure
  footer.innerHTML = `
    <footer class="footer">
      <div class="footer-container">

        <!-- Logo and Copyright -->
        <div class="footer-logo">
          <img src="../assets/images/logo/logo.png" alt="Hospital CMS Logo">
          <p>© Copyright 2025. All Rights Reserved by Hospital CMS.</p>
        </div>

        <!-- Links Section -->
        <div class="footer-links">
          
          <!-- Company Column -->
          <div class="footer-column">
            <h4>Company</h4>
            <a href="#">About</a>
            <a href="#">Careers</a>
            <a href="#">Press</a>
          </div>

          <!-- Support Column -->
          <div class="footer-column">
            <h4>Support</h4>
            <a href="#">Account</a>
            <a href="#">Help Center</a>
            <a href="#">Contact Us</a>
          </div>

          <!-- Legals Column -->
          <div class="footer-column">
            <h4>Legals</h4>
            <a href="#">Terms & Conditions</a>
            <a href="#">Privacy Policy</a>
            <a href="#">Licensing</a>
          </div>

        </div> <!-- End of footer-links -->

      </div> <!-- End of footer-container -->
    </footer>
  `;
}

// 3. Call the function to render footer on page load
document.addEventListener("DOMContentLoaded", renderFooter);

