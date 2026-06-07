package it.polimi.tiw_project_js.dao;

import it.polimi.tiw_project_js.beans.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final Connection connection;

    public UserDAO(Connection connection) { this.connection = connection; }

    public User checkCredentials(String username, String password) throws SQLException {
        String query = "SELECT * FROM user WHERE username = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return createUserBean(rs);
            }
        }
        return null;
    }

    public User getUserByUsername(String username) throws SQLException {
        return findByUsername(username);
    }

    public User findByUsername(String username) throws SQLException {
        String query = "SELECT * FROM user WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return createUserBean(rs);
            }
        }

        return null;
    }

    public List<User> getStaffByRole(User.Role role) throws SQLException {
        List<User> list = new ArrayList<>();
        String query = "SELECT * FROM user WHERE role = ? ORDER BY last_name, first_name";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, role.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(createUserBean(rs));
            }
        }
        return list;
    }

    public List<User> getCollaboratorsByManager(String managerUsername) throws SQLException {
        List<User> list = new ArrayList<>();
        String query =
                "SELECT DISTINCT u.* FROM user u " +
                        "JOIN task_assignee tc ON u.username = tc.username " +
                        "JOIN task t ON tc.task_id = t.id " +
                        "JOIN work_package wp ON t.wp_id = wp.id " +
                        "JOIN project p ON wp.project_id = p.id " +
                        "WHERE p.manager = ? ORDER BY u.last_name, u.first_name";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, managerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(createUserBean(rs));
            }
        }
        return list;
    }

    private User createUserBean(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("username"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("password"),
                rs.getString("photo"),
                rs.getString("role"),
                checkIfManager(rs.getString("username")),
                checkIfAssignee(rs.getString("username"))
        );
    }

    private boolean checkIfManager(String username) throws SQLException {
        String query = "SELECT * FROM project WHERE manager = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return true;
            }
        }
        return false;
    }

    private boolean checkIfAssignee(String username) throws SQLException {
        String query = "SELECT * FROM task_assignee WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return true;
            }
        }
        return false;
    }
}

