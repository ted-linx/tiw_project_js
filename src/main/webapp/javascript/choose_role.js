import { initGreeting, initLogout } from './utils.js';

(function () {
  const ctx = window.APP_CONTEXT || '';

  // ── DOM refs ──────────────────────────────────────────────────────────────
  const btnManager     = document.getElementById('btn-manager');
  const btnCollaborator = document.getElementById('btn-collaborator');

  // ── Populate user greeting from JSP-injected data ────────────────────────
  initGreeting();

  btnManager?.addEventListener('click', () => {
    window.location.href = `${ctx}/manager-home`;
  })

  btnCollaborator?.addEventListener('click', () => {
    window.location.href = `${ctx}/assignee-home`;
  })

  // ── Logout ────────────────────────────────────────────────────────────────
  initLogout(ctx);

})();
