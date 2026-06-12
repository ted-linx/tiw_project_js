<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Collaborator Home – Project Management</title>

  <script>
    window.APP_CONTEXT = '${pageContext.request.contextPath}';
    window.APP_USER = { fullName: '${user.fullName}', username: '${user.username}' };
  </script>

  <script type="module" src="${pageContext.request.contextPath}/javascript/assignee_home.js" defer></script>

  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/verify.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager_home.css"/>
</head>
<body>
<header class="site-header">
  <div class="header-inner">
    <a class="logo" aria-label="Project Management – Home">
      <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M4 6.5C4 5.67 4.67 5 5.5 5h13c.83 0 1.5.67 1.5 1.5v11c0 .83-.67 1.5-1.5 1.5h-13C4.67 19 4 18.33 4 17.5v-11Z" stroke="currentColor" stroke-width="1.7"></path>
        <path d="M8 9h8M8 12h8M8 15h5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"></path>
      </svg>
      Project Management
    </a>

    <div class="header-user">
      <span class="greeting" id="greeting"></span>
      <a class="btn-logout" id="btn-logout" href="${pageContext.request.contextPath}/logout">
        <svg aria-hidden="true" fill="none" width="15" height="15" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
          <polyline points="16 17 21 12 16 7"></polyline>
          <line x1="21" y1="12" x2="9" y2="12"></line>
        </svg>
        Logout
      </a>
    </div>
  </div>
</header>

<main class="main-content">
  <div class="page-heading">
    <p class="page-eyebrow">Collaborator area</p>
    <h1>Report worked hours</h1>
    <p>Select a project and edit your worked hours directly inside the table.</p>
  </div>

  <div class="alert alert-success" id="alert-success" hidden="hidden">
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" width="16">
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
      <polyline points="22 4 12 14.01 9 11.01"></polyline>
    </svg>
    <span id="success-message"></span>
  </div>

  <div class="alert alert-error" role="alert" id="alert-error" hidden="hidden" tabindex="-1">
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" width="16">
      <circle cx="12" cy="12" r="10"></circle>
      <line x1="12" x2="12" y1="8" y2="12"></line>
      <line x1="12" x2="12.01" y1="16" y2="16"></line>
    </svg>
    <span id="error-message"></span>
  </div>

  <div class="verify-layout">
    <aside class="project-list-panel">
      <div class="panel-header">
        <svg aria-hidden="true" fill="none" height="14" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" width="14">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
        </svg>
        My Projects
      </div>

      <div class="empty-state" id="empty-state-projects" hidden="hidden">
        <svg aria-hidden="true" fill="none" height="32" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24" width="32">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
        </svg>
        <p>You are not assigned to any project yet.</p>
      </div>

      <ul class="project-nav" id="project-nav"></ul>
    </aside>

    <section class="project-detail-panel">
      <div class="detail-placeholder" id="detail-placeholder">
        <svg aria-hidden="true" fill="none" height="48" stroke="currentColor" stroke-width="1.2" viewBox="0 0 24 24" width="48">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" x2="16.65" y1="21" y2="16.65"></line>
        </svg>
        <p>Select a project from the left to view your assigned work packages and tasks.</p>
      </div>

      <div id="project-details" hidden="hidden"></div>
    </section>
  </div>
</main>
</body>
</html>
