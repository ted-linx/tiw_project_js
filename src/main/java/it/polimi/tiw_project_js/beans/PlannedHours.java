package it.polimi.tiw_project_js.beans;

public class PlannedHours {
    private int taskId;
    private int month;
    private int hours;

    public PlannedHours() {}

    public PlannedHours(int taskId, int month, int hours) {
        this.taskId = taskId;
        this.month = month;
        this.hours = hours;
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int id) { this.taskId = id; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }
}

