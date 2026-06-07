package it.polimi.tiw_project_js.dao;

import it.polimi.tiw_project_js.beans.Task;
import it.polimi.tiw_project_js.beans.WP;
import it.polimi.tiw_project_js.forms.WPForm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WPDAO {
    private final Connection connection;

    public WPDAO(Connection connection) {
        this.connection = connection;
    }

    public void createWP(WPForm wpForm) throws SQLException {
        String query = "INSERT INTO work_package(order_number, title, start_month, end_month, project_id) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, getNextOrderNumber(wpForm.project_id()));
            ps.setString(2, wpForm.title());
            ps.setInt(3, wpForm.start_month());
            ps.setInt(4, wpForm.end_month());
            ps.setInt(5, wpForm.project_id());
            ps.executeUpdate();
        }
    }

    public int getNextOrderNumber(int project_id) throws SQLException {
        String query = "SELECT MAX(order_number) as order_number FROM work_package WHERE project_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, project_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("order_number") + 1;
                }
            }
        }

        return 1;
    }

    public List<WP> getWPsOfProject(int project_id) throws SQLException {
        List<WP> wps = new ArrayList<>();
        String query = "SELECT * FROM work_package WHERE project_id = ? ORDER BY order_number";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, project_id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    wps.add(createWPBean(rs));
                }
            }
        }

        return wps;
    }

    public WP getWPById(int wp_id) throws SQLException {
        String query = "SELECT * FROM work_package WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, wp_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createWPBean(rs);
                }
            }
        }

        return null;
    }

    public boolean projectContainsWP(int project_id, int wp_id) throws SQLException {
        String query = "SELECT * FROM work_package WHERE project_id = ? AND id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, project_id);
            ps.setInt(2, wp_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }

        return false;
    }

    public WP createWPBean(ResultSet rs) throws SQLException {
        TaskDAO taskDAO = new TaskDAO(connection);
        List<Task> tasks = taskDAO.getTasksOfWP(rs.getInt("id"));
        return new WP(
                rs.getInt("id"),
                rs.getInt("order_number"),
                rs.getString("title"),
                rs.getInt("start_month"),
                rs.getInt("end_month"),
                rs.getInt("project_id"),
                tasks
        );
    }
}
