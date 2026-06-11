(() => {
    const ctx = window.APP_CONTEXT || '';

    const greeting = document.getElementById('greeting');
    const logoutBtn = document.getElementById('btn-logout');
    const alertSuccess = document.getElementById('alert-success');
    const successMsg = document.getElementById('success-message');
    const alertError = document.getElementById('alert-error');
    const errorMsg = document.getElementById('error-message');

    const projectSelect = document.getElementById('assignment-project-id');
    const wpSelect = document.getElementById('assignment-wp-id');
    const taskSelect = document.getElementById('assignment-task-id');
    const collaboratorSelect = document.getElementById('assignment-collaborator');

    const plannedHoursCaption = document.getElementById('planned-hours-caption');
    const monthsGrid = document.getElementById('months-grid');

    const assignProjectSection = document.getElementById('assign-project-section');
    const assignProjectId = document.getElementById('assign-project-id');

    const state = {
        assignedProjects: [],
        projectWPs: [],
        wpTasks: [],
        collaborators: [],
        selectedProject: null,
        selectedTask: null
    };

    if (greeting && window.APP_USER?.fullName) {
        greeting.innerHTML = `Hello, <strong>${window.APP_USER.fullName}</strong>`;
    }

    if (projectSelect) projectSelect.addEventListener('change', onProjectChange);
    if (wpSelect) wpSelect.addEventListener('change', onWpChange);
    if (taskSelect) taskSelect.addEventListener('change', onTaskChange);

    init();

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

    function clearMessages() {
        clearError();
        clearSuccess();
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

    async function init() {
        clearMessages();

        try {
            const data = await getJSON(`${ctx}/manager-home?action=init`);

            state.assignedProjects = data.assignedProjects || [];
            state.collaborators = data.collaborators || [];

            renderProjects();
            renderWPs();
            renderTasks();
            renderCollaborators();
            renderMonths(null);
            toggleAssignProjectSection();

            if (state.assignedProjects.length && projectSelect) {
                projectSelect.value = String(state.assignedProjects[0].id);
                await onProjectChange();
            }
        } catch (err) {
            showError(err.message || 'Could not load initial data.');
        }
    }

    async function onProjectChange() {
        clearMessages();

        const projectId = projectSelect.value;
        state.selectedProject = state.assignedProjects.find(
            p => String(p.id) === String(projectId)
        ) || null;
        state.projectWPs = [];
        state.wpTasks = [];
        state.selectedTask = null;

        renderWPs();
        renderTasks();
        renderMonths(null);
        toggleAssignProjectSection();

        if (!projectId) return;

        try {
            const data = await getJSON(
                `${ctx}/manager-home?action=wps&project_id=${encodeURIComponent(projectId)}`
            );

            state.projectWPs = data.projectWPs || [];
            state.selectedProject = data.selectedProject || state.selectedProject;

            renderWPs();
            toggleAssignProjectSection();
        } catch (err) {
            showError(err.message || 'Could not load work packages.');
        }
    }

    function renderProjects() {
        fillSelect(
            projectSelect,
            state.assignedProjects,
            '— Select a project —',
            p => ({ value: p.id, label: p.title })
        );
    }

    async function onWpChange() {
        clearMessages();

        const projectId = projectSelect.value;
        const wpId = wpSelect.value;

        state.wpTasks = [];
        state.selectedTask = null;

        renderTasks();
        renderMonths(null);

        if (!projectId || !wpId) return;

        try {
            const data = await getJSON(
                `${ctx}/manager-home?action=tasks` +
                `&project_id=${encodeURIComponent(projectId)}` +
                `&wp_id=${encodeURIComponent(wpId)}`
            );

            state.wpTasks = data.wpTasks || [];
            renderTasks();
        } catch (err) {
            showError(err.message || 'Could not load tasks.');
        }
    }

    function renderWPs() {
        const hasProject = !!projectSelect?.value;
        fillSelect(
            wpSelect,
            state.projectWPs,
            hasProject ? '— Select a work package —' : '— Select a project first —',
            wp => ({ value: wp.id, label: `WP${wp.order_number} – ${wp.title}` }),
            !hasProject
        );
    }

    async function onTaskChange() {
        clearMessages();

        const taskId = taskSelect.value;
        state.selectedTask = null;

        renderMonths(null);

        if (!taskId) {
            return;
        }

        try {
            const data = await getJSON(
                `${ctx}/manager-home?action=taskDetails` +
                `&task_id=${encodeURIComponent(taskId)}`
            );

            state.selectedTask = data.selectedTask || null;
            renderMonths(state.selectedTask);
        } catch (err) {
            showError(err.message || 'Could not load task details.');
        }
    }

    function renderTasks() {
        const hasWp = !!wpSelect?.value;
        fillSelect(
            taskSelect,
            state.wpTasks,
            hasWp ? '— Select a task —' : '— Select a work package first —',
            task => ({ value: task.id, label: `T${task.order_number} – ${task.title}` }),
            !hasWp
        );
    }

    function renderCollaborators() {
        fillSelect(
            collaboratorSelect,
            state.collaborators,
            state.collaborators.length ? '— Select a collaborator —' : '— No collaborators available —',
            c => ({ value: c.username, label: c.fullName || `${c.lastName || ''} ${c.firstName || ''}`.trim() }),
            state.collaborators.length === 0
        );
    }

    function renderMonths(task) {
        if (!monthsGrid || !plannedHoursCaption) return;

        monthsGrid.innerHTML = '';

        if (!task) {
            plannedHoursCaption.textContent = 'Please select a task first';
            return;
        }

        plannedHoursCaption.textContent =
            `Months M${task.start_month} – M${task.end_month}`;

        for (let m = task.start_month; m <= task.end_month; m++) {
            const field = document.createElement('div');
            field.className = 'field';

            field.innerHTML = `
        <label for="m${m}">M${m}</label>
        <input id="m${m}" name="m${m}" type="number" min="0" step="1" value="0" />
      `;

            monthsGrid.appendChild(field);
        }
    }

    function fillSelect(selectEl, items, placeholder, mapFn, disabled = false) {
        if (!selectEl) return;

        selectEl.innerHTML = '';

        const placeholderOption = document.createElement('option');
        placeholderOption.value = '';
        placeholderOption.textContent = placeholder;
        selectEl.appendChild(placeholderOption);

        items.forEach(item => {
            const mapped = mapFn(item);
            const option = document.createElement('option');
            option.value = mapped.value;
            option.textContent = mapped.label;
            selectEl.appendChild(option);
        });

        selectEl.disabled = disabled;
        selectEl.value = '';
    }

    function toggleAssignProjectSection() {
        if (!assignProjectSection || !assignProjectId) return;

        const project = state.selectedProject;
        const shouldShow = !!project && String(project.status) === 'CREATED';

        assignProjectSection.hidden = !shouldShow;
        assignProjectId.value = shouldShow ? project.id : '';
    }
})();
