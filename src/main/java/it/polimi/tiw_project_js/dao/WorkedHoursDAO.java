package it.polimi.tiw_project_js.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class WorkedHoursDAO {
    private final Connection connection;

    public WorkedHoursDAO(Connection connection) {
        this.connection = connection;
    }

    public Map<Integer, Integer> getWorkedHoursOfTask(int id, int start_month, int end_month) throws SQLException {
        Map<Integer, Integer> workedHours = new HashMap<>();
        for(int i = start_month; i <= end_month; i++) {
            workedHours.put(i, 0);
        }
        String query = "SELECT month, SUM(hours) AS hours FROM worked_hours WHERE task_id = ? GROUP BY month ORDER BY month";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    workedHours.put(rs.getInt("month"), rs.getInt("hours"));
                }
            }
        }

        return workedHours;
    }

    public Map<Integer, Integer> getWorkedHoursOfTaskForCollaborator(int taskId, String username, int startMonth, int endMonth) throws SQLException {
        Map<Integer, Integer> workedHours = new HashMap<>();
        for (int i = startMonth; i <= endMonth; i++) {
            workedHours.put(i, 0);
        }
        String query = "SELECT month, hours FROM worked_hours WHERE task_id = ? AND username = ? ORDER BY month";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, taskId);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workedHours.put(rs.getInt("month"), rs.getInt("hours"));
                }
            }
        }
        return workedHours;
    }

    public void saveOrUpdateWorkedHours(int taskId, String username, int month, int hours) throws SQLException {
        String query = "INSERT INTO worked_hours(task_id, username, month, hours) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE hours = VALUES(hours)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, taskId);
            ps.setString(2, username);
            ps.setInt(3, month);
            ps.setInt(4, hours);
            ps.executeUpdate();
        }
    }
}
