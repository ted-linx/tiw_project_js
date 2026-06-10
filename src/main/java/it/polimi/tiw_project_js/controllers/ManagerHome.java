package it.polimi.tiw_project_js.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.polimi.tiw_project_js.beans.Project;
import it.polimi.tiw_project_js.beans.Task;
import it.polimi.tiw_project_js.beans.User;
import it.polimi.tiw_project_js.beans.WP;
import it.polimi.tiw_project_js.dao.PlannedHoursDAO;
import it.polimi.tiw_project_js.dao.ProjectDAO;
import it.polimi.tiw_project_js.dao.TaskAssigneeDAO;
import it.polimi.tiw_project_js.dao.TaskDAO;
import it.polimi.tiw_project_js.dao.UserDAO;
import it.polimi.tiw_project_js.dao.WPDAO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/manager-home")
public class ManagerHome extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String action = req.getParameter("action");

        if (action == null || action.isBlank()) {
            req.setAttribute("user", user);
            RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/manager_home.jsp");
            rd.forward(req, resp);
            return;
        }

        switch (action) {
            case "init" -> handleInit(req, resp, user);
            case "wps" -> handleLoadWPs(req, resp, user);
            case "tasks" -> handleLoadTasks(req, resp, user);
            case "taskDetails" -> handleTaskDetails(req, resp, user);
            default -> sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
            case "saveAssignment" -> handleSaveAssignment(req, resp, user);
            case "assignProject" -> handleAssignProject(req, resp, user);
            default -> sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
        }
    }

    private void handleInit(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        Connection connection = null;
        try {
            connection = ConnectionHandler.getConnection(getServletContext());
            ProjectDAO projectDAO = new ProjectDAO(connection);
            UserDAO userDAO = new UserDAO(connection);

            List<Project> assignedProjects = projectDAO.getProjectsByManager(user.getUsername());
            List<User> collaborators = userDAO.getStaffByRole(User.Role.TECHNICAL);

            JsonObject result = new JsonObject();
            result.add("assignedProjects", gson.toJsonTree(assignedProjects));
            result.add("collaborators", gson.toJsonTree(collaborators));

            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleLoadWPs(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String projectIdParam = req.getParameter("project_id");
        if (projectIdParam == null || projectIdParam.isBlank()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing project_id");
            return;
        }

        Connection connection = null;
        try {
            int projectId = Integer.parseInt(projectIdParam);
            connection = ConnectionHandler.getConnection(getServletContext());

            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);

            Project selectedProject = projectDAO.getProjectById(projectId);
            if (selectedProject == null) {
                sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Project not found");
                return;
            }

            if (!projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You cannot access this project");
                return;
            }

            List<WP> projectWPs = wpDAO.getWPsOfProject(projectId);

            JsonObject result = new JsonObject();
            result.add("selectedProject", gson.toJsonTree(selectedProject));
            result.add("projectWPs", gson.toJsonTree(projectWPs));

            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid project_id");
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleLoadTasks(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String projectIdParam = req.getParameter("project_id");
        String wpIdParam = req.getParameter("wp_id");

        if (projectIdParam == null || projectIdParam.isBlank() || wpIdParam == null || wpIdParam.isBlank()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing project_id or wp_id");
            return;
        }

        Connection connection = null;
        try {
            int projectId = Integer.parseInt(projectIdParam);
            int wpId = Integer.parseInt(wpIdParam);

            connection = ConnectionHandler.getConnection(getServletContext());

            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);
            TaskDAO taskDAO = new TaskDAO(connection);

            if (!projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You cannot access this project");
                return;
            }

            if (!wpDAO.projectContainsWP(projectId, wpId)) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "The selected work package does not belong to the selected project");
                return;
            }

            List<Task> wpTasks = taskDAO.getTasksOfWP(wpId);

            JsonObject result = new JsonObject();
            result.add("wpTasks", gson.toJsonTree(wpTasks));

            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ids");
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleTaskDetails(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String taskIdParam = req.getParameter("task_id");

        if (taskIdParam == null || taskIdParam.isBlank()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing task_id");
            return;
        }

        Connection connection = null;
        try {
            int taskId = Integer.parseInt(taskIdParam);

            connection = ConnectionHandler.getConnection(getServletContext());

            TaskDAO taskDAO = new TaskDAO(connection);
            UserDAO userDAO = new UserDAO(connection);

            Task selectedTask = taskDAO.getTaskById(taskId);
            if (selectedTask == null) {
                sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Task not found");
                return;
            }

            List<User> collaborators = userDAO.getStaffByRole(User.Role.TECHNICAL);

            JsonObject result = new JsonObject();
            result.add("selectedTask", gson.toJsonTree(selectedTask));
            result.add("collaborators", gson.toJsonTree(collaborators));

            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid task_id");
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleSaveAssignment(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String projectIdParam = req.getParameter("project_id");
        String wpIdParam = req.getParameter("wp_id");
        String taskIdParam = req.getParameter("task_id");
        String collaborator = req.getParameter("collaborator");

        System.out.println("[saveAssignment] project_id=" + projectIdParam
                + ", wp_id=" + wpIdParam
                + ", task_id=" + taskIdParam
                + ", collaborator=" + collaborator);

        List<String> missingFields = new ArrayList<>();
        if (projectIdParam == null || projectIdParam.isBlank()) missingFields.add("project_id");
        if (wpIdParam == null || wpIdParam.isBlank()) missingFields.add("wp_id");
        if (taskIdParam == null || taskIdParam.isBlank()) missingFields.add("task_id");
        if (collaborator == null || collaborator.isBlank()) missingFields.add("collaborator");

        if (!missingFields.isEmpty()) {
            JsonObject result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("error", "Missing required fields");
            result.add("missingFields", gson.toJsonTree(missingFields));
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, result);
            return;
        }

        Connection connection = null;
        try {
            int projectId = Integer.parseInt(projectIdParam);
            int wpId = Integer.parseInt(wpIdParam);
            int taskId = Integer.parseInt(taskIdParam);

            connection = ConnectionHandler.getConnection(getServletContext());
            connection.setAutoCommit(false);

            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);
            TaskDAO taskDAO = new TaskDAO(connection);
            TaskAssigneeDAO taskAssigneeDAO = new TaskAssigneeDAO(connection);
            PlannedHoursDAO plannedHoursDAO = new PlannedHoursDAO(connection);

            Map<String, String> invalidFields = new LinkedHashMap<>();

            if (!projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                invalidFields.put("project_id", "You cannot edit this project");
            }

            if (!wpDAO.projectContainsWP(projectId, wpId)) {
                invalidFields.put("wp_id", "The selected work package does not belong to the selected project");
            }

            if (!taskDAO.wpContainsTask(wpId, taskId)) {
                invalidFields.put("task_id", "The selected task does not belong to the selected work package");
            }

            Task task = taskDAO.getTaskById(taskId);
            if (task == null) {
                invalidFields.put("task_id", "Task not found");
            }

            if (user.getUsername().equals(collaborator)) {
                invalidFields.put("collaborator", "Self-assignment is not allowed");
            }

            if (!invalidFields.isEmpty()) {
                rollbackQuietly(connection);
                JsonObject result = new JsonObject();
                result.addProperty("success", false);
                result.addProperty("error", "Validation failed");
                result.add("invalidFields", gson.toJsonTree(invalidFields));
                writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, result);
                return;
            }

            // QUI devi usare il nome esatto del tuo metodo DAO:
            // taskAssigneeDAO.assignCollaboratorToTask(taskId, collaborator);
            // oppure taskAssigneeDAO.replaceAssigneesOfTask(taskId, List.of(collaborator));

            Map<String, String> hourErrors = new LinkedHashMap<>();

            for (int month = task.getStart_month(); month <= task.getEnd_month(); month++) {
                String paramName = "m" + month;
                String value = req.getParameter(paramName);

                int hours = 0;
                if (value != null && !value.isBlank()) {
                    hours = Integer.parseInt(value);
                    if (hours < 0) {
                        hourErrors.put(paramName, "Hours cannot be negative");
                        continue;
                    }
                }

                // QUI devi usare il nome esatto del tuo metodo DAO:
                // plannedHoursDAO.saveOrUpdate(taskId, month, hours);
            }

            if (!hourErrors.isEmpty()) {
                rollbackQuietly(connection);
                JsonObject result = new JsonObject();
                result.addProperty("success", false);
                result.addProperty("error", "Validation failed");
                result.add("invalidFields", gson.toJsonTree(hourErrors));
                writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, result);
                return;
            }

            connection.commit();

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Assignment saved successfully");

            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            rollbackQuietly(connection);
            JsonObject result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("error", "Invalid numeric value");
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, result);
        } catch (IllegalArgumentException e) {
            rollbackQuietly(connection);
            JsonObject result = new JsonObject();
            result.addProperty("success", false);
            result.addProperty("error", e.getMessage());
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, result);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            resetAutocommitAndClose(connection);
        }
    }

    private void handleAssignProject(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String projectIdParam = req.getParameter("project_id");
        if (projectIdParam == null || projectIdParam.isBlank()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing project_id");
            return;
        }

        Connection connection = null;
        try {
            int projectId = Integer.parseInt(projectIdParam);

            connection = ConnectionHandler.getConnection(getServletContext());
            connection.setAutoCommit(false);

            ProjectDAO projectDAO = new ProjectDAO(connection);

            Project project = projectDAO.getProjectById(projectId);
            if (project == null) {
                sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Project not found");
                return;
            }

            if (!projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You cannot assign this project");
                return;
            }

            if (!projectDAO.canBeAssigned(projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "All tasks must have collaborators and planned hours before assigning the project");
                return;
            }

            projectDAO.assignProject(projectId);

            connection.commit();

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Project assigned successfully");

            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            rollbackQuietly(connection);
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid project_id");
        } catch (SQLException e) {
            rollbackQuietly(connection);
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            resetAutocommitAndClose(connection);
        }
    }

    private void writeJson(HttpServletResponse resp, int status, Object payload) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }

    private void sendJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        writeJson(resp, status, error);
    }

    private void closeQuietly(Connection connection) {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException ignore) {
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) return;
        try {
            connection.rollback();
        } catch (SQLException ignore) {
        }
    }

    private void resetAutocommitAndClose(Connection connection) {
        if (connection == null) return;
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignore) {
        }
        closeQuietly(connection);
    }
}
