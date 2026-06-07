package it.polimi.tiw_project_js.dao;

import it.polimi.tiw_project_js.beans.Project;
import it.polimi.tiw_project_js.beans.WP;
import it.polimi.tiw_project_js.forms.ProjectForm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {
    private final Connection connection;

    public ProjectDAO(Connection connection) {
        this.connection = connection;
    }

    public List<Project> getProjectByStatusAndCreator(Project.Status status, String creator) throws SQLException {
        List<Project> list = new ArrayList<>();
        String query = "SELECT * FROM project WHERE status=? AND created_by=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, status.toString());
            ps.setString(2, creator);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(createProjectBean(rs));
            }
        }
        return list;
    }

    public void createProject(ProjectForm projectForm, String creator) throws SQLException {
        String query = "INSERT INTO project(title, duration, status, created_by, manager) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, projectForm.title());
            ps.setInt(2, projectForm.duration());
            ps.setString(3, "CREATED");
            ps.setString(4, creator);
            ps.setString(5, projectForm.manager());
            ps.executeUpdate();
        }
    }

    public Project getProjectById(int id) throws SQLException {
        String query = "SELECT * FROM project WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createProjectBean(rs);
                }
            }
        }
        return null;
    }

    public List<Project> getProjectsByManager(String managerUsername) throws SQLException {
        List<Project> list = new ArrayList<>();
        String query = "SELECT * FROM project WHERE manager = ? ORDER BY id DESC";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, managerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(createProjectBean(rs));
            }
        }
        return list;
    }

    public boolean userCreatedProjectWithStatus(String creator, int project_id, Project.Status status) throws SQLException {
        String query = "SELECT * FROM project WHERE created_by=? AND id=? AND status=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, creator);
            ps.setInt(2, project_id);
            ps.setString(3, status.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean userIsProjectManager(String username, int project_id) throws SQLException {
        String query = "SELECT * FROM project WHERE id=? AND manager=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, project_id);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canBeConcluded(int projectId) throws SQLException {
        String query = """
            SELECT
                EXISTS (
                    SELECT 1
                    FROM project p
                    JOIN work_package w ON w.project_id = p.id
                    JOIN task t ON t.wp_id = w.id
                    WHERE p.id = ?
                      AND p.status <> 'CONCLUDED'
                ) AS has_tasks,
                EXISTS (
                    SELECT 1
                    FROM project p
                    JOIN work_package w ON w.project_id = p.id
                    JOIN task t ON t.wp_id = w.id
                    WHERE p.id = ?
                      AND p.status <> 'CONCLUDED'
                      AND EXISTS (
                          SELECT 1
                          FROM planned_hours ph
                          WHERE ph.task_id = t.id
                      )
                      AND (COALESCE((
                          SELECT SUM(ph.hours)
                          FROM planned_hours ph
                          WHERE ph.task_id = t.id
                      ), 0) > COALESCE((
                          SELECT SUM(wh.hours)
                          FROM worked_hours wh
                          WHERE wh.task_id = t.id
                      ), 0))
                      OR (
                          SELECT SUM(ph.hours)
                          FROM planned_hours ph
                          WHERE ph.task_id = t.id
                          ) = 0
                ) AS has_incomplete_tasks
            """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, projectId);
            ps.setInt(2, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("has_tasks") && !rs.getBoolean("has_incomplete_tasks");
                }
            }
        }
        return false;
    }


    public void completeProject(int projectId) throws SQLException {
        String query = "UPDATE project SET status = 'CONCLUDED' WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }
    }


    public boolean canBeAssigned(int projectId) throws SQLException {
        // Returns true if every task of the project (in CREATED state) has:
        //   1. at least one assignee
        //   2. planned hours for every month in [start_month, end_month]
        String query = """
            SELECT
                /* does ANY task lack at least one collaborator? */
                EXISTS (
                    SELECT 1
                    FROM project p
                    JOIN work_package w ON w.project_id = p.id
                    JOIN task t        ON t.wp_id = w.id
                    WHERE p.id = ? AND p.status = 'CREATED'
                      AND NOT EXISTS (
                            SELECT 1 FROM task_assignee ta WHERE ta.task_id = t.id
                      )
                ) AS missing_assignee,
                /* does ANY task lack planned hours for a full month range? */
                EXISTS (
                    SELECT 1
                    FROM project p
                    JOIN work_package w ON w.project_id = p.id
                    JOIN task t        ON t.wp_id = w.id
                    WHERE p.id = ? AND p.status = 'CREATED'
                      AND (
                            SELECT COUNT(*) FROM planned_hours ph
                            WHERE ph.task_id = t.id
                      ) < (t.end_month - t.start_month + 1)
                ) AS missing_hours
            """;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, projectId);
            ps.setInt(2, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return !rs.getBoolean("missing_assignee") && !rs.getBoolean("missing_hours");
                }
            }
        }
        return false;
    }

    public void updateStatus(int projectId, String newStatus) throws SQLException {
        String query = "UPDATE project SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, newStatus);
            ps.setInt(2, projectId);
            ps.executeUpdate();
        }
    }
    private Project createProjectBean(ResultSet rs) throws SQLException {
        WPDAO wpDAO = new WPDAO(connection);
        List<WP> workPackages = wpDAO.getWPsOfProject(rs.getInt("id"));
        return new Project(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getInt("duration"),
                rs.getString("status"),
                rs.getString("created_by"),
                rs.getString("manager"),
                workPackages
        );
    }
}
