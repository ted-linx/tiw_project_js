import { clearMessages } from './utils.js';

(() => {
    const ctx = window.APP_CONTEXT || '';
    const nav = document.getElementById('project-monitor-nav');
    const empty = document.getElementById('project-monitor-empty');
    const placeholder = document.getElementById('project-monitor-placeholder');
    const details = document.getElementById('project-monitor-details');

    if (!nav || !details) return;

    const state = {
        projects: [],
        selectedId: null,
        loaded: false
    };

    document.addEventListener('manager:viewchange', event => {
        clearMessages();
        if (event.detail?.view === 'projects' && !state.loaded) {
            loadProjects();
        }
    });

    document.addEventListener('manager:save-assignment', event => {
        state.loaded = false;
    })

    async function loadProjects() {
        const data = await getJSON(`${ctx}/manager-home?action=projectMonitoringList`);
        state.projects = (data.projects || []).map((project, index) => normalizeProject(project, index));
        state.loaded = true;
        renderNav();
        if (state.projects.length) {
            await selectProject(state.projects[0].id);
        }
    }

    async function selectProject(projectId) {
        state.selectedId = projectId;
        renderNav();
        const data = await getJSON(`${ctx}/manager-home?action=projectMonitoring&project_id=${encodeURIComponent(projectId)}`);
        renderDetails({
            ...data,
            project: normalizeProject(data.project || {}, 0),
            wps: (data.wps || data.project?.workPackages || []).map((wp, index) => normalizeWorkPackage(wp, index))
        });
    }

    function normalizeProject(project, index) {
        return {
            ...project,
            id: project?.id ?? index,
            title: project?.title ?? 'Untitled project',
            duration: numberOr(project?.duration, 0),
            manager: project?.manager ?? project?.projectManager ?? '',
            status: project?.status ?? 'CREATED'
        };
    }

    function normalizeWorkPackage(wp, index) {
        const normalized = {
            ...wp,
            order_number: wp?.order_number ?? wp?.orderNumber ?? index + 1,
            title: wp?.title ?? 'Untitled WP',
            start_month: numberOr(wp?.start_month ?? wp?.startMonth, 1),
            end_month: numberOr(wp?.end_month ?? wp?.endMonth, 1),
            totalPlannedHours: numberOr(wp?.totalPlannedHours ?? wp?.plannedHours, 0),
            totalWorkedHours: numberOr(wp?.totalWorkedHours ?? wp?.workedHours, 0),
            tasks: []
        };

        normalized.tasks = Array.isArray(wp?.tasks)
            ? wp.tasks.map((task, taskIndex) => normalizeTask(task, taskIndex, normalized))
            : [];

        return normalized;
    }

    function normalizeTask(task, index, wp) {
        return {
            ...task,
            order_number: task?.order_number ?? task?.orderNumber ?? index + 1,
            title: task?.title ?? 'Task title',
            description: task?.description ?? '',
            start_month: numberOr(task?.start_month ?? task?.startMonth ?? wp?.start_month, 1),
            end_month: numberOr(task?.end_month ?? task?.endMonth ?? wp?.end_month, 1),
            planned_hours: task?.planned_hours ?? task?.plannedHoursByMonth ?? task?.plannedHours ?? {},
            worked_hours: task?.worked_hours ?? task?.workedHoursByMonth ?? task?.workedHours ?? {}
        };
    }

    function renderNav() {
        const hasProjects = state.projects.length > 0;
        empty.hidden = hasProjects;
        nav.hidden = !hasProjects;
        nav.innerHTML = '';

        state.projects.forEach(project => {
            const li = document.createElement('li');
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'project-nav-btn' + (String(project.id) === String(state.selectedId) ? ' active' : '');
            button.innerHTML = `
                <span class="project-nav-title">${escapeHtml(project.title)}</span>
                <span class="project-nav-meta">
                    <span class="status-badge badge-${String(project.status || '').toLowerCase()}">${escapeHtml(project.status || '')}</span>
                    <span>${project.duration} mo</span>
                </span>
            `;
            button.addEventListener('click', () => selectProject(project.id));
            li.appendChild(button);
            nav.appendChild(li);
        });
    }

    function renderDetails(data) {
        placeholder.hidden = true;
        details.hidden = false;

        const project = data.project;
        const wps = data.wps || [];
        const canBeConcluded = !!data.canBeConcluded;

        details.innerHTML = `
            <div>
                <div class="detail-title-bar">
                    <h2>${escapeHtml(project.title)}</h2>
                    <div class="detail-meta">
                        <span class="status-badge badge-${String(project.status || '').toLowerCase()}">${escapeHtml(project.status || '')}</span>
                        <span>Duration: M1 – M${project.duration}</span>
                        ${project.manager ? `<span>PM: ${escapeHtml(project.manager)}</span>` : ''}
                    </div>
                </div>

                ${wps.length
                    ? `<div class="wp-list">${wps.map(renderWpCard).join('')}</div>`
                    : `<div class="empty-state empty-state-inline"><p>No work packages defined for this project yet.</p></div>`}
            </div>
            ${canBeConcluded ? `<div class="project-actions"><button type="button" class="btn-primary" id="complete-project-btn">Concludi</button></div>` : ''}
        `;

        const completeBtn = document.getElementById('complete-project-btn');
        if (completeBtn) {
            completeBtn.addEventListener('click', async () => {
                const response = await fetch(`${ctx}/manager-home?action=completeProject`, {
                    method: 'POST',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
                    },
                    body: new URLSearchParams({ project_id: project.id })
                });

                const payload = await response.json().catch(() => ({}));
                if (!response.ok) {
                    alert(payload.error || 'Unable to complete project.');
                    return;
                }

                const projectInList = state.projects.find(p => String(p.id) === String(project.id));
                if (projectInList) {
                    projectInList.status = 'CONCLUDED';
                }
                await selectProject(project.id);
            });
        }
    }

    function renderWpCard(wp) {
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
                        <span class="hours-label">Planned</span>
                        <span class="hours-value planned">${numberOr(wp.totalPlannedHours, 0)} h</span>
                        <span class="hours-sep">·</span>
                        <span class="hours-label">Worked</span>
                        <span class="hours-value worked">${numberOr(wp.totalWorkedHours, 0)} h</span>
                    </div>
                </div>

                ${tasks.length
                    ? `<div class="task-table-scroll">
                        <table class="task-table monitor-task-table">
                            <thead>
                                <tr>
                                    <th scope="col">ID</th>
                                    <th scope="col">Title</th>
                                    <th scope="col">Interval</th>
                                    <th scope="col">Monthly breakdown</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${tasks.map(task => renderTaskRow(wp, task)).join('')}
                            </tbody>
                        </table>
                    </div>`
                    : `<div class="task-empty">No tasks in this WP.</div>`}
            </div>
        `;
    }

    function renderTaskRow(wp, task) {
        const months = range(task.start_month, task.end_month);
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
                    <div class="month-compare-table">
                        <div class="month-compare-head">Month</div>
                        <div class="month-compare-head">Planned</div>
                        <div class="month-compare-head">Worked</div>
                        ${months.map(month => `
                            <div class="month-cell-label">M${month}</div>
                            <div class="month-cell-planned">${lookup(task.planned_hours, month)} h</div>
                            <div class="month-cell-worked">${lookup(task.worked_hours, month)} h</div>
                        `).join('')}
                    </div>
                </td>
            </tr>
        `;
    }

    function range(start, end) {
        const from = numberOr(start, 1);
        const to = numberOr(end, from);
        const values = [];
        for (let i = from; i <= to; i += 1) {
            values.push(i);
        }
        return values;
    }

    function lookup(map, month) {
        if (!map || typeof map !== 'object') return 0;
        const value = map[month] ?? map[String(month)] ?? 0;
        return numberOr(value, 0);
    }

    function numberOr(value, fallback) {
        const n = Number(value);
        return Number.isFinite(n) ? n : fallback;
    }

    async function getJSON(url) {
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.error || 'Request failed');
        }
        return data;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
})();
