package it.polimi.tiw_project_js.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.polimi.tiw_project_js.beans.Project;
import it.polimi.tiw_project_js.beans.Task;
import it.polimi.tiw_project_js.beans.User;
import it.polimi.tiw_project_js.beans.WP;
import it.polimi.tiw_project_js.dao.PlannedHoursDAO;
import it.polimi.tiw_project_js.dao.ProjectDAO;
import it.polimi.tiw_project_js.dao.TaskDAO;
import it.polimi.tiw_project_js.dao.UserDAO;
import it.polimi.tiw_project_js.dao.WPDAO;
import it.polimi.tiw_project_js.forms.ProjectForm;
import it.polimi.tiw_project_js.utils.ConnectionHandler;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@WebServlet("/admin-home")
public class AdminHome extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");

        String action = req.getParameter("action");

        if (action == null || action.isBlank()) {
            req.setAttribute("user", user);
            RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/admin_home.jsp");
            rd.forward(req, resp);
            return;
        }

        if ("data".equals(action)) {
            Connection connection = null;
            try {
                connection = ConnectionHandler.getConnection(getServletContext());
                ProjectDAO projectDAO = new ProjectDAO(connection);
                UserDAO userDAO = new UserDAO(connection);

                List<Project> createdProjects = projectDAO.getProjectsByCreator(user.getUsername());
                List<User> technicalUsers = userDAO.getStaffByRole(User.Role.TECHNICAL);

                JsonObject userJson = new JsonObject();
                userJson.addProperty("username", user.getUsername());
                userJson.addProperty("firstName", user.getFirstName());
                userJson.addProperty("lastName", user.getLastName());
                userJson.addProperty("fullName", user.getFullName());
                userJson.addProperty("role", user.getRole().toString());
                userJson.addProperty("isManager", user.isManager());
                userJson.addProperty("isAssignee", user.isAssignee());

                JsonObject result = new JsonObject();
                result.add("user", userJson);
                result.add("technicalUsers", gson.toJsonTree(technicalUsers));
                result.add("createdProjects", gson.toJsonTree(createdProjects));

                writeJson(resp, HttpServletResponse.SC_OK, result);
                return;

            } catch (SQLException e) {
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + escapeJson(e.getMessage()));
                return;
            } finally {
                try {
                    ConnectionHandler.closeConnection(connection);
                } catch (SQLException ignore) {
                }
            }
        }

        sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated");
            return;
        }

        String action = req.getParameter("action");
        if (action == null || action.isBlank()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing action");
            return;
        }

        switch (action) {
            case "save" -> handleSave(req, resp, user);
            default -> sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
        }
    }

    private void handleSave(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        Connection connection = null;
        try {
            Project incoming = gson.fromJson(req.getReader(), Project.class);
            validateProjectPayload(incoming);

            connection = ConnectionHandler.getConnection(getServletContext());
            connection.setAutoCommit(false);

            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);
            TaskDAO taskDAO = new TaskDAO(connection);
            PlannedHoursDAO plannedHoursDAO = new PlannedHoursDAO(connection);

            int projectId;
            boolean isNew = incoming.getId() <= 0;

            if (isNew) {
                ProjectForm projectForm = new ProjectForm(
                        incoming.getTitle().trim(),
                        incoming.getDuration(),
                        incoming.getManager().trim()
                );
                projectId = projectDAO.createProjectAndReturnId(projectForm, user.getUsername());
            } else {
                boolean editable = projectDAO.userCreatedProjectWithStatus(user.getUsername(), incoming.getId(), Project.Status.CREATED);
                if (!editable) {
                    sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "Project not editable");
                    return;
                }

                projectDAO.updateProject(incoming.getId(), incoming.getTitle().trim(), incoming.getDuration(), incoming.getManager().trim());
                taskDAO.deleteTasksByProject(incoming.getId());
                wpDAO.deleteWPsByProject(incoming.getId());
                projectId = incoming.getId();
            }

            List<WP> workPackages = incoming.getWorkPackages();
            if (workPackages != null) {
                for (int wpIndex = 0; wpIndex < workPackages.size(); wpIndex++) {
                    WP wp = workPackages.get(wpIndex);
                    int wpId = wpDAO.createWPAndReturnId(projectId, wpIndex + 1, wp.getTitle().trim(), wp.getStart_month(), wp.getEnd_month());

                    List<Task> tasks = wp.getTasks();
                    if (tasks == null) continue;

                    for (int taskIndex = 0; taskIndex < tasks.size(); taskIndex++) {
                        Task task = tasks.get(taskIndex);
                        int taskId = taskDAO.createTaskAndReturnId(
                                wpId,
                                taskIndex + 1,
                                task.getTitle().trim(),
                                task.getDescription() == null ? "" : task.getDescription().trim(),
                                task.getStart_month(),
                                task.getEnd_month()
                        );

                        Map<Integer, Integer> plannedHours = task.getPlanned_hours();
                        if (plannedHours != null) {
                            for (Map.Entry<Integer, Integer> entry : plannedHours.entrySet()) {
                                int month = entry.getKey();
                                int hours = entry.getValue() == null ? 0 : entry.getValue();
                                if (hours < 0) {
                                    throw new IllegalArgumentException("Planned hours cannot be negative");
                                }
                                plannedHoursDAO.saveOrUpdatePlannedHours(taskId, month, hours);
                            }
                        }
                    }
                }
            }

            connection.commit();

            JsonObject result = new JsonObject();
            result.addProperty("message", "Project saved successfully.");
            result.addProperty("projectId", projectId);
            writeJson(resp, HttpServletResponse.SC_OK, result);

        } catch (IllegalArgumentException e) {
            rollbackQuietly(connection);
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            rollbackQuietly(connection);
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + escapeJson(e.getMessage()));
        } catch (Exception e) {
            rollbackQuietly(connection);
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Save failed: " + escapeJson(e.getMessage()));
        } finally {
            resetAutoCommitAndClose(connection);
        }
    }

    private void validateProjectPayload(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Missing project payload");
        }
        if (project.getTitle() == null || project.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Project title is required");
        }
        if (project.getManager() == null || project.getManager().trim().isEmpty()) {
            throw new IllegalArgumentException("Project manager is required");
        }
        if (project.getDuration() <= 0) {
            throw new IllegalArgumentException("Project duration must be greater than 0");
        }

        List<WP> workPackages = project.getWorkPackages();
        if (workPackages == null) {
            return;
        }

        for (WP wp : workPackages) {
            if (wp.getTitle() == null || wp.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Every work package must have a title");
            }
            if (wp.getStart_month() < 1 || wp.getEnd_month() < 1 || wp.getStart_month() > wp.getEnd_month()) {
                throw new IllegalArgumentException("Invalid work package interval");
            }
            if (wp.getEnd_month() > project.getDuration()) {
                throw new IllegalArgumentException("A work package ends after the project duration");
            }

            List<Task> tasks = wp.getTasks();
            if (tasks == null) {
                continue;
            }

            for (Task task : tasks) {
                if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
                    throw new IllegalArgumentException("Every task must have a title");
                }
                if (task.getStart_month() < wp.getStart_month() || task.getEnd_month() > wp.getEnd_month() || task.getStart_month() > task.getEnd_month()) {
                    throw new IllegalArgumentException("A task interval must stay inside its work package interval");
                }
                validateHoursMap(task.getPlanned_hours(), task.getStart_month(), task.getEnd_month(), "planned");
                validateHoursMap(task.getWorked_hours(), task.getStart_month(), task.getEnd_month(), "worked");
            }
        }
    }

    private void validateHoursMap(Map<Integer, Integer> hoursMap, int startMonth, int endMonth, String label) {
        if (hoursMap == null) {
            return;
        }
        for (Map.Entry<Integer, Integer> entry : hoursMap.entrySet()) {
            Integer month = entry.getKey();
            Integer hours = entry.getValue();
            if (month == null || month < startMonth || month > endMonth) {
                throw new IllegalArgumentException("Invalid " + label + " hours month");
            }
            if (hours == null || hours < 0) {
                throw new IllegalArgumentException("Invalid " + label + " hours value");
            }
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) return;
        try {
            connection.rollback();
        } catch (SQLException ignore) {
        }
    }

    private void resetAutoCommitAndClose(Connection connection) {
        if (connection == null) return;
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignore) {
        }
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException ignore) {
        }
    }

    private void sendJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("error", message);
        writeJson(resp, status, payload);
    }

    private void writeJson(HttpServletResponse resp, int status, Object payload) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        gson.toJson(payload, resp.getWriter());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
