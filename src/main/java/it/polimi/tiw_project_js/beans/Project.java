package it.polimi.tiw_project_js.beans;

import java.util.List;

public class Project {
    public enum Status {
        CREATED,
        ASSIGNED,
        CONCLUDED
    }

    private int id;
    private String title;
    private int duration;           // number of months
    private Status status;          // "CREATED", "ASSIGNED", "CONCLUDED"
    private String manager;
    private String created_by;
    private List<WP> workPackages;

    public Project() {}

    public Project(int id, String title, int duration, String status,
                   String created_by, String manager, List<WP> workPackages) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.status = Status.valueOf(status);
        this.created_by = created_by;
        this.manager = manager;
        this.workPackages = workPackages;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getManager() { return manager; }
    public void setManager(String u) { this.manager = u; }

    public String getCreatedBy() { return created_by; }
    public void setCreatedBy(String u) { this.created_by = u; }

    public List<WP> getWorkPackages() { return workPackages; }
}
