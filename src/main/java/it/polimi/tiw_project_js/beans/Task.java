package it.polimi.tiw_project_js.beans;

import java.util.Map;

public class Task {
    private int id;
    private int order_number;
    private String title;
    private String description;
    private int start_month;
    private int end_month;
    private int wp_id;
    private Map<Integer, Integer> planned_hours;
    private Map<Integer, Integer> worked_hours;

    public Task() {}

    public Task(int id, int order_number, String title, String description,
                int start_month, int end_month, int wp_id, Map<Integer, Integer> planned_hours, Map<Integer, Integer> worked_hours) {
        this.id = id;
        this.order_number = order_number;
        this.title = title;
        this.description = description;
        this.start_month = start_month;
        this.end_month = end_month;
        this.wp_id = wp_id;
        this.planned_hours = planned_hours;
        this.worked_hours = worked_hours;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrder_number() { return order_number; }
    public void setOrder_number(int n) { this.order_number = n; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    public int getStart_month() { return start_month; }
    public void setStart_month(int m) { this.start_month = m; }

    public int getEnd_month() { return end_month; }
    public void setEnd_month(int m) { this.end_month = m; }

    public int getWp_id() { return wp_id; }
    public void setWp_id(int id) { this.wp_id = id; }

    public Map<Integer, Integer> getPlanned_hours() {
        return planned_hours;
    }

    public Map<Integer, Integer> getWorked_hours() {
        return worked_hours;
    }

    public int getTotalWorkedHours() {
        return worked_hours.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalPlannedHours() {
        return planned_hours.values().stream().mapToInt(Integer::intValue).sum();
    }
}

