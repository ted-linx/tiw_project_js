<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Choose Role – Project Management</title>
    <script>
        window.APP_CONTEXT = '${pageContext.request.contextPath}';
        window.APP_USER = { fullName: '${user.fullName}' };
    </script>
    <script type="module" src="${pageContext.request.contextPath}/javascript/choose_role.js" defer></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/choose_role.css"/>
</head>
<body>
<header class="site-header">
    <div class="header-inner">
        <a class="logo" href="${pageContext.request.contextPath}/login">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M4 6.5C4 5.67 4.67 5 5.5 5h13c.83 0 1.5.67 1.5 1.5v11c0 .83-.67 1.5-1.5 1.5h-13C4.67 19 4 18.33 4 17.5v-11Z"
                      stroke="currentColor" stroke-width="1.7"></path>
                <path d="M8 9h8M8 12h8M8 15h5"
                      stroke="currentColor" stroke-width="1.7" stroke-linecap="round"></path>
            </svg>
            <span>Project Management</span>
        </a>

        <div class="header-user">
            <span class="greeting" id="greeting">
            </span>

            <button id="btn-logout" class="btn-logout">
                <svg aria-hidden="true" fill="none" width="15" height="15" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                    <polyline points="16 17 21 12 16 7"></polyline>
                    <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
                Logout
            </button>
        </div>
    </div>
</header>

<main class="main-content choose-role-page">
    <section class="choose-role-shell">
        <div class="choose-role-intro">
            <p class="choose-role-eyebrow">Access mode</p>
            <h1>Choose how to enter the application</h1>
            <p class="choose-role-lead">
                Your profile can operate both as project manager and collaborator.
                Select the mode you want to use for this session.
            </p>
        </div>

        <div class="choose-role-grid">
            <div class="role-card role-card--manager">
                <div class="role-card-icon" aria-hidden="true">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                        <path d="M12 4.5 19 8v8l-7 3.5L5 16V8l7-3.5Z"
                              stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"></path>
                        <path d="M9.5 11.5h5M9.5 14.5h3"
                              stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
                    </svg>
                </div>

                <div class="role-card-copy">
                    <h2>Manager</h2>
                    <p>
                        Access project monitoring, work package progress and coordination-related views.
                    </p>
                </div>

                <button id="btn-manager" class="btn-primary role-card-action">
                    Enter as manager
                </button>
            </div>

            <div class="role-card role-card--collaborator">
                <div class="role-card-icon" aria-hidden="true">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                        <path d="M12 12a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"
                              stroke="currentColor" stroke-width="1.8"></path>
                        <path d="M6.5 19a5.5 5.5 0 0 1 11 0"
                              stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
                    </svg>
                </div>

                <div class="role-card-copy">
                    <h2>Collaborator</h2>
                    <p>
                        Access assigned activities, personal workload and task execution information.
                    </p>
                </div>

                <button id="btn-collaborator" class="btn-primary role-card-action">
                    Enter as collaborator
                </button>
            </div>
        </div>
    </section>
</main>
</body>
</html>
