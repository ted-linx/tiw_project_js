/**
 * utils.js — shared utilities across all pages
 *
 * Exports:
 *  - showSuccess(message)
 *  - showError(message)
 *  - clearSuccess()
 *  - clearError()
 *  - clearMessages()
 *  - initGreeting()
 *  - initLogout(ctx)
 *  - apiFetch(url, options) → { ok, data }
 */

/**
 * Shows the success alert banner.
 * Expects #alert-success and #success-message in the DOM.
 */
export function showSuccess(message) {
    const alert = document.getElementById('alert-success');
    const msg   = document.getElementById('success-message');
    if (!alert || !msg) return;
    msg.textContent = message;
    alert.removeAttribute('hidden');
    alert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/**
 * Shows the error alert banner and hides success.
 * Expects #alert-error and #error-message in the DOM.
 */
export function showError(message) {
    const alertErr  = document.getElementById('alert-error');
    const msgErr    = document.getElementById('error-message');
    const alertSucc = document.getElementById('alert-success');
    if (msgErr)    msgErr.textContent = message;
    if (alertErr)  alertErr.removeAttribute('hidden');
    if (alertSucc) alertSucc.setAttribute('hidden', 'hidden');
    if (alertErr)  alertErr.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/**
 * Hides and clears the success alert banner.
 */
export function clearSuccess() {
    const alert = document.getElementById('alert-success');
    const msg   = document.getElementById('success-message');
    if (alert) alert.setAttribute('hidden', 'hidden');
    if (msg)   msg.textContent = '';
}

/**
 * Hides and clears the error alert banner.
 */
export function clearError() {
    const alert = document.getElementById('alert-error');
    const msg   = document.getElementById('error-message');
    if (alert) alert.setAttribute('hidden', 'hidden');
    if (msg)   msg.textContent = '';
}

/**
 * Clears both success and error banners at once.
 * Call this at the start of any async operation or on view change.
 */
export function clearMessages() {
    clearSuccess();
    clearError();
}

/**
 * Initialises the greeting element with the logged-in user's full name.
 * Expects #greeting in the DOM and window.APP_USER.fullName.
 */
export function initGreeting() {
    const greeting = document.getElementById('greeting');
    if (greeting && window.APP_USER?.fullName) {
        greeting.innerHTML = `Hello, <strong>${window.APP_USER.fullName}</strong>`;
    }
}

/**
 * Wires up the logout button (#btn-logout) to redirect to /logout.
 * @param {string} ctx  — window.APP_CONTEXT value
 */
export function initLogout(ctx) {
    const btn = document.getElementById('btn-logout');
    if (btn) {
        btn.addEventListener('click', () => {
            window.location.href = `${ctx}/logout`;
        });
    }
}
