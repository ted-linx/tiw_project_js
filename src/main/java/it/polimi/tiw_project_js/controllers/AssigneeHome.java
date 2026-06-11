package it.polimi.tiw_project_js.controllers;

import com.google.gson.Gson;
import it.polimi.tiw_project_js.beans.Project;
import it.polimi.tiw_project_js.beans.Task;
import it.polimi.tiw_project_js.beans.User;
import it.polimi.tiw_project_js.beans.WP;
import it.polimi.tiw_project_js.dao.ProjectDAO;
import it.polimi.tiw_project_js.dao.TaskAssigneeDAO;
import it.polimi.tiw_project_js.dao.WorkedHoursDAO;
import it.polimi.tiw_project_js.utils.ConnectionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/assignee-home")
public class AssigneeHome extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in.");
            return;
        }

        String action = req.getParameter("action");
        if (action == null || action.isBlank()) {
            req.getRequestDispatcher("/WEB-INF/assignee_home.jsp").forward(req, resp);
            return;
        }

        switch (action) {
            case "init" -> handleInit(user, req, resp);
            case "projectDetails" -> handleProjectDetails(user, req, resp);
            default -> sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            sendJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "User not logged in.");
            return;
        }

        String action = req.getParameter("action");
        if (!"saveWorkedHours".equals(action)) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
            return;
        }

        handleSaveWorkedHours(user, req, resp);
    }

    private void handleInit(User user, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Connection connection = null;
        try {
            connection = ConnectionHandler.getConnection(getServletContext());
            TaskAssigneeDAO taskAssigneeDAO = new TaskAssigneeDAO(connection);

            List<Project> assignedProjects = taskAssigneeDAO.getAssignedProjectsOfCollaborator(user.getUsername());

            Map<String, Object> payload = new HashMap<>();
            payload.put("assignedProjects", assignedProjects);

            sendJson(resp, HttpServletResponse.SC_OK, payload);
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load assigned projects.");
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleProjectDetails(User user, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String projectIdParam = req.getParameter("project_id");
        Connection connection = null;

        try {
            int projectId = Integer.parseInt(projectIdParam);

            connection = ConnectionHandler.getConnection(getServletContext());
            TaskAssigneeDAO taskAssigneeDAO = new TaskAssigneeDAO(connection);
            ProjectDAO projectDAO = new ProjectDAO(connection);

            if (!taskAssigneeDAO.isCollaboratorAssignedToProject(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not assigned to this project.");
                return;
            }

            Project selectedProject = projectDAO.getProjectById(projectId);
            List<WP> visibleWPs = taskAssigneeDAO.getAssignedWPsOfCollaboratorInProject(user.getUsername(), projectId);
            Map<Integer, List<Task>> tasksByWp = taskAssigneeDAO.getAssignedTasksByWpInProject(user.getUsername(), projectId);

            Map<String, Object> projectPayload = new HashMap<>();
            projectPayload.put("id", selectedProject.getId());
            projectPayload.put("title", selectedProject.getTitle());
            projectPayload.put("duration", selectedProject.getDuration());
            projectPayload.put("status", selectedProject.getStatus());
            projectPayload.put("visibleWPs", visibleWPs);
            projectPayload.put("tasksByWp", tasksByWp);

            Map<String, Object> payload = new HashMap<>();
            payload.put("selectedProject", projectPayload);

            sendJson(resp, HttpServletResponse.SC_OK, payload);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Please select a valid project.");
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load project details.");
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleSaveWorkedHours(User user, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Connection connection = null;

        try {
            Map<String, Object> body = readJsonBody(req);

            int projectId = getInt(body.get("project_id"));
            int taskId = getInt(body.get("task_id"));
            int month = getInt(body.get("month"));
            int hours = getInt(body.get("hours"));

            if (hours < 0) {
                throw new IllegalArgumentException("Please provide valid non-negative integer hours.");
            }

            connection = ConnectionHandler.getConnection(getServletContext());
            TaskAssigneeDAO taskAssigneeDAO = new TaskAssigneeDAO(connection);
            WorkedHoursDAO workedHoursDAO = new WorkedHoursDAO(connection);

            if (!taskAssigneeDAO.isCollaboratorAssignedToProject(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not assigned to this project.");
                return;
            }

            Task task = taskAssigneeDAO.getAssignedTaskDetails(user.getUsername(), taskId);
            if (task == null) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid task selection.");
                return;
            }

            if (month < task.getStart_month() || month > task.getEnd_month()) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Selected month is outside the task interval.");
                return;
            }

            workedHoursDAO.saveOrUpdateWorkedHours(taskId, user.getUsername(), month, hours);
            Task updatedTask = taskAssigneeDAO.getAssignedTaskDetails(user.getUsername(), taskId);

            Map<String, Object> payload = new HashMap<>();
            payload.put("success", true);
            payload.put("message", "Worked hours saved successfully.");
            payload.put("updatedTask", updatedTask);

            sendJson(resp, HttpServletResponse.SC_OK, payload);
        } catch (IllegalArgumentException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to save worked hours right now.");
        } finally {
            closeQuietly(connection);
        }
    }

    private Map<String, Object> readJsonBody(HttpServletRequest req) throws IOException {
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }
        return gson.fromJson(json.toString(), Map.class);
    }

    private int getInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Invalid numeric payload.");
    }

    private void sendJson(HttpServletResponse resp, int status, Object payload) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }

    private void sendJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", false);
        payload.put("error", message);
        sendJson(resp, status, payload);
    }

    private void closeQuietly(Connection connection) {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException ignore) { }
    }
}
