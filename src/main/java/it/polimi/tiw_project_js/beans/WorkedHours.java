package it.polimi.tiw_project_js.beans;

public class WorkedHours {
    private int taskId;
    private String username;
    private int month;
    private int hours;

    public WorkedHours() {}

    public WorkedHours(int taskId, String username, int month, int hours) {
        this.taskId = taskId;
        this.username = username;
        this.month = month;
        this.hours = hours;
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int id) { this.taskId = id; }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }
}

