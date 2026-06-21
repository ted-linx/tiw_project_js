export function showSuccess(message) {
    const alert = document.getElementById('alert-success');
    const msg   = document.getElementById('success-message');
    if (!alert || !msg) return;
    msg.textContent = message;
    alert.removeAttribute('hidden');
    alert.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

export function showError(message) {
    const alertErr  = document.getElementById('alert-error');
    const msgErr    = document.getElementById('error-message');
    const alertSucc = document.getElementById('alert-success');
    if (msgErr)    msgErr.textContent = message;
    if (alertErr)  alertErr.removeAttribute('hidden');
    if (alertSucc) alertSucc.setAttribute('hidden', 'hidden');
    if (alertErr)  alertErr.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

export function clearSuccess() {
    const alert = document.getElementById('alert-success');
    const msg   = document.getElementById('success-message');
    if (alert) alert.setAttribute('hidden', 'hidden');
    if (msg)   msg.textContent = '';
}

export function clearError() {
    const alert = document.getElementById('alert-error');
    const msg   = document.getElementById('error-message');
    if (alert) alert.setAttribute('hidden', 'hidden');
    if (msg)   msg.textContent = '';
}

export function clearMessages() {
    clearSuccess();
    clearError();
}

export function initGreeting() {
    const greeting = document.getElementById('greeting');
    if (greeting && window.APP_USER?.fullName) {
        greeting.innerHTML = `Hello, <strong>${window.APP_USER.fullName}</strong>`;
    }
}

export function initLogout(ctx) {
    const btn = document.getElementById('btn-logout');
    if (btn) {
        btn.addEventListener('click', () => {
            window.location.href = `${ctx}/logout`;
        });
    }
}
