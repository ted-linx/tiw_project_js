package it.polimi.tiw_project_js.beans;

public class TaskAssignee {
    private int taskId;
    private String username;

    public TaskAssignee() {}

    public TaskAssignee(int taskId, String username) {
        this.taskId = taskId;
        this.username = username;
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username;
    }
}

