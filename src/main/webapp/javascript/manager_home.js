import { showSuccess, showError, clearMessages, initGreeting, initLogout } from './utils.js';

(() => {
    const ctx = window.APP_CONTEXT || '';

    const projectSelect = document.getElementById('assignment-project-id');
    const wpSelect = document.getElementById('assignment-wp-id');
    const taskSelect = document.getElementById('assignment-task-id');
    const collaboratorSelect = document.getElementById('assignment-collaborator');

    const assignmentForm = document.getElementById('assignment-form');

    const plannedHoursCaption = document.getElementById('planned-hours-caption');
    const monthsGrid = document.getElementById('months-grid');

    const assignProjectSection = document.getElementById('assign-project-section');
    const assignProjectId = document.getElementById('assign-project-id');
    const assignProjectForm = document.getElementById('assign-project-form');

    const viewButtons = Array.from(document.querySelectorAll('[data-view-target]'));
    const views = Array.from(document.querySelectorAll('[data-view]'));

    const state = {
        assignedProjects: [],
        projectWPs: [],
        wpTasks: [],
        collaborators: [],
        selectedProject: null,
        selectedTask: null,
        currentView: 'assignment'
    };

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        initGreeting();
        initLogout(ctx);

        bindEvents();
        switchView(state.currentView);
        loadInitialData();
    }

    function bindEvents() {
        projectSelect?.addEventListener('change', onProjectChange);
        wpSelect?.addEventListener('change', onWpChange);
        taskSelect?.addEventListener('change', onTaskChange);
        assignmentForm?.addEventListener('submit', onSaveAssignment);
        assignProjectForm?.addEventListener('submit', onAssignProject);
        viewButtons.forEach(button => {
            button.addEventListener('click', () => switchView(button.dataset.viewTarget));
        });
    }

    function switchView(viewName) {
        state.currentView = viewName;

        views.forEach(section => {
            section.hidden = section.dataset.view !== viewName;
        });

        viewButtons.forEach(button => {
            const active = button.dataset.viewTarget === viewName;
            button.classList.toggle('is-active', active);
            button.setAttribute('aria-pressed', String(active));
        });

        document.dispatchEvent(new CustomEvent('manager:viewchange', { detail: { view: viewName } }));
    }

    async function loadInitialData() {
        clearMessages();

        try {
            const data = await getJSON(`${ctx}/manager-home?action=init`);

            state.assignedProjects = data.assignedProjects || [];
            state.collaborators = data.collaborators || [];
            state.selectedProject = null;
            state.selectedTask = null;
            state.projectWPs = [];
            state.wpTasks = [];

            renderProjects();
            renderWPs();
            renderTasks();
            renderCollaborators();
            renderMonths(null);
            toggleAssignProjectSection();
        } catch (err) {
            showError(err.message || 'Could not load manager home data.');
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

            state.projectWPs = data.projectWPs || data.wps || [];
            state.selectedProject = data.selectedProject || state.selectedProject;

            renderWPs();
            toggleAssignProjectSection();
        } catch (err) {
            showError(err.message || 'Could not load work packages.');
        }
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
                `${ctx}/manager-home?action=tasks&project_id=${encodeURIComponent(projectId)}&wp_id=${encodeURIComponent(wpId)}`
            );

            state.wpTasks = data.wpTasks || data.tasks || [];
            renderTasks();
        } catch (err) {
            showError(err.message || 'Could not load tasks.');
        }
    }

    async function onTaskChange() {
        clearMessages();

        const taskId = taskSelect.value;

        state.selectedTask = null;
        renderMonths(null);

        if (!taskId) return;

        try {
            const data = await getJSON(
                `${ctx}/manager-home?action=taskDetails&task_id=${encodeURIComponent(taskId)}`
            );

            state.selectedTask = data.selectedTask || data.task || null;
            renderMonths(state.selectedTask);
        } catch (err) {
            showError(err.message || 'Could not load task details.');
        }
    }

    async function onSaveAssignment(event) {
        event.preventDefault();
        clearMessages();

        try {
            const formData = new URLSearchParams(new FormData(assignmentForm));
            console.log('saveAssignment payload:', Array.from(formData.entries()));
            const data = await postForm(`${ctx}/manager-home?action=saveAssignment`, formData);

            if (data.success === false) {
                showStructuredErrors(data);
                return;
            }

            showSuccess(data.message || 'Assignment saved successfully.');
            document.dispatchEvent(new CustomEvent('manager:save-assignment'))
        } catch (err) {
            const structured = err?.payload;
            if (structured) {
                showStructuredErrors(structured);
                return;
            }
            showError(err.message || 'Could not save assignment.');
        }
    }

    async function onAssignProject(event) {
        event.preventDefault();
        clearMessages();

        try {
            const formData = new URLSearchParams(new FormData(assignProjectForm));
            const data = await postForm(`${ctx}/manager-home?action=assignProject`, formData);

            if (data.success === false) {
                showStructuredErrors(data);
                return;
            }

            showSuccess(data.message || 'Project assigned successfully.');

            if (state.selectedProject) {
                state.selectedProject.status = 'ASSIGNED';
            }

            toggleAssignProjectSection();
        } catch (err) {
            const structured = err?.payload;
            if (structured) {
                showStructuredErrors(structured);
                return;
            }
            showError(err.message || 'Could not assign project.');
        }
    }

    async function getJSON(url) {
        const res = await fetch(url, {
            method: 'GET',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        const data = await res.json().catch(() => ({}));

        if (!res.ok) {
            throw new Error(data.error || 'Request failed.');
        }

        return data;
    }

    async function postForm(url, formData) {
        const res = await fetch(url, {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: formData
        });

        const data = await res.json().catch(() => ({}));

        if (!res.ok) {
            const error = new Error(data.error || 'Request failed.');
            error.payload = data;
            throw error;
        }

        return data;
    }

    function renderProjects() {
        fillSelect(
            projectSelect,
            state.assignedProjects,
            '— Select a project —',
            p => ({ value: p.id, label: p.title }),
            false
        );
    }

    function renderWPs() {
        const hasProject = !!projectSelect.value;

        fillSelect(
            wpSelect,
            state.projectWPs,
            hasProject ? '— Select a work package —' : '— Select a project first —',
            wp => ({ value: wp.id, label: `WP${wp.order_number} – ${wp.title}` }),
            !hasProject
        );
    }

    function renderTasks() {
        const hasWp = !!wpSelect.value;

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
            c => ({
                value: c.username,
                label: c.fullName || `${c.lastName || c.lastname || ''} ${c.firstName || c.firstname || ''}`.trim()
            }),
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

        plannedHoursCaption.textContent = `Months M${task.start_month} – M${task.end_month}`;

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

    function toggleAssignProjectSection() {
        if (!assignProjectSection || !assignProjectId) return;

        const project = state.selectedProject;
        const status = project?.status?.toString?.() ?? project?.status ?? '';
        const shouldShow = !!project && status === 'CREATED';

        assignProjectSection.hidden = !shouldShow;
        assignProjectId.value = shouldShow ? project.id : '';
    }

    function fillSelect(selectEl, items, placeholder, mapper, disabled = false) {
        if (!selectEl) return;

        selectEl.innerHTML = '';

        const firstOption = document.createElement('option');
        firstOption.value = '';
        firstOption.textContent = placeholder;
        selectEl.appendChild(firstOption);

        items.forEach(item => {
            const mapped = mapper(item);
            const option = document.createElement('option');
            option.value = mapped.value;
            option.textContent = mapped.label;
            selectEl.appendChild(option);
        });

        selectEl.disabled = disabled;
        selectEl.value = '';
    }

    function formatFieldName(name) {
        return name
            .replaceAll('_', ' ')
            .replace(/\bm\d+\b/g, m => m.toUpperCase())
            .replace(/\b\w/g, c => c.toUpperCase());
    }

    function showStructuredErrors(data) {
        const missing = Array.isArray(data.missingFields) ? data.missingFields : [];
        const invalid = data.invalidFields && typeof data.invalidFields === 'object' ? data.invalidFields : {};
        const details = [];

        if (missing.length) {
            details.push(`Missing fields: ${missing.map(formatFieldName).join(', ')}`);
        }

        const invalidParts = Object.entries(invalid).map(([field, msg]) => `${formatFieldName(field)}: ${msg}`);
        if (invalidParts.length) {
            details.push(`Invalid fields: ${invalidParts.join(' | ')}`);
        }

        const message = details.length
            ? details.join(' · ')
            : (data.error || 'Could not save assignment.');

        showError(message);
    }
})();
