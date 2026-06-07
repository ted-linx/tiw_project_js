'use strict';

(function () {

  // ── DOM refs ──────────────────────────────────────────────
  const form= document.getElementById('login-form');
  const alertError= document.getElementById('alert-error');
  const errorMsg= document.getElementById('error-message');
  const submitBtn= form.querySelector('button[type="submit"]');

  // ── Helpers ───────────────────────────────────────────────
  function showError(message) {
    errorMsg.textContent = message;
    alertError.removeAttribute('hidden');
    alertError.focus();
  }

  function clearError() {
    alertError.setAttribute('hidden', '');
    errorMsg.textContent = '';
  }

  function setLoading(loading) {
    submitBtn.disabled = loading;
    submitBtn.textContent = loading ? 'Signing in…' : 'Sign in';
  }

  // ── Submit handler ────────────────────────────────────────
  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    clearError();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    // Client-side validation
    if (!username || !password) {
      showError('Please fill in both username and password.');
      return;
    }

    setLoading(true);

    try {
      const res = await fetch('/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ username, password }),
      });

      const data = await res.json();

      if (!res.ok) {
        showError(data.error ?? 'Login failed. Please try again.');
        return;
      }

      // Redirect in base al ruolo restituito dal server
      const target = data.role === 'ADMINISTRATIVE' ? '/admin-home' : '/technical-home';
      window.location.href = target;

    } catch (err) {
      showError('Network error. Please check your connection and try again.');
    } finally {
      setLoading(false);
    }
  });

})();
