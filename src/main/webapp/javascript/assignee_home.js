import {
    showSuccess,
    showError,
    clearMessages,
    initGreeting
} from './utils.js';

(() => {
    const ctx = window.APP_CONTEXT || '';

    const emptyStateProjects = document.getElementById('empty-state-projects');
    const projectNav = document.getElementById('project-nav');
    const detailPlaceholder = document.getElementById('detail-placeholder');
    const projectDetails = document.getElementById('project-details');

    const state = {
        assignedProjects: [],
        selectedProject: null,
        editingKey: null
    };

    initGreeting();

    init();



    async function getJSON(url) {
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'application/json'
            }
        });

        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.error || 'Request failed');
        }
        return data;
    }

    async function postJSON(url, payload) {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            const err = new Error(data.error || 'Request failed');
            err.payload = data;
            throw err;
        }
        return data;
    }

    async function init() {
        clearMessages();

        try {
            const data = await getJSON(`${ctx}/assignee-home?action=init`);
            state.assignedProjects = Array.isArray(data.assignedProjects) ? data.assignedProjects : [];
            renderProjectList();

            if (state.assignedProjects.length > 0) {
                await selectProject(state.assignedProjects[0].id);
            } else {
                renderDetails();
            }
        } catch (err) {
            showError(err.message || 'Could not load collaborator data.');
        }
    }

    function renderProjectList() {
        if (!projectNav || !emptyStateProjects) return;

        projectNav.innerHTML = '';

        if (!state.assignedProjects.length) {
            emptyStateProjects.hidden = false;
            return;
        }

        emptyStateProjects.hidden = true;

        state.assignedProjects.forEach(project => {
            const li = document.createElement('li');

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'project-nav-btn' + (state.selectedProject?.id === project.id ? ' active' : '');
            btn.innerHTML = `
        <span class="project-nav-title">${escapeHtml(project.title)}</span>
        <span class="project-nav-meta">
          <span class="status-badge badge-${String(project.status || '').toLowerCase()}">${escapeHtml(project.status || '')}</span>
          <span>${escapeHtml(String(project.duration || 0))} mo</span>
        </span>
      `;
            btn.addEventListener('click', () => selectProject(project.id));

            li.appendChild(btn);
            projectNav.appendChild(li);
        });
    }

    async function selectProject(projectId) {
        clearMessages();

        try {
            const data = await getJSON(`${ctx}/assignee-home?action=projectDetails&project_id=${encodeURIComponent(projectId)}`);
            state.selectedProject = data.selectedProject || null;
            renderProjectList();
            renderDetails();
        } catch (err) {
            showError(err.message || 'Could not load project details.');
        }
    }

    function renderDetails() {
        if (!detailPlaceholder || !projectDetails) return;

        if (!state.selectedProject) {
            detailPlaceholder.hidden = false;
            projectDetails.hidden = true;
            projectDetails.innerHTML = '';
            return;
        }

        detailPlaceholder.hidden = true;
        projectDetails.hidden = false;

        const p = state.selectedProject;
        const wps = Array.isArray(p.visibleWPs) ? p.visibleWPs : [];
        const tasksByWp = p.tasksByWp || {};

        projectDetails.innerHTML = `
      <div class="detail-title-bar">
        <div>
          <h2>${escapeHtml(p.title)}</h2>
          <div class="detail-meta">
            <span class="status-badge badge-${String(p.status || '').toLowerCase()}">${escapeHtml(p.status || '')}</span>
            <span>Duration: M1 – M${escapeHtml(String(p.duration || 0))}</span>
          </div>
        </div>
      </div>

      ${wps.length === 0 ? `
        <div class="empty-state empty-state-inline">
          <p>No work packages assigned to you in this project.</p>
        </div>
      ` : `
        <div class="wp-list">
          ${wps.map(wp => renderWpCard(wp, tasksByWp[String(wp.id)] || tasksByWp[wp.id] || [])).join('')}
        </div>
      `}
    `;

        bindInlineEditors();
    }

    function renderWpCard(wp, tasks) {
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
            <span class="hours-value worked">${totalWorkedHours(tasks)} h</span>
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
        ` : `<div class="task-empty">No assigned tasks in this WP.</div>`}
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
          <div class="month-compare-table" style="grid-template-columns: 64px 110px;">
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
            const worked = task.worked_hours?.[month] ?? task.worked_hours?.[String(month)] ?? 0;
            cells.push(`
        <div class="month-cell-label">M${month}</div>
        <button
          type="button"
          class="month-cell-worked inline-hours-trigger"
          data-task-id="${task.id}"
          data-month="${month}"
          data-value="${worked}">
          ${worked} h
        </button>
      `);
        }
        return cells.join('');
    }

    function totalWorkedHours(tasks) {
        return (tasks || []).reduce((sum, task) => {
            const explicitTotal = Number(task.totalWorkedHours);
            if (Number.isFinite(explicitTotal) && explicitTotal > 0) {
                return sum + explicitTotal;
            }

            const workedHours = task.worked_hours || {};
            const taskTotal = Object.values(workedHours).reduce((taskSum, value) => {
                return taskSum + (Number(value) || 0);
            }, 0);

            return sum + taskTotal;
        }, 0);
    }

    function bindInlineEditors() {
        document.querySelectorAll('.inline-hours-trigger').forEach(btn => {
            btn.addEventListener('click', () => activateHoursEditor(btn));
        });
    }

    function activateHoursEditor(button) {
        const key = `${button.dataset.taskId}:${button.dataset.month}`;
        if (state.editingKey && state.editingKey !== key) return;
        state.editingKey = key;

        const oldValue = button.dataset.value ?? '0';
        const taskId = button.dataset.taskId;
        const month = button.dataset.month;

        const input = document.createElement('input');
        input.type = 'number';
        input.min = '0';
        input.step = '1';
        input.value = oldValue;
        input.className = 'inline-hours-input';

        const cell = button;
        cell.replaceWith(input);
        input.focus();
        input.select();

        let saved = false;

        const restore = (value) => {
            const newButton = document.createElement('button');
            newButton.type = 'button';
            newButton.className = 'month-cell-worked inline-hours-trigger';
            newButton.dataset.taskId = taskId;
            newButton.dataset.month = month;
            newButton.dataset.value = value;
            newButton.textContent = `${value} h`;
            input.replaceWith(newButton);
            state.editingKey = null;
            bindInlineEditors();
        };

        const commit = async () => {
            if (saved) return;
            saved = true;

            const nextValue = input.value.trim();
            if (!/^(0|[1-9]\d*)$/.test(nextValue)) {
                restore(oldValue);
                showError('Please provide valid non-negative integer hours.');
                return;
            }

            if (nextValue === String(oldValue)) {
                restore(oldValue);
                return;
            }

            try {
                const data = await postJSON(`${ctx}/assignee-home?action=saveWorkedHours`, {
                    task_id: Number(taskId),
                    month: Number(month),
                    hours: Number(nextValue),
                    project_id: Number(state.selectedProject.id)
                });

                updateLocalWorkedHours(Number(taskId), Number(month), Number(nextValue), data.updatedTask || null);
                restore(String(nextValue));
                showSuccess(data.message || 'Worked hours saved successfully.');
            } catch (err) {
                restore(oldValue);
                showError(err?.payload?.error || err.message || 'Unable to save worked hours.');
            }
        };

        input.addEventListener('blur', commit);
        input.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                input.blur();
            }
            if (event.key === 'Escape') {
                saved = true;
                restore(oldValue);
            }
        });
    }

    function updateLocalWorkedHours(taskId, month, hours, updatedTask) {
        if (!state.selectedProject) return;

        const tasksByWp = state.selectedProject.tasksByWp || {};
        Object.keys(tasksByWp).forEach(wpId => {
            tasksByWp[wpId] = tasksByWp[wpId].map(task => {
                if (Number(task.id) !== Number(taskId)) return task;

                const nextTask = { ...task };
                nextTask.worked_hours = { ...(task.worked_hours || {}), [month]: hours };

                if (updatedTask) {
                    nextTask.totalWorkedHours = updatedTask.totalWorkedHours ?? nextTask.totalWorkedHours;
                } else {
                    const values = Object.values(nextTask.worked_hours).map(v => Number(v) || 0);
                    nextTask.totalWorkedHours = values.reduce((sum, v) => sum + v, 0);
                }
                return nextTask;
            });
        });

        renderDetails();
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
})();
