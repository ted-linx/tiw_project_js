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
  const newProjectBtn = document.getElementById('new-project-btn');

  let createdProjects = [];
  let technicalUsers = [];
  let user = null;
  let currentProject = null;
  let originalProjectSnapshot = null;
  let isDirty = false;

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

  function markDirty() {
    isDirty = true;
  }

  function clearDirty() {
    isDirty = false;
  }

  function cloneProject(project) {
    return project ? JSON.parse(JSON.stringify(project)) : null;
  }

  function setCleanSnapshot(project) {
    originalProjectSnapshot = cloneProject(project);
    clearDirty();
  }

  function refreshDirtyState() {
    if (!currentProject || !originalProjectSnapshot) {
      clearDirty();
      return;
    }

    isDirty = JSON.stringify(currentProject) !== JSON.stringify(originalProjectSnapshot);
  }

  function normalizeTask(task, taskIndex, wp) {
    return {
      ...task,
      order_number: task?.order_number ?? taskIndex + 1,
      title: task?.title ?? 'Task title',
      description: task?.description ?? '',
      start_month: task?.start_month ?? wp?.start_month ?? 1,
      end_month: task?.end_month ?? wp?.end_month ?? 1,
      totalPlannedHours: task?.totalPlannedHours ?? task?.plannedHours ?? 0,
      totalWorkedHours: task?.totalWorkedHours ?? task?.workedHours ?? 0,
      planned_hours: task?.planned_hours ?? {},
      worked_hours: task?.worked_hours ?? {}
    };
  }

  function normalizeWorkPackage(wp, wpIndex) {
    const normalized = {
      ...wp,
      order_number: wp?.order_number ?? wpIndex + 1,
      title: wp?.title ?? 'Untitled WP',
      start_month: wp?.start_month ?? 1,
      end_month: wp?.end_month ?? 1,
      totalPlannedHours: wp?.totalPlannedHours ?? wp?.plannedHours ?? 0,
      totalWorkedHours: wp?.totalWorkedHours ?? wp?.workedHours ?? 0
    };

    normalized.tasks = Array.isArray(wp?.tasks)
      ? wp.tasks.map((task, taskIndex) => normalizeTask(task, taskIndex, normalized))
      : [];

    return normalized;
  }

  function normalizeProject(project) {
    const normalized = {
      ...project,
      id: project?.id ?? null,
      title: project?.title ?? 'Untitled project',
      duration: project?.duration ?? 12,
      manager: project?.manager ?? user?.username ?? '',
      status: project?.status ?? 'CREATED'
    };

    normalized.workPackages = Array.isArray(project?.workPackages)
      ? project.workPackages.map((wp, wpIndex) => normalizeWorkPackage(wp, wpIndex))
      : [];

    return normalized;
  }

  function buildEmptyProject() {
    return normalizeProject({
      id: null,
      title: 'New project',
      duration: 12,
      manager: user?.username || '',
      status: 'CREATED',
      workPackages: []
    });
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
      technicalUsers = Array.isArray(data.technicalUsers) ? data.technicalUsers : [];
      createdProjects = Array.isArray(data.createdProjects)
        ? data.createdProjects.map(normalizeProject)
        : [];
      currentProject = null;
      originalProjectSnapshot = null;
      clearDirty();

      if (greeting && user) {
        const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
        greeting.innerHTML = fullName
          ? `Welcome, <strong>${escapeHtml(fullName)}</strong>`
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

    const draftProjects = createdProjects.filter((project) => project.id == null);
    const persistedProjects = createdProjects.filter((project) => project.id != null);
    const allProjects = [...draftProjects, ...persistedProjects];

    if (!allProjects.length) return;

    emptyStateProjects.setAttribute('hidden', 'hidden');
    projectNav.removeAttribute('hidden');

    allProjects.forEach((project) => {
      const li = document.createElement('li');
      const isActive = currentProject && (
        currentProject === project ||
        (currentProject.id != null && project.id != null && currentProject.id === project.id) ||
        (currentProject.id == null && project.id == null && currentProject.title === project.title)
      );
      const isDraft = project.id == null;

      li.innerHTML = `
        <button type="button" class="project-nav-btn ${isActive ? 'active' : ''}">
          <span class="project-nav-title">${escapeHtml(project.title)}</span>
          <span class="project-nav-meta">
            <span class="status-badge badge-${String(project.status).toLowerCase()}">${escapeHtml(project.status)}</span>
            <span>${escapeHtml(String(project.duration))} mo</span>
            ${isDraft ? '<span>Draft</span>' : ''}
          </span>
        </button>
      `;

      li.querySelector('button').addEventListener('click', () => {
        currentProject = normalizeProject(project);
        setCleanSnapshot(currentProject);
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
      return;
    }

    detailPlaceholder.setAttribute('hidden', 'hidden');
    projectDetails.removeAttribute('hidden');

    const workPackages = Array.isArray(currentProject.workPackages) ? currentProject.workPackages : [];
    const wpBlocks = workPackages.length === 0
      ? `<div class="empty-state empty-state-inline"><p>No work packages defined for this project yet.</p></div>`
      : workPackages.map((wp, wpIndex) => renderWorkPackage(wp, wpIndex)).join('');

    projectDetails.innerHTML = `
      <div class="detail-title-bar">
        <div>
          <h2>
            <span class="inline-edit" data-entity="project" data-field="title">${escapeHtml(currentProject.title)}</span>
          </h2>
          <div class="detail-meta">
            <span class="status-badge badge-${String(currentProject.status).toLowerCase()}">${escapeHtml(currentProject.status)}</span>
            <span>Duration: M1 – M<span class="inline-edit" data-entity="project" data-field="duration">${escapeHtml(String(currentProject.duration))}</span></span>
            <span class="meta-pm">PM: <select class="inline-select" data-entity="project" data-field="manager">
              ${technicalUsers.map(u => `<option value="${escapeHtml(u.username)}" ${u.username === currentProject.manager ? 'selected' : ''}>${escapeHtml(u.lastName + ' ' + u.firstName)}</option>`).join('')}
            </select></span>
            ${isDirty ? '<span>Unsaved changes</span>' : ''}
          </div>
        </div>
        <div class="detail-actions">
          <button type="button" class="action-chip action-chip--primary" data-action="add-wp">+ WP</button>
          ${isDirty ? '<button type="button" class="btn btn-primary" data-action="save-project">Save</button>' : ''}
        </div>
      </div>
      <div class="wp-list">
        ${wpBlocks}
      </div>
    `;

    wireWorkspaceEvents();
  }

  function renderWorkPackage(wp, wpIndex) {
    const tasks = Array.isArray(wp.tasks) ? wp.tasks : [];
    const taskMarkup = tasks.length === 0
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
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              ${tasks.map((task, taskIndex) => renderTask(task, taskIndex, wpIndex)).join('')}
            </tbody>
          </table>
        `;

    return `
      <div class="wp-card">
        <div class="wp-header">
          <div class="wp-header-left">
            <span class="wp-badge">WP${escapeHtml(String(wp.order_number))}</span>
            <div>
              <span class="wp-title inline-edit" data-entity="wp" data-field="title" data-wp-index="${wpIndex}">${escapeHtml(wp.title)}</span>
              <span class="wp-interval">
                M<span class="inline-edit" data-entity="wp" data-field="start_month" data-wp-index="${wpIndex}">${escapeHtml(String(wp.start_month))}</span>
                –
                M<span class="inline-edit" data-entity="wp" data-field="end_month" data-wp-index="${wpIndex}">${escapeHtml(String(wp.end_month))}</span>
              </span>
            </div>
          </div>
          <div class="wp-hours-summary">
            <span class="hours-label">Planned</span>
            <span class="hours-value planned">${escapeHtml(String(wp.totalPlannedHours))} h</span>
            <span class="hours-sep">·</span>
            <span class="hours-label">Worked</span>
            <span class="hours-value worked">${escapeHtml(String(wp.totalWorkedHours))} h</span>
          </div>
        </div>
        <div class="wp-toolbar">
          <button type="button" class="action-chip action-chip--primary" data-action="add-task" data-wp-index="${wpIndex}">+ Task</button>
          <button type="button" class="action-chip action-chip--danger" data-action="delete-wp" data-wp-index="${wpIndex}">− WP</button>
        </div>
        ${taskMarkup}
      </div>
    `;
  }

  function renderTask(task, taskIndex, wpIndex) {
    return `
      <tr>
        <td class="task-id-cell"><span class="task-id">T${escapeHtml(String(wpIndex + 1))}.${escapeHtml(String(task.order_number))}</span></td>
        <td class="task-title-cell"><span class="inline-edit" data-entity="task" data-field="title" data-wp-index="${wpIndex}" data-task-index="${taskIndex}">${escapeHtml(task.title)}</span></td>
        <td class="task-desc-cell"><span class="inline-edit" data-entity="task" data-field="description" data-wp-index="${wpIndex}" data-task-index="${taskIndex}">${escapeHtml(task.description || '—')}</span></td>
        <td class="task-interval-cell">
          M<span class="inline-edit" data-entity="task" data-field="start_month" data-wp-index="${wpIndex}" data-task-index="${taskIndex}">${escapeHtml(String(task.start_month))}</span>
          –
          M<span class="inline-edit" data-entity="task" data-field="end_month" data-wp-index="${wpIndex}" data-task-index="${taskIndex}">${escapeHtml(String(task.end_month))}</span>
        </td>
        <td><span class="hours-pill planned">${escapeHtml(String(task.totalPlannedHours))} h</span></td>
        <td><span class="hours-pill worked">${escapeHtml(String(task.totalWorkedHours))} h</span></td>
        <td><button type="button" class="action-chip action-chip--primary" data-action="delete-task" data-wp-index="${wpIndex}" data-task-index="${taskIndex}">− Task</button></td>
      </tr>
    `;
  }

  function wireWorkspaceEvents() {
    if (!projectDetails) return;

    projectDetails.querySelectorAll('.inline-edit').forEach((el) => {
      el.addEventListener('click', () => makeEditable(el));
    });

    projectDetails.querySelectorAll('.inline-select').forEach((sel) => {
      sel.addEventListener('change', () => {
        applyValue(sel.dataset, sel.value);
        refreshDirtyState();
        renderProjectList();
        renderWorkspace();
      });
    });

    projectDetails.querySelector('[data-action="add-wp"]')?.addEventListener('click', addWP);
    projectDetails.querySelector('[data-action="save-project"]')?.addEventListener('click', saveCurrentProject);

    projectDetails.querySelectorAll('[data-action="add-task"]').forEach((btn) => {
      btn.addEventListener('click', () => addTask(Number(btn.dataset.wpIndex)));
    });

    projectDetails.querySelectorAll('[data-action="delete-wp"]').forEach((btn) => {
      btn.addEventListener('click', () => deleteWP(Number(btn.dataset.wpIndex)));
    });

    projectDetails.querySelectorAll('[data-action="delete-task"]').forEach((btn) => {
      btn.addEventListener('click', () => deleteTask(Number(btn.dataset.wpIndex), Number(btn.dataset.taskIndex)));
    });
  }

  function makeEditable(el) {
    const currentValue = el.textContent.replace(/\s+h$/, '').trim();
    const input = document.createElement('input');
    input.type = 'text';
    input.value = currentValue === '—' ? '' : currentValue;
    input.className = 'inline-editor';
    el.replaceWith(input);
    input.focus();
    input.select();

    const commit = () => {
      applyValue(el.dataset, input.value.trim());
      refreshDirtyState();
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
      refreshDirtyState();
      return;
    }

    const wp = currentProject.workPackages?.[Number(dataset.wpIndex)];
    if (!wp) return;

    if (dataset.entity === 'wp') {
      wp[field] = normalizeValue(field, value, wp[field]);
      refreshDirtyState();
      return;
    }

    const task = wp.tasks?.[Number(dataset.taskIndex)];
    if (!task) return;
    task[field] = normalizeValue(field, value, task[field]);
    refreshDirtyState();
  }

  function normalizeValue(field, value, fallback) {
    if (['duration', 'start_month', 'end_month', 'totalPlannedHours', 'totalWorkedHours'].includes(field)) {
      const parsed = Number.parseInt(value, 10);
      return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
    }
    return value || fallback;
  }

  function addWP() {
    if (!currentProject) {
      showError('Create or select a project before adding a work package.');
      return;
    }

    const workPackages = Array.isArray(currentProject.workPackages)
      ? currentProject.workPackages
      : (currentProject.workPackages = []);

    workPackages.push(normalizeWorkPackage({
      order_number: workPackages.length + 1,
      title: 'WP title',
      start_month: 1,
      end_month: currentProject.duration || 12,
      totalPlannedHours: 0,
      totalWorkedHours: 0,
      tasks: []
    }, workPackages.length));

    markDirty();
    refreshDirtyState();
    renderProjectList();
    renderWorkspace();
  }

  function deleteWP(wpIndex) {
    if (!currentProject?.workPackages) return;
    currentProject.workPackages.splice(wpIndex, 1);
    currentProject.workPackages = currentProject.workPackages.map((wp, index) => normalizeWorkPackage({ ...wp, order_number: index + 1 }, index));
    refreshDirtyState();
    renderProjectList();
    renderWorkspace();
  }

  function addTask(wpIndex) {
    const wp = currentProject?.workPackages?.[wpIndex];
    if (!wp) return;
    if (!Array.isArray(wp.tasks)) wp.tasks = [];

    wp.tasks.push(normalizeTask({
      order_number: wp.tasks.length + 1,
      title: 'Task title',
      description: '',
      start_month: wp.start_month,
      end_month: wp.end_month,
      totalPlannedHours: 0,
      totalWorkedHours: 0,
      planned_hours: {},
      worked_hours: {}
    }, wp.tasks.length, wp));

    markDirty();
    refreshDirtyState();
    renderWorkspace();
  }

  function deleteTask(wpIndex, taskIndex) {
    const wp = currentProject?.workPackages?.[wpIndex];
    if (!wp || !Array.isArray(wp.tasks)) return;
    wp.tasks.splice(taskIndex, 1);
    wp.tasks = wp.tasks.map((task, index) => normalizeTask({ ...task, order_number: index + 1 }, index, wp));
    refreshDirtyState();
    renderWorkspace();
  }

  async function saveCurrentProject() {
    if (!currentProject) {
      showError('Select or create a project before saving.');
      return;
    }

    try {
      clearError();
      clearSuccess();

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

      if (currentProject.id == null && data.projectId != null) {
        currentProject.id = data.projectId;
      }

      const existingIndex = createdProjects.findIndex((project) => project.id != null && project.id === currentProject.id);
      if (existingIndex >= 0) {
        createdProjects[existingIndex] = normalizeProject(currentProject);
      } else {
        createdProjects.unshift(normalizeProject(currentProject));
      }

      currentProject = normalizeProject(currentProject);
      setCleanSnapshot(currentProject);
      renderProjectList();
      renderWorkspace();
      showSuccess(data.message || 'Project saved successfully.');
    } catch {
      showError('Save failed because the server response was not valid.');
    }
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
      createdProjects = [currentProject, ...createdProjects];
      markDirty();
      renderProjectList();
      renderWorkspace();
    });
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      window.location.href = `${ctx}/logout`;
    });
  }

  loadPage();
})();
