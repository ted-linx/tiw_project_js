import {initGreeting, initLogout} from './utils.js'
(() => {
    const ctx = window.APP_CONTEXT || '';

    initGreeting();
    initLogout(ctx);
})();