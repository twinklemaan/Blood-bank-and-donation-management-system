// main.js - DOM Manipulation, Mobile Menu & Live Stock Fetch (Unit 3: DOM, Event Handling)

document.addEventListener('DOMContentLoaded', () => {
  // 1. Mobile Hamburger Menu Toggle
  const menuToggle = document.getElementById('menuToggle');
  const navLinks = document.getElementById('navLinks');

  if (menuToggle && navLinks) {
    menuToggle.addEventListener('click', () => {
      navLinks.classList.toggle('active');
    });
  }

  // 2. Live Stock Section: Fetches public stock and renders cards using secure DOM methods
  const stockContainer = document.getElementById('liveStockContainer');
  if (stockContainer) {
    fetchStockData(stockContainer);
  }
});

/**
 * Fetches public blood stock and populates the container using createElement & textContent
 * SYLLABUS REQUIREMENT: Never use innerHTML with untrusted/server data to prevent XSS.
 */
function fetchStockData(container) {
  const API_URL = 'http://localhost:5000/api/public/stock';

  fetch(API_URL)
    .then((response) => {
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      return response.json();
    })
    .then((stockList) => {
      // Clear placeholder
      container.textContent = '';

      if (!Array.isArray(stockList) || stockList.length === 0) {
        const emptyMsg = document.createElement('p');
        emptyMsg.textContent = 'No live stock data currently available.';
        container.appendChild(emptyMsg);
        return;
      }

      // Build cards safely using createElement and textContent
      stockList.forEach((item) => {
        const card = document.createElement('div');
        card.className = 'stock-card';
        if (item.units <= 5) {
          card.classList.add('low');
        }

        const groupEl = document.createElement('div');
        groupEl.className = 'stock-group';
        groupEl.textContent = item.bloodGroup;

        const unitsEl = document.createElement('div');
        unitsEl.className = 'stock-units';
        unitsEl.textContent = item.units;

        const labelEl = document.createElement('div');
        labelEl.className = 'stock-label';
        labelEl.textContent = 'Units';

        card.appendChild(groupEl);
        card.appendChild(unitsEl);
        card.appendChild(labelEl);

        container.appendChild(card);
      });
    })
    .catch((err) => {
      container.textContent = '';
      const errorMsg = document.createElement('p');
      errorMsg.textContent = 'API currently offline or connecting to live server...';
      errorMsg.style.color = '#7f8c8d';
      container.appendChild(errorMsg);
    });
}
