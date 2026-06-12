import { showError, clearError } from './utils.js';

(function () {
  const ctx = window.APP_CONTEXT || '';

  const form = document.getElementById('login-form');
  const submitBtn = form.querySelector('button[type="submit"]');

  function setLoading(loading) {
    submitBtn.disabled = loading;
    submitBtn.textContent = loading ? 'Signing in…' : 'Sign in';
  }

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    clearError();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!username || !password) {
      showError('Please fill in both username and password.');
      return;
    }

    setLoading(true);

    try {
      const res = await fetch(`${ctx}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ username, password }),
      });

      const data = await res.json();

      if (!res.ok) {
        showError(data.error ?? 'Login failed. Please try again.');
        return;
      }

      const target = data.role === 'ADMINISTRATIVE'
          ? `${ctx}/admin-home`
          : `${ctx}/technical-home`;

      window.location.href = target;

    } catch (err) {
      showError('Network error. Please check your connection and try again.');
    } finally {
      setLoading(false);
    }
  });
})();
