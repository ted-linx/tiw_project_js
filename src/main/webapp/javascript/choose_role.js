(function () {
  const ctx = window.APP_CONTEXT || '';

  // ── DOM refs ──────────────────────────────────────────────────────────────
  const greeting   = document.getElementById('greeting');
  const logoutBtn      = document.getElementById('btn-logout');
  const btnManager     = document.getElementById('btn-manager');
  const btnCollaborator = document.getElementById('btn-collaborator');

  // ── Populate user greeting from JSP-injected data ────────────────────────
  if (greeting && window.APP_USER?.fullName) {
    greeting.innerHTML = `Hello, <strong>${window.APP_USER.fullName}</strong>`;
  }

  btnManager?.addEventListener('click', () => {
    window.location.href = `${ctx}/manager-home`;
  })

  btnCollaborator?.addEventListener('click', () => {
    window.location.href = `${ctx}/assignee-home`;
  })

  // ── Logout ────────────────────────────────────────────────────────────────
  logoutBtn?.addEventListener('click', () => {
    window.location.href = `${ctx}/logout`;
  });

})();
