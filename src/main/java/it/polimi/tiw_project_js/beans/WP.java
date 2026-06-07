package it.polimi.tiw_project_js.beans;

import java.util.List;

public class WP {
    private int id;
    private int order_number;
    private String title;
    private int start_month;
    private int end_month;
    private int project_id;
    private List<Task> tasks;

    public WP() {}

    public WP(int id, int order_number, String title,
              int start_month, int end_month, int project_id, List<Task> tasks) {
        this.id = id;
        this.order_number = order_number;
        this.title = title;
        this.start_month = start_month;
        this.end_month = end_month;
        this.project_id = project_id;
        this.tasks = tasks;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrder_number() { return order_number; }
    public void setOrderNumber(int n) { this.order_number = n; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStart_month() { return start_month; }
    public void setStart_month(int m) { this.start_month = m; }

    public int getEnd_month() { return end_month; }
    public void setEnd_month(int m) { this.end_month = m; }

    public int getProject_id() { return project_id; }
    public void setProject_id(int id) { this.project_id = id; }

    public int getTotalPlannedHours() {
        return tasks.stream().mapToInt(Task::getTotalPlannedHours).sum();
    }

    public int getTotalWorkedHours() {
        return tasks.stream().mapToInt(Task::getTotalWorkedHours).sum();
    }
    public List<Task> getTasks() { return tasks; }
}

