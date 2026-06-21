<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Login – Project Management</title>

  <link rel="stylesheet"/>
  <script>
    window.APP_CONTEXT = '${pageContext.request.contextPath}';
  </script>
  <script type="module" src="${pageContext.request.contextPath}/javascript/login.js" defer></script>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/login.css"/>
</head>
<body>
  <main class="login-card">

    <!-- Brand mark -->
    <div class="brand">
      <div class="brand-icon" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="1.8">
          <rect x="2" y="3" width="20" height="14" rx="2"/>
          <path d="M8 21h8M12 17v4M7 8h10M7 12h6"/>
        </svg>
      </div>
      <div class="brand-name">
        Project Management
        <span>TIW 2025–26</span>
      </div>
    </div>

    <h1>Sign in</h1>
    <p class="subtitle">Enter your credentials to access the application.</p>

    <!-- Errore login (gestito da login.js) -->
    <div id="alert-error" class="alert-error" role="alert" hidden="hidden">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
           stroke="currentColor" stroke-width="2" aria-hidden="true">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      <span id="error-message"></span>
    </div>

    <form id="login-form">

      <div class="field">
        <label for="username">Username</label>
        <input id="username" name="username" type="text"
               required maxlength="45" autocomplete="username"
               placeholder="e.g. mario.rossi"/>
      </div>

      <div class="field">
        <label for="password">Password</label>
        <input id="password" name="password" type="password"
               required maxlength="45" autocomplete="current-password"
               placeholder="••••••••"/>
      </div>

      <button type="submit" class="btn-primary">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
          <polyline points="10 17 15 12 10 7"/>
          <line x1="15" y1="12" x2="3" y2="12"/>
        </svg>
        Sign in
      </button>
    </form>

    <p class="hint">Created by Matteo d'Amato.</p>
  </main>
</body>
</html>
