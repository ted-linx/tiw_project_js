package it.polimi.tiw_project_js.dao;

import it.polimi.tiw_project_js.beans.Task;
import it.polimi.tiw_project_js.forms.TaskForm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskDAO {
    private final Connection connection;

    public TaskDAO(Connection connection) {
        this.connection = connection;
    }

    public void createTask(TaskForm taskForm) throws SQLException {
        String query = "INSERT INTO task(order_number, title, description, start_month, end_month, wp_id) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, getNextOrderNumber(taskForm.wp_id()));
            ps.setString(2, taskForm.title());
            ps.setString(3, taskForm.description());
            ps.setInt(4, taskForm.start_month());
            ps.setInt(5, taskForm.end_month());
            ps.setInt(6, taskForm.wp_id());
            ps.executeUpdate();
        }
    }

    public List<Task> getTasksOfWP(int wp_id) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT * FROM task WHERE wp_id=? ORDER BY order_number";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, wp_id);
             try (ResultSet rs = ps.executeQuery();) {
                 while (rs.next()) {
                     tasks.add(createTaskBean(rs));
                 }
             }
        }

        return tasks;
    }

    public int getNextOrderNumber(int wp_id) throws SQLException {
        String query = "SELECT MAX(order_number) as order_number FROM task WHERE wp_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, wp_id);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    return rs.getInt("order_number") + 1;
                }
            }
        }

        return 1;
    }

    public boolean wpContainsTask(int wp_id, int task_id) throws SQLException {
        String query = "SELECT * FROM task WHERE wp_id = ? AND id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, wp_id);
            ps.setInt(2, task_id);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    return true;
                }
            }
        }

        return false;
    }

    public Task getTaskById(int task_id) throws SQLException {
        String query = "SELECT * FROM task WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, task_id);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    return createTaskBean(rs);
                }
            }
        }

        return null;
    }

    public boolean isAlreadyAssigned(int task_id, String username) throws SQLException {
        String query = "SELECT 1 FROM task_assignee WHERE task_id = ? AND username = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, task_id);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void createAssignment(int task_id, String username) throws SQLException {
        String query = "INSERT INTO task_assignee(task_id, username) VALUES(?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, task_id);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    public Task createTaskBean(ResultSet rs) throws SQLException {
        Map<Integer, Integer> plannedHours = new HashMap<>();
        Map<Integer, Integer> workedHours = new HashMap<>();

        PlannedHoursDAO plannedHoursDAO = new PlannedHoursDAO(connection);
        plannedHours = plannedHoursDAO.getPlannedHoursOfTask(rs.getInt("id"), rs.getInt("start_month"), rs.getInt("end_month"));

        WorkedHoursDAO workedHoursDAO = new WorkedHoursDAO(connection);
        workedHours = workedHoursDAO.getWorkedHoursOfTask(rs.getInt("id"),  rs.getInt("start_month"), rs.getInt("end_month"));

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
}
