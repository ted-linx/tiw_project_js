(() => {
    const ctx = window.APP_CONTEXT || '';
    const nav = document.getElementById('collaborator-monitor-nav');
    const empty = document.getElementById('collaborator-monitor-empty');
    const placeholder = document.getElementById('collaborator-monitor-placeholder');
    const details = document.getElementById('collaborator-monitor-details');

    if (!nav || !details) return;

    const state = {
        collaborators: [],
        selectedUsername: null,
        loaded: false
    };

    document.addEventListener('manager:viewchange', event => {
        if (event.detail?.view === 'collaborators' && !state.loaded) {
            loadCollaborators();
        }
    });

    async function loadCollaborators() {
        const data = await getJSON(`${ctx}/manager-home?action=collaboratorMonitoringList`);
        state.collaborators = data.collaborators || [];
        state.loaded = true;
        renderNav();
        if (state.collaborators.length) {
            await selectCollaborator(state.collaborators[0].username);
        }
    }

    async function selectCollaborator(username) {
        state.selectedUsername = username;
        renderNav();
        const data = await getJSON(`${ctx}/manager-home?action=collaboratorMonitoring&username=${encodeURIComponent(username)}`);
        renderDetails(normalizeCollaboratorData(data));
    }

    function renderNav() {
        const hasCollaborators = state.collaborators.length > 0;
        empty.hidden = hasCollaborators;
        nav.hidden = !hasCollaborators;
        nav.innerHTML = '';

        state.collaborators.forEach(collaborator => {
            const li = document.createElement('li');
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'project-nav-btn' + (collaborator.username === state.selectedUsername ? ' active' : '');
            button.innerHTML = `
                <span class="project-nav-title">${escapeHtml(collaborator.fullName || collaborator.username)}</span>
                <span class="project-nav-meta">${escapeHtml(collaborator.username)}</span>
            `;
            button.addEventListener('click', () => selectCollaborator(collaborator.username));
            li.appendChild(button);
            nav.appendChild(li);
        });
    }

    function renderDetails(data) {
        placeholder.hidden = true;
        details.hidden = false;

        const projects = data.projects || [];
        if (!projects.length) {
            details.innerHTML = `
                <div class="detail-title-bar">
                    <div>
                        <h2>${escapeHtml(data.fullName || data.username || 'Collaborator')}</h2>
                        <div class="detail-meta">
                            <span>Username: ${escapeHtml(data.username || '—')}</span>
                            <span>Projects involved: 0</span>
                        </div>
                    </div>
                </div>
                <div class="empty-state empty-state-inline">
                    <p>No activity found for this collaborator in your projects.</p>
                </div>
            `;
            return;
        }

        details.innerHTML = `
            <div class="detail-title-bar">
                <div>
                    <h2>${escapeHtml(data.fullName || data.username || 'Collaborator')}</h2>
                    <div class="detail-meta">
                        <span>Username: ${escapeHtml(data.username || '—')}</span>
                        <span>Projects involved: ${projects.length}</span>
                    </div>
                </div>
            </div>
            <div class="wp-list">
                ${projects.map(renderProjectCard).join('')}
            </div>
        `;
    }

    function renderProjectCard(project) {
        const wps = project.wps || [];
        return `
            <div class="wp-card">
                <div class="wp-header">
                    <div class="wp-header-left">
                        <span class="wp-badge">PRJ</span>
                        <div>
                            <span class="wp-title">${escapeHtml(project.title)}</span>
                            <span class="wp-interval">M1 – M${project.duration}</span>
                        </div>
                    </div>
                    <div class="wp-hours-summary">
                        <span class="status-badge badge-${escapeHtml(String(project.status || '').toLowerCase())}">${escapeHtml(project.status || '')}</span>
                    </div>
                </div>
                ${wps.length ? `<div class="wp-list" style="padding: var(--space-4);">${wps.map(renderWorkPackageCard).join('')}</div>` : `
                    <div class="empty-state empty-state-inline">
                        <p>No work packages found for this project.</p>
                    </div>
                `}
            </div>
        `;
    }

    function renderWorkPackageCard(wp) {
        const tasks = wp.tasks || [];
        return `
            <div class="wp-card">
                <div class="wp-header">
                    <div class="wp-header-left">
                        <span class="wp-badge">WP${escapeHtml(String(wp.order_number))}</span>
                        <div>
                            <span class="wp-title">${escapeHtml(wp.title)}</span>
                            <span class="wp-interval">M${wp.start_month} – M${wp.end_month}</span>
                        </div>
                    </div>
                    <div class="wp-hours-summary">
                        <span class="hours-label">Worked</span>
                        <span class="hours-value worked">${totalWorkedHours(wp)} h</span>
                    </div>
                </div>
                ${tasks.length ? `
                    <div class="task-table-scroll">
                        <table class="task-table monitor-task-table">
                            <thead>
                                <tr>
                                    <th scope="col">ID</th>
                                    <th scope="col">Title</th>
                                    <th scope="col">Interval</th>
                                    <th scope="col">Monthly worked hours</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${tasks.map(task => renderTaskRow(task, wp)).join('')}
                            </tbody>
                        </table>
                    </div>
                ` : `<div class="task-empty">No tasks available in this work package.</div>`}
            </div>
        `;
    }

    function renderTaskRow(task, wp) {
        return `
            <tr>
                <td class="task-id-cell">
                    <span class="task-id">T${escapeHtml(String(wp.order_number))}.${escapeHtml(String(task.order_number))}</span>
                </td>
                <td class="task-title-cell">
                    <div>${escapeHtml(task.title)}</div>
                    <div class="task-desc-cell">${escapeHtml(task.description || '—')}</div>
                </td>
                <td class="task-interval-cell">
                    M${task.start_month} – M${task.end_month}
                </td>
                <td class="task-month-breakdown">
                    <div class="month-compare-table" style="grid-template-columns: 64px 88px;">
                        <div class="month-compare-head">Month</div>
                        <div class="month-compare-head">Worked</div>
                        ${renderWorkedMonthCells(task)}
                    </div>
                </td>
            </tr>
        `;
    }

    function renderWorkedMonthCells(task) {
        const cells = [];
        for (let month = task.start_month; month <= task.end_month; month++) {
            cells.push(`
                <div class="month-cell-label">M${month}</div>
                <div class="month-cell-worked">${valueAt(task.worked_hours, month)} h</div>
            `);
        }
        return cells.join('');
    }

    function normalizeCollaboratorData(data) {
        const projects = (data.projects || data.collaboratingProjects || []).map(normalizeProject);
        return {
            username: data.username || data.selectedCollaborator?.username || '',
            fullName: data.fullName || data.selectedCollaborator?.fullName || data.username || '',
            projects
        };
    }

    function normalizeProject(project) {
        return {
            title: project.title || project.name || 'Untitled project',
            duration: numberOr(project.duration, 1),
            status: project.status || '',
            wps: (project.wps || project.workPackages || []).map((wp, index) => normalizeWorkPackage(wp, index))
        };
    }

    function normalizeWorkPackage(wp, index) {
        const normalized = {
            order_number: wp?.order_number ?? wp?.orderNumber ?? index + 1,
            title: wp?.title || 'Untitled work package',
            start_month: numberOr(wp?.start_month ?? wp?.startMonth, 1),
            end_month: numberOr(wp?.end_month ?? wp?.endMonth ?? wp?.start_month ?? wp?.startMonth, 1),
            worked_hours: wp?.worked_hours ?? wp?.workedHoursByMonth ?? wp?.workedHours ?? {},
            tasks: []
        };

        normalized.tasks = Array.isArray(wp?.tasks)
            ? wp.tasks.map((task, taskIndex) => normalizeTask(task, taskIndex, normalized))
            : [];

        return normalized;
    }

    function normalizeTask(task, index, wp) {
        return {
            order_number: task?.order_number ?? task?.orderNumber ?? index + 1,
            title: task?.title || 'Untitled task',
            description: task?.description || '',
            start_month: numberOr(task?.start_month ?? task?.startMonth ?? wp?.start_month, 1),
            end_month: numberOr(task?.end_month ?? task?.endMonth ?? wp?.end_month ?? wp?.start_month, 1),
            worked_hours: task?.worked_hours ?? task?.workedHoursByMonth ?? task?.workedHours ?? {}
        };
    }

    function totalWorkedHours(wp) {
        const source = wp.worked_hours || {};
        return Object.values(source).reduce((sum, value) => sum + Number(value || 0), 0);
    }

    function valueAt(map, month) {
        if (!map) return 0;
        return Number(map[month] ?? map[String(month)] ?? 0);
    }

    function numberOr(value, fallback) {
        const n = Number(value);
        return Number.isFinite(n) ? n : fallback;
    }

    async function getJSON(url) {
        const response = await fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } });
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        return response.json();
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
})();
