package it.polimi.tiw_project_js.dao;

import it.polimi.tiw_project_js.beans.Project;
import it.polimi.tiw_project_js.beans.Task;
import it.polimi.tiw_project_js.beans.WP;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskAssigneeDAO {
    private Connection connection;
    public TaskAssigneeDAO(Connection connection) {
        this.connection = connection;
    }


    public List<Project> getAssignedProjectsOfCollaborator(String collaboratorUsername) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String query =
                "SELECT DISTINCT p.id, p.title, p.duration, p.status, p.created_by, p.manager " +
                        "FROM project p " +
                        "JOIN work_package wp ON wp.project_id = p.id " +
                        "JOIN task t ON t.wp_id = wp.id " +
                        "JOIN task_assignee ta ON ta.task_id = t.id " +
                        "WHERE ta.username = ? " +
                        "ORDER BY p.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, collaboratorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    projects.add(new Project(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getInt("duration"),
                            rs.getString("status"),
                            rs.getString("created_by"),
                            rs.getString("manager"),
                            null
                    ));
                }
            }
        }
        return projects;
    }

    public boolean isCollaboratorAssignedToProject(String collaboratorUsername, int projectId) throws SQLException {
        String query =
                "SELECT 1 FROM task_assignee ta " +
                        "JOIN task t ON ta.task_id = t.id " +
                        "JOIN work_package wp ON t.wp_id = wp.id " +
                        "WHERE ta.username = ? AND wp.project_id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, collaboratorUsername);
            ps.setInt(2, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<WP> getAssignedWPsOfCollaboratorInProject(String collaboratorUsername, int projectId) throws SQLException {
        List<WP> wps = new ArrayList<>();
        String query =
                "SELECT DISTINCT wp.* FROM work_package wp " +
                        "JOIN task t ON t.wp_id = wp.id " +
                        "JOIN task_assignee ta ON ta.task_id = t.id " +
                        "WHERE ta.username = ? AND wp.project_id = ? " +
                        "ORDER BY wp.order_number";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, collaboratorUsername);
            ps.setInt(2, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    wps.add(new WP(
                            rs.getInt("id"),
                            rs.getInt("order_number"),
                            rs.getString("title"),
                            rs.getInt("start_month"),
                            rs.getInt("end_month"),
                            rs.getInt("project_id"),
                            null
                    ));
                }
            }
        }
        return wps;
    }

    public Map<Integer, List<Task>> getAssignedTasksByWpInProject(String collaboratorUsername, int projectId) throws SQLException {
        Map<Integer, List<Task>> tasksByWp = new HashMap<>();
        String query =
                "SELECT t.* FROM task t " +
                        "JOIN work_package wp ON t.wp_id = wp.id " +
                        "JOIN task_assignee ta ON ta.task_id = t.id " +
                        "WHERE ta.username = ? AND wp.project_id = ? " +
                        "ORDER BY t.wp_id, t.order_number";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, collaboratorUsername);
            ps.setInt(2, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task task = createAssignedTaskBean(rs, collaboratorUsername);
                    tasksByWp.computeIfAbsent(task.getWp_id(), key -> new ArrayList<>()).add(task);
                }
            }
        }
        return tasksByWp;
    }

    public Task getAssignedTaskDetails(String collaboratorUsername, int taskId) throws SQLException {
        String query =
                "SELECT t.* FROM task t " +
                        "JOIN task_assignee ta ON ta.task_id = t.id " +
                        "WHERE ta.username = ? AND t.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, collaboratorUsername);
            ps.setInt(2, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAssignedTaskBean(rs, collaboratorUsername);
                }
            }
        }
        return null;
    }

    private Task createAssignedTaskBean(ResultSet rs, String collaboratorUsername) throws SQLException {
        PlannedHoursDAO plannedHoursDAO = new PlannedHoursDAO(connection);
        WorkedHoursDAO workedHoursDAO = new WorkedHoursDAO(connection);
        Map<Integer, Integer> plannedHours = plannedHoursDAO.getPlannedHoursOfTask(
                rs.getInt("id"), rs.getInt("start_month"), rs.getInt("end_month")
        );
        Map<Integer, Integer> workedHours = workedHoursDAO.getWorkedHoursOfTaskForCollaborator(
                rs.getInt("id"), collaboratorUsername, rs.getInt("start_month"), rs.getInt("end_month")
        );

        return new Task(
                rs.getInt("id"),
                rs.getInt("order_number"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("start_month"),
                rs.getInt("end_month"),
                rs.getInt("wp_id"),
                plannedHours,
                workedHours
        );
    }

    public boolean collaboratorWorksForManager(String collaboratorUsername, String managerUsername) throws SQLException {
        String query =
                "SELECT 1 " +
                        "FROM task_assignee ta " +
                        "JOIN task t ON ta.task_id = t.id " +
                        "JOIN work_package wp ON t.wp_id = wp.id " +
                        "JOIN project p ON wp.project_id = p.id " +
                        "WHERE ta.username = ? AND p.manager = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, collaboratorUsername);
            ps.setString(2, managerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Project> getCollaboratingProjectsByManager(String collaboratorUsername, String managerUsername) throws SQLException {
        List<Project> reports = new ArrayList<>();
        String query =
                "SELECT DISTINCT p.id, p.title, p.duration, p.status, p.created_by, p.manager " +
                        "FROM project p " +
                        "JOIN work_package wp ON wp.project_id = p.id " +
                        "JOIN task t ON t.wp_id = wp.id " +
                        "JOIN task_assignee ta ON ta.task_id = t.id " +
                        "WHERE p.manager = ? AND ta.username = ? " +
                        "ORDER BY p.id DESC";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, managerUsername);
            ps.setString(2, collaboratorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    List<WP> wps = getCollaboratorWPsOfProject(rs.getInt("id"), collaboratorUsername);
                    Project project = new Project(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getInt("duration"),
                            rs.getString("status"),
                            rs.getString("created_by"),
                            rs.getString("manager"),
                            wps
                    );
                    reports.add(project);
                }
            }
        }
        return reports;
    }

    private List<WP> getCollaboratorWPsOfProject(int projectId, String collaboratorUsername) throws SQLException {
        List<WP> wps = new ArrayList<>();
        String query =
                "SELECT DISTINCT wp.* " +
                        "FROM work_package wp " +
                        "JOIN task t ON t.wp_id = wp.id " +
                        "JOIN task_assignee ta ON ta.task_id = t.id " +
                        "WHERE wp.project_id = ? AND ta.username = ? " +
                        "ORDER BY wp.order_number";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, projectId);
            ps.setString(2, collaboratorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    wps.add(new WP(
                            rs.getInt("id"),
                            rs.getInt("order_number"),
                            rs.getString("title"),
                            rs.getInt("start_month"),
                            rs.getInt("end_month"),
                            rs.getInt("project_id"),
                            getCollaboratorTasksOfWP(rs.getInt("id"), collaboratorUsername)
                    ));
                }
            }
        }
        return wps;
    }

    private List<Task> getCollaboratorTasksOfWP(int wpId, String collaboratorUsername) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String query =
                "SELECT DISTINCT t.* " +
                        "FROM task t " +
                        "JOIN task_assignee ta ON ta.task_id = t.id " +
                        "WHERE t.wp_id = ? AND ta.username = ? " +
                        "ORDER BY t.order_number";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, wpId);
            ps.setString(2, collaboratorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new Task(
                            rs.getInt("id"),
                            rs.getInt("order_number"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getInt("start_month"),
                            rs.getInt("end_month"),
                            rs.getInt("wp_id"),
                            new HashMap<>(),
                            getWorkedHoursOfTaskForCollaborator(rs.getInt("id"), rs.getInt("start_month"), rs.getInt("end_month"), collaboratorUsername)
                    ));
                }
            }
        }
        return tasks;
    }

    private Map<Integer, Integer> getWorkedHoursOfTaskForCollaborator(int taskId, int startMonth, int endMonth, String collaboratorUsername) throws SQLException {
        Map<Integer, Integer> workedHours = new HashMap<>();
        for (int i = startMonth; i <= endMonth; i++) {
            workedHours.put(i, 0);
        }

        String query =
                "SELECT month, hours FROM worked_hours " +
                        "WHERE task_id = ? AND username = ? ORDER BY month";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, taskId);
            ps.setString(2, collaboratorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workedHours.put(rs.getInt("month"), rs.getInt("hours"));
                }
            }
        }
        return workedHours;
    }
}
