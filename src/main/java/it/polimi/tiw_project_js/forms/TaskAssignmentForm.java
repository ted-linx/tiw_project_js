package it.polimi.tiw_project_js.forms;

import java.util.List;

public record TaskAssignmentForm(Integer project_id, Integer wp_id, Integer task_id, String collaborator, List<Integer> months) {
    public TaskAssignmentForm withProjectId(Integer project_id) {
        return new TaskAssignmentForm(project_id, wp_id, task_id, collaborator, months);
    }

    public TaskAssignmentForm withWpId(Integer wp_id) {
        return new TaskAssignmentForm(project_id, wp_id, task_id, collaborator, months);
    }

    public TaskAssignmentForm withTaskId(Integer task_id) {
        return new TaskAssignmentForm(project_id, wp_id, task_id, collaborator, months);
    }

    public TaskAssignmentForm withCollaborator(String collaborator) {
        return new TaskAssignmentForm(project_id, wp_id, task_id, collaborator, months);
    }
    public TaskAssignmentForm withMonths(List<Integer> months) {
        return new TaskAssignmentForm(project_id, wp_id, task_id, collaborator, months);
    }
}
