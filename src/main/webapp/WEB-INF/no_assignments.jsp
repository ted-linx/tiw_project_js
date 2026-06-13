<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>No Assignments – Project Management</title>

    <script>
        window.APP_CONTEXT = '${pageContext.request.contextPath}'
        window.APP_USER = {
            username: '${user.username}',
            fullName: '${user.fullName}'
        }
    </script>
    <script type="module" src="${pageContext.request.contextPath}/javascript/no_assignments.js"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"/>
</head>
<body>
<header class="site-header">
    <div class="header-inner">
        <a class="logo">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M4 6.5C4 5.67 4.67 5 5.5 5h13c.83 0 1.5.67 1.5 1.5v11c0 .83-.67 1.5-1.5 1.5h-13C4.67 19 4 18.33 4 17.5v-11Z"
                      stroke="currentColor" stroke-width="1.7"/>
                <path d="M8 9h8M8 12h8M8 15h5"
                      stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
            </svg>
            <span>Project Management</span>
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

<main class="main-content message-page">
    <div class="form-card">
        <div class="form-card-header">
            <div aria-hidden="true" class="form-icon">
                <svg stroke="currentColor" width="18" height="18" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg" fill="none">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.12 4.623a1 1 0 011.76 0l11.32 20.9A1 1 0 0127.321 27H4.679a1 1 0 01-.88-1.476l11.322-20.9zM16 18v-6"></path>
                    <path d="M17.5 22.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z"></path>
                </svg>
            </div>
            <div>
                <h2>
                    No active assignments
                </h2>
                <p>
                    Your account is active, but there are currently no assigned tasks and no managed projects associated with your profile.
                </p>
                <p>
                    As soon as a task or project becomes available, the appropriate home page will be accessible at the next login.
                </p>
            </div>
        </div>
    </div>
</main>
</body>
</html>

