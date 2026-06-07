package it.polimi.tiw_project_js.dao;

import it.polimi.tiw_project_js.beans.Task;
import it.polimi.tiw_project_js.forms.TaskForm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    public int createTaskAndReturnId(int wpId, int orderNumber, String title, String description, int startMonth, int endMonth) throws SQLException {
        String query = "INSERT INTO task(order_number, title, description, start_month, end_month, wp_id) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderNumber);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setInt(4, startMonth);
            ps.setInt(5, endMonth);
            ps.setInt(6, wpId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Unable to retrieve generated task id");
    }

    public void deleteTasksByProject(int projectId) throws SQLException {
        String deletePlannedHours = "DELETE ph FROM planned_hours ph JOIN task t ON ph.task_id = t.id JOIN work_package w ON t.wp_id = w.id WHERE w.project_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deletePlannedHours)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }

        String deleteWorkedHours = "DELETE wh FROM worked_hours wh JOIN task t ON wh.task_id = t.id JOIN work_package w ON t.wp_id = w.id WHERE w.project_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteWorkedHours)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }

        String deleteTasks = "DELETE t FROM task t JOIN work_package w ON t.wp_id = w.id WHERE w.project_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteTasks)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }
    }

    public List<Task> getTasksOfWP(int wp_id) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String query = "SELECT * FROM task WHERE wp_id=? ORDER BY order_number";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, wp_id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(createTaskBean(rs));
                }
            }
        }
        return tasks;
    }

    public Task getTaskById(int task_id) throws SQLException {
        String query = "SELECT * FROM task WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, task_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createTaskBean(rs);
                }
            }
        }
        return null;
    }

    public boolean wpContainsTask(int wp_id, int task_id) throws SQLException {
        String query = "SELECT * FROM task WHERE id=? AND wp_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, task_id);
            ps.setInt(2, wp_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getNextOrderNumber(int wp_id) throws SQLException {
        String query = "SELECT MAX(order_number) as order_number FROM task WHERE wp_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, wp_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("order_number") + 1;
                }
            }
        }

        return 1;
    }

    public Task createTaskBean(ResultSet rs) throws SQLException {
        PlannedHoursDAO plannedHoursDAO = new PlannedHoursDAO(connection);
        WorkedHoursDAO workedHoursDAO = new WorkedHoursDAO(connection);

        int id = rs.getInt("id");
        int startMonth = rs.getInt("start_month");
        int endMonth = rs.getInt("end_month");

        Map<Integer, Integer> plannedHours = plannedHoursDAO.getPlannedHoursOfTask(id, startMonth, endMonth);
        Map<Integer, Integer> workedHours = workedHoursDAO.getWorkedHoursOfTask(id, startMonth, endMonth);

        return new Task(
                id,
                rs.getInt("order_number"),
                rs.getString("title"),
                rs.getString("description"),
                startMonth,
                endMonth,
                rs.getInt("wp_id"),
                plannedHours,
                workedHours
        );
    }
}
