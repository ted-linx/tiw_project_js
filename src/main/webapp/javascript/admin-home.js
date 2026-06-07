(() => {
  const ctx = window.APP_CONTEXT || '';

  const greeting = document.getElementById('greeting');
  const logoutBtn = document.getElementById('btn-logout');
  const alertSuccess = document.getElementById('alert-success');
  const successMsg = document.getElementById('success-message');
  const alertError = document.getElementById('alert-error');
  const errorMsg = document.getElementById('error-message');
  const emptyStateProjects = document.getElementById('empty-state-projects');
  const projectNav = document.getElementById('project-nav');
  const detailPlaceholder = document.getElementById('detail-placeholder');
  const projectDetails = document.getElementById('project-details');
  const tableContainer = document.getElementById('table-container');
  const workspaceSubtitle = document.getElementById('workspace-subtitle');
  const addWpBtn = document.getElementById('add-wp-btn');
  const saveProjectBtn = document.getElementById('save-project-btn');
  const newProjectBtn = document.getElementById('new-project-btn');

  let createdProjects = [];
  let user = null;
  let currentProject = null;

  function showSuccess(message) {
    if (!alertSuccess || !successMsg) return;
    successMsg.textContent = message;
    alertSuccess.removeAttribute('hidden');
  }

  function showError(message) {
    if (errorMsg) errorMsg.textContent = message;
    if (alertError) {
      alertError.removeAttribute('hidden');
      alertError.focus?.();
      return;
    }
    console.error(message);
  }

  function clearError() {
    if (alertError) alertError.setAttribute('hidden', 'hidden');
    if (errorMsg) errorMsg.textContent = '';
  }

  function clearSuccess() {
    if (alertSuccess) alertSuccess.setAttribute('hidden', 'hidden');
    if (successMsg) successMsg.textContent = '';
  }

  function normalizeProject(project) {
    const workPackages = Array.isArray(project?.workPackages) ? project.workPackages : [];
    return {
      ...project,
      workPackages: workPackages.map((wp, wpIndex) => ({
        ...wp,
        order_number: wp.order_number ?? wpIndex + 1,
        tasks: Array.isArray(wp.tasks) ? wp.tasks.map((task, taskIndex) => ({
          ...task,
          order_number: task.order_number ?? taskIndex + 1,
          description: task.description ?? '',
          start_month: task.start_month ?? wp.start_month ?? 1,
          end_month: task.end_month ?? wp.end_month ?? 1,
          totalPlannedHours: task.totalPlannedHours ?? 0,
          totalWorkedHours: task.totalWorkedHours ?? 0
        })) : [],
        title: wp.title ?? 'Untitled WP',
        start_month: wp.start_month ?? 1,
        end_month: wp.end_month ?? 1,
        totalPlannedHours: wp.totalPlannedHours ?? 0,
        totalWorkedHours: wp.totalWorkedHours ?? 0
      }))
    };
  }

  function buildEmptyProject() {
    return {
      id: null,
      title: 'New project',
      duration: 12,
      manager: user?.username || '',
      status: 'CREATED',
      workPackages: []
    };
  }

  async function loadPage() {
    try {
      clearError();
      clearSuccess();
      const res = await fetch(`${ctx}/admin-home?action=data`, {
        headers: { Accept: 'application/json' }
      });
      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'Unable to load administrator data.');
      }

      user = data.user || null;
      createdProjects = Array.isArray(data.createdProjects) ? data.createdProjects.map(normalizeProject) : [];
      currentProject = null;

      if (greeting && user) {
        const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
        greeting.innerHTML = fullName
          ? `Welcome, <strong>${escapeHtml(fullName)}</strong>!`
          : `Welcome, <strong>${escapeHtml(user.username || '')}</strong>!`;
      }

      renderProjectList();
      renderWorkspace();
    } catch (error) {
      showError(error.message || 'Unable to load the administrator workspace.');
    }
  }

  function renderProjectList() {
    if (!emptyStateProjects || !projectNav) return;

    emptyStateProjects.removeAttribute('hidden');
    projectNav.setAttribute('hidden', 'hidden');
    projectNav.innerHTML = '';

    const drafts = currentProject && currentProject.id == null ? [currentProject] : [];
    const allProjects = [...drafts, ...createdProjects];

    if (!allProjects.length) return;

    emptyStateProjects.setAttribute('hidden', 'hidden');
    projectNav.removeAttribute('hidden');

    allProjects.forEach((project) => {
      const li = document.createElement('li');
      const isActive = currentProject === project || (currentProject && project.id != null && currentProject.id === project.id);
      li.innerHTML = `
        <form method="get">
          <input type="hidden" name="project_id" value="${project.id ?? ''}"/>
          <button type="submit" class="project-nav-btn ${isActive ? 'active' : ''}">
            <span class="project-nav-title">${escapeHtml(project.title ?? 'Untitled project')}</span>
            <span class="project-nav-meta">
              <span class="status-badge badge-${String(project.status || 'CREATED').toLowerCase()}">${escapeHtml(project.status || 'CREATED')}</span>
              <span>${escapeHtml(String(project.duration ?? ''))} mo</span>
            </span>
          </button>
        </form>
      `;
      li.addEventListener('click', () => {
        currentProject = normalizeProject(project);
        renderProjectList();
        renderWorkspace();
      });
      projectNav.appendChild(li);
    });
  }

  function renderWorkspace() {
    clearError();
    clearSuccess();

    if (!detailPlaceholder || !projectDetails) return;

    detailPlaceholder.removeAttribute('hidden');
    projectDetails.setAttribute('hidden', 'hidden');

    if (!currentProject) {
      if (workspaceSubtitle) {
        workspaceSubtitle.textContent = 'Select a project from the list or create a new draft.';
      }
      if (tableContainer) {
        tableContainer.innerHTML = '<div class="empty-state empty-state-inline"><p>No project selected yet.</p></div>';
      }
      return;
    }

    detailPlaceholder.setAttribute('hidden', 'hidden');
    projectDetails.removeAttribute('hidden');

    if (workspaceSubtitle) {
      workspaceSubtitle.textContent = 'Click a value to edit it inline.';
    }

    const workPackages = Array.isArray(currentProject.workPackages) ? currentProject.workPackages : [];
    const wpBlocks = workPackages.length === 0
      ? [`<div class="empty-state empty-state-inline"><p>No work packages defined for this project yet.</p></div>`]
      : workPackages.map((wp, wpIndex) => renderWorkPackage(wp, wpIndex));

    projectDetails.innerHTML = `
      <div class="detail-title-bar">
        <div>
          <h2 id="project-title">${escapeHtml(currentProject.title ?? 'Untitled project')}</h2>
          <div class="detail-meta">
            <span class="status-badge badge-${String(currentProject.status || 'CREATED').toLowerCase()}">${escapeHtml(currentProject.status || 'CREATED')}</span>
            <span>Duration: M1 – M<span>${escapeHtml(String(currentProject.duration ?? ''))}</span></span>
            <span>PM: <span>${escapeHtml(currentProject.manager ?? '')}</span></span>
          </div>
        </div>
      </div>
      <div id="table-container">
        ${wpBlocks.join('')}
      </div>
    `;

    wireWorkspaceEvents();
  }

  function renderWorkPackage(wp, wpIndex) {
    const tasks = Array.isArray(wp.tasks) ? wp.tasks : [];
    const taskContent = tasks.length === 0
      ? `<div class="task-empty">No tasks in this WP.</div>`
      : `
        <table class="task-table">
          <thead>
            <tr>
              <th scope="col">ID</th>
              <th scope="col">Title</th>
              <th scope="col">Description</th>
              <th scope="col">Interval</th>
              <th scope="col">Planned</th>
              <th scope="col">Worked</th>
            </tr>
          </thead>
          <tbody>
            ${tasks.map((task, taskIndex) => `
              <tr>
                <td class="task-id-cell"><span class="task-id">T${escapeHtml(String(wp.order_number ?? wpIndex + 1))}.${escapeHtml(String(task.order_number ?? taskIndex + 1))}</span></td>
                <td class="task-title-cell">${escapeHtml(task.title ?? '')}</td>
                <td class="task-desc-cell">${escapeHtml(task.description ?? '—')}</td>
                <td class="task-interval-cell">M${escapeHtml(String(task.start_month ?? wp.start_month ?? '—'))} – M${escapeHtml(String(task.end_month ?? wp.end_month ?? '—'))}</td>
                <td><span class="hours-pill planned">${escapeHtml(String(task.totalPlannedHours ?? 0))} h</span></td>
                <td><span class="hours-pill worked">${escapeHtml(String(task.totalWorkedHours ?? 0))} h</span></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;

    return `
      <div class="wp-list">
        <div class="wp-card">
          <div class="wp-header">
            <div class="wp-header-left">
              <span class="wp-badge">WP${escapeHtml(String(wp.order_number ?? wpIndex + 1))}</span>
              <div>
                <span class="wp-title">${escapeHtml(wp.title ?? 'Untitled WP')}</span>
                <span class="wp-interval">M${escapeHtml(String(wp.start_month ?? '—'))} – M${escapeHtml(String(wp.end_month ?? '—'))}</span>
              </div>
            </div>
            <div class="wp-hours-summary">
              <span class="hours-label">Planned</span>
              <span class="hours-value planned">${escapeHtml(String(wp.totalPlannedHours ?? 0))} h</span>
              <span class="hours-sep">·</span>
              <span class="hours-label">Worked</span>
              <span class="hours-value worked">${escapeHtml(String(wp.totalWorkedHours ?? 0))} h</span>
            </div>
          </div>
          ${taskContent}
        </div>
      </div>
    `;
  }

  function wireWorkspaceEvents() {
    if (!projectDetails) return;

    projectDetails.querySelectorAll('.inline-edit').forEach((el) => {
      el.addEventListener('click', () => makeEditable(el));
    });

    projectDetails.querySelectorAll('[data-action="add-wp"]').forEach((btn) => {
      btn.addEventListener('click', addWP);
    });

    projectDetails.querySelectorAll('[data-action="add-task"]').forEach((btn) => {
      btn.addEventListener('click', () => addTask(Number(btn.dataset.wpIndex)));
    });

    projectDetails.querySelectorAll('[data-action="delete-wp"]').forEach((btn) => {
      btn.addEventListener('click', () => {
        currentProject.workPackages.splice(Number(btn.dataset.wpIndex), 1);
        renderProjectList();
        renderWorkspace();
      });
    });

    projectDetails.querySelectorAll('[data-action="delete-task"]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const wp = currentProject.workPackages[Number(btn.dataset.wpIndex)];
        if (!wp || !Array.isArray(wp.tasks)) return;
        wp.tasks.splice(Number(btn.dataset.taskIndex), 1);
        renderProjectList();
        renderWorkspace();
      });
    });
  }

  function makeEditable(el) {
    const currentValue = el.textContent.trim();
    const input = document.createElement('input');
    input.type = 'text';
    input.value = currentValue;
    input.className = 'inline-editor';
    el.replaceWith(input);
    input.focus();
    input.select();

    const commit = () => {
      applyValue(el.dataset, input.value.trim());
      renderProjectList();
      renderWorkspace();
    };

    input.addEventListener('blur', commit, { once: true });
    input.addEventListener('keydown', (event) => {
      if (event.key === 'Enter') input.blur();
      if (event.key === 'Escape') renderWorkspace();
    });
  }

  function applyValue(dataset, value) {
    if (!currentProject) return;
    const field = dataset.field;

    if (dataset.entity === 'project') {
      currentProject[field] = normalizeValue(field, value, currentProject[field]);
      return;
    }

    const wp = currentProject.workPackages?.[Number(dataset.wpIndex)];
    if (!wp) return;

    if (dataset.entity === 'wp') {
      wp[field] = normalizeValue(field, value, wp[field]);
      return;
    }

    const task = wp.tasks?.[Number(dataset.taskIndex)];
    if (!task) return;
    task[field] = normalizeValue(field, value, task[field]);
  }

  function normalizeValue(field, value, fallback) {
    if (['duration', 'start_month', 'end_month'].includes(field)) {
      const parsed = Number.parseInt(value, 10);
      return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
    }
    return value || fallback;
  }

  function addWP() {
    if (!currentProject) {
      showError('Create or select a project before adding a work package.');
      return;
    }

    const workPackages = Array.isArray(currentProject.workPackages) ? currentProject.workPackages : (currentProject.workPackages = []);
    workPackages.push({
      order_number: workPackages.length + 1,
      title: 'WP title',
      start_month: 1,
      end_month: currentProject.duration || 12,
      totalPlannedHours: 0,
      totalWorkedHours: 0,
      tasks: []
    });

    renderProjectList();
    renderWorkspace();
  }

  function addTask(wpIndex) {
    const wp = currentProject?.workPackages?.[wpIndex];
    if (!wp) return;
    if (!Array.isArray(wp.tasks)) wp.tasks = [];
    wp.tasks.push({
      order_number: wp.tasks.length + 1,
      title: 'Task title',
      description: '',
      start_month: wp.start_month,
      end_month: wp.end_month,
      totalPlannedHours: 0,
      totalWorkedHours: 0,
      planned_hours: {},
      worked_hours: {}
    });

    renderProjectList();
    renderWorkspace();
  }

  function sumMap(map) {
    if (!map || typeof map !== 'object') return 0;
    return Object.values(map).reduce((sum, value) => sum + Number(value || 0), 0);
  }

  function sumTaskHours(tasks, key) {
    return (tasks || []).reduce((sum, task) => sum + sumMap(task[key]), 0);
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  if (newProjectBtn) {
    newProjectBtn.addEventListener('click', () => {
      currentProject = buildEmptyProject();
      renderProjectList();
      renderWorkspace();
    });
  }

  if (addWpBtn) {
    addWpBtn.addEventListener('click', addWP);
  }

  if (saveProjectBtn) {
    saveProjectBtn.addEventListener('click', async () => {
      if (!currentProject) {
        showError('Select or create a project before saving.');
        return;
      }

      try {
        const res = await fetch(`${ctx}/admin-home?action=save`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json'
          },
          body: JSON.stringify(currentProject)
        });

        const text = await res.text();
        let data = {};
        try {
          data = text ? JSON.parse(text) : {};
        } catch {
          showError('Server returned a non-JSON response while saving.');
          return;
        }

        if (!res.ok) {
          showError(data.error || 'Save failed.');
          return;
        }

        showSuccess(data.message || 'Project saved successfully.');
      } catch {
        showError('Save failed because the server response was not valid.');
      }
    });
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      window.location.href = `${ctx}/logout`;
    });
  }

  loadPage();
})();
