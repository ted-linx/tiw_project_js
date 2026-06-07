package it.polimi.tiw_project_js.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PlannedHoursDAO {
    private final Connection connection;

    public PlannedHoursDAO(Connection connection) {
        this.connection = connection;
    }

    public Map<Integer, Integer> getPlannedHoursOfTask(int id, int start_month, int end_month) throws SQLException {
        Map<Integer, Integer> plannedHours = new HashMap<>();
        for(int i = start_month; i <= end_month; i++) {
            plannedHours.put(i, 0);
        }
        String query = "SELECT month, hours FROM planned_hours WHERE task_id = ? ORDER BY month";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    plannedHours.put(rs.getInt("month"), rs.getInt("hours"));
                }
            }
        }

        return plannedHours;
    }

    public void saveOrUpdatePlannedHours(int task_id, int month, int hours) throws SQLException {
        String query = "INSERT INTO planned_hours(task_id, month, hours) VALUES(?,?,?) " +
                       "ON DUPLICATE KEY UPDATE hours = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, task_id);
            ps.setInt(2, month);
            ps.setInt(3, hours);
            ps.setInt(4, hours);
            ps.executeUpdate();
        }
    }
}
