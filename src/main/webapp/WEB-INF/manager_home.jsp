<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Manager Home – Project Management</title>
  <script>
    window.APP_CONTEXT = '${pageContext.request.contextPath}'
    window.APP_USER = { fullName: '${user.fullName}'}
  </script>
  <script src="${pageContext.request.contextPath}/javascript/prova.js" defer></script>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager_home.css"/>
</head>
<body>
<header class="site-header">
  <div class="header-inner">
    <a aria-label="Project Management – Home" class="logo">
      <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M4 6.5C4 5.67 4.67 5 5.5 5h13c.83 0 1.5.67 1.5 1.5v11c0 .83-.67 1.5-1.5 1.5h-13C4.67 19 4 18.33 4 17.5v-11Z"
              stroke="currentColor" stroke-width="1.7"></path>
        <path d="M8 9h8M8 12h8M8 15h5"
              stroke="currentColor" stroke-width="1.7" stroke-linecap="round"></path>
      </svg>
      Project Management
    </a>

    <div class="header-user">
            <span class="greeting" id="greeting">
            </span>

      <a class="btn-logout" id="btn-logout">
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
    <p class="page-eyebrow">Manager area</p>
    <h1>Assign collaborators and planned hours</h1>
    <p class="page-lead">
      Select one of your projects, choose a work package and a task,
      then assign collaborators and planned hours month by month.
    </p>
  </div>
  <div class="alert alert-success" id="alert-success" hidden="hidden">
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor" stroke-width="2" viewbox="0 0 24 24" width="16">
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14">
      </path>
      <polyline points="22 4 12 14.01 9 11.01">
      </polyline>
    </svg>
    <span id="success-message">

    </span>
  </div>
  <div class="alert alert-error" role="alert" id="alert-error" hidden="hidden">
    <svg aria-hidden="true" fill="none" height="16" stroke="currentColor" stroke-width="2" viewbox="0 0 24 24" width="16">
      <circle cx="12" cy="12" r="10">
      </circle>
      <line x1="12" x2="12" y1="8" y2="12">
      </line>
      <line x1="12" x2="12.01" y1="16" y2="16">
      </line>
    </svg>
    <span id="error-message">

    </span>
  </div>
  <div class="manager-home-grid">
    <div class="form-card">
      <div class="form-card-header">
        <div aria-hidden="true" class="form-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M12 4.5 19 8v8l-7 3.5L5 16V8l7-3.5Z"
                  stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"></path>
            <path d="M9.5 11.5h5M9.5 14.5h3"
                  stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
          </svg>
        </div>
        <div>
          <h2>
            Task Assignment
          </h2>
          <p>Configure collaborators and planned monthly hours for a selected task.</p>
        </div>
      </div>
      <form method="post" id="assignment-form">
        <div class="field">
          <label for="assignment-project-id">Project</label>
          <div class="select-wrapper">
            <select name="project_id" id="assignment-project-id" required>

            </select>
          </div>
          <span class="field-error" id="project-id-error" hidden="hidden">
                        </span>
        </div>
        <div class="field">
          <label for="assignment-wp-id">Work Package</label>
          <div class="select-wrapper">
            <select name="wp_id" id="assignment-wp-id" required>
            </select>
          </div>
          <span class="field-error" id="wp-id-error" hidden="hidden">
                        </span>
        </div>
        <div class="field">
          <label for="assignment-task-id">Task</label>
          <div class="select-wrapper">
            <select name="task_id" id="assignment-task-id" required>
            </select>
          </div>
          <span class="field-error" id="task-id-error" hidden="hidden">
                        </span>
        </div>
        <div class="field">
          <label for="assignment-collaborator">Collaborator</label>
          <div class="select-wrapper">
            <select name="collaborator" id="assignment-collaborator" required>
            </select>
          </div>
          <span class="field-error" id="collaborator-error" hidden="hidden">
                        </span>
        </div>
        <div class="planned-hours-block">
          <div class="planned-hours-header">
            <h3>Planned hours by month</h3>
            <p id="planned-hours-caption">
            </p>
          </div>

          <div class="months-grid" id="months-grid">
          </div>
        </div>

        <button class="btn-primary" type="submit" id="btn-submit">
          <svg aria-hidden="true" fill="none" height="15" stroke="currentColor" stroke-width="2" viewbox="0 0 24 24" width="15">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z">
            </path>
            <polyline points="17 21 17 13 7 13 7 21">
            </polyline>
            <polyline points="7 3 7 8 15 8">
            </polyline>
          </svg>
          Save Assignment
        </button>
      </form>

      <!--/* ASSEGNA button – only shown when a CREATED project is selected */-->
      <div class="section-divider"
           id="assign-project-section"
           style="padding-top: var(--space-5); margin-top: var(--space-2);">
        <p class="field-hint">
          Once all tasks have collaborators and planned hours,
          use the button below to move the project to <strong>ASSIGNED</strong> state.
        </p>
        <form method="post" id="assign-project-form" style="margin-top: var(--space-3);">
          <input type="hidden" name="project_id" id="assign-project-id"/>
          <button class="btn-primary" type="submit">
            <svg aria-hidden="true" fill="none" height="15" stroke="currentColor"
                 stroke-width="2" viewBox="0 0 24 24" width="15">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            Assign Project
          </button>
        </form>
      </div>
    </div>
<%--    <aside>--%>
<%--      <section class="form-card">--%>
<%--        <div class="form-card-header">--%>
<%--          <div class="form-icon" aria-hidden="true">--%>
<%--            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">--%>
<%--              <path d="M5 12h14M12 5v14"--%>
<%--                    stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>--%>
<%--            </svg>--%>
<%--          </div>--%>
<%--          <div>--%>
<%--            <h2>Project actions</h2>--%>
<%--            <p>Reach the pages dedicated to project and collaborator monitoring.</p>--%>
<%--          </div>--%>
<%--        </div>--%>

<%--        <div class="action-links">--%>
<%--          <a class="action-link-card" th:href="@{/monitor-projects}">--%>
<%--            <span class="action-link-title">Monitor projects</span>--%>
<%--            <span class="action-link-copy">Check planned and worked hours for projects, work packages and tasks.</span>--%>
<%--          </a>--%>

<%--          <a class="action-link-card" th:href="@{/monitor-collaborators}">--%>
<%--            <span class="action-link-title">Monitor collaborators</span>--%>
<%--            <span class="action-link-copy">Inspect collaborators’ worked hours limited to your managed projects.</span>--%>
<%--          </a>--%>
<%--        </div>--%>
<%--      </section>--%>
<%--    </aside>--%>
  </div>
</main>
</body>
</html>

