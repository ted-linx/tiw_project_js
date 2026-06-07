package it.polimi.tiw_project_js.forms;

public record TaskForm(int project_id, Integer wp_id, String title, String description, Integer start_month, Integer end_month) {
}
