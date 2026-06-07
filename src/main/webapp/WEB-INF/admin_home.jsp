<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="utf-8"/>
  <meta content="width=device-width, initial-scale=1.0" name="viewport"/>
  <title>Project Verification – Project Management</title>
  <script>
    window.APP_CONTEXT = '${pageContext.request.contextPath}'
  </script>
  <script src="${pageContext.request.contextPath}/javascript/admin-home.js" defer></script>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/verify.css"/>
</head>
<body>

<!-- ══════════ HEADER ══════════ -->
<header class="site-header">
  <div class="header-inner">
    <a class="logo admin-home-btn" aria-label="Project Management – Home">
      <svg aria-hidden="true" fill="none" height="26" stroke="currentColor"
           stroke-width="1.8" viewBox="0 0 24 24" width="26">
        <rect height="14" rx="2" width="20" x="2" y="3"/>
        <path d="M8 21h8M12 17v4"/>
        <path d="M7 8h10M7 12h6"/>
      </svg>
      Project Management
    </a>
    <div class="header-user">
        <span class="greeting" id="greeting">
<%--          Welcome user--%>
        </span>
      <a class="btn-icon-link admin-home-btn" id="admin-home-btn">
        <svg aria-hidden="true" fill="none" height="15" stroke="currentColor"
             stroke-width="2" viewBox="0 0 24 24" width="15">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
          <polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        Home
      </a>
      <a class="btn-logout" id="btn-logout">
        <svg aria-hidden="true" fill="none" height="15" stroke="currentColor"
             stroke-width="2" viewBox="0 0 24 24" width="15">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
          <polyline points="16 17 21 12 16 7"/>
          <line x1="21" x2="9" y1="12" y2="12"/>
        </svg>
        Logout
      </a>
    </div>
  </div>
</header>

<!-- ══════════ MAIN ══════════ -->
<main class="main-content" id="main">

  <div class="page-heading">
    <h1>Project Verification</h1>
    <p>Select a project to inspect its WP and task hierarchy.</p>
  </div>

  <!-- Feedback -->
  <div class="alert alert-success" id="alert-success" role="status" hidden="hidden">
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor"
         stroke-width="2" viewBox="0 0 24 24" width="16">
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
      <polyline points="22 4 12 14.01 9 11.01"/>
    </svg>
    <span id="success-message"></span>
  </div>
  <div class="alert alert-error" id="alert-error" role="alert" hidden="hidden">
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor"
         stroke-width="2" viewBox="0 0 24 24" width="16">
      <circle cx="12" cy="12" r="10"/>
      <line x1="12" x2="12" y1="8" y2="12"/>
      <line x1="12" x2="12.01" y1="16" y2="16"/>
    </svg>
    <span id="error-message"></span>
  </div>

  <!-- ══════════ LAYOUT ══════════ -->
  <div class="verify-layout">

    <!-- ── LEFT: project list ── -->
    <aside class="project-list-panel">
      <div class="panel-header">
        <svg aria-hidden="true" fill="none" height="14" stroke="currentColor"
             stroke-width="2" viewBox="0 0 24 24" width="14">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
        </svg>
        My Projects
      </div>

      <!-- Empty state -->
      <div class="empty-state" id="empty-state-projects">
        <svg aria-hidden="true" fill="none" height="32" stroke="currentColor"
             stroke-width="1.5" viewBox="0 0 24 24" width="32">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
        </svg>
        <p>No projects yet. <a class="admin-home-btn" href="">Create one →</a></p>
      </div>

      <!-- Project selection: one GET form per project -->
      <ul class="project-nav" id="project-nav" hidden="hidden">
      </ul>
    </aside>

    <!-- ── RIGHT: project detail ── -->
    <section class="project-detail-panel">

      <!-- Placeholder -->
      <div class="detail-placeholder" id="detail-placeholder" hidden="">
        <svg aria-hidden="true" fill="none" height="48" stroke="currentColor"
             stroke-width="1.2" viewBox="0 0 24 24" width="48">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" x2="16.65" y1="21" y2="16.65"/>
        </svg>
        <p>Select a project from the left to view its structure.</p>
      </div>

      <!-- Detail -->
      <div id="project-details" hidden="hidden">
      </div>
      <!-- /selectedProject -->

    </section>
  </div>
  <!-- /verify-layout -->

</main>
</body>
</html>
