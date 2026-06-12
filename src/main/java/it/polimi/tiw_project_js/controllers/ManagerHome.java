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
import it.polimi.tiw_project_js.dao.WorkedHoursDAO;
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
            case "projectMonitoringList" -> handleProjectMonitoringList(resp, user);
            case "projectMonitoring" -> handleProjectMonitoring(req, resp, user);
            case "collaboratorMonitoringList" -> handleCollaboratorMonitoringList(resp, user);
            case "collaboratorMonitoring" -> handleCollaboratorMonitoring(req, resp, user);
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
            case "completeProject" -> handleCompleteProject(req, resp, user);
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

            if (!projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not the manager of this project");
                return;
            }

            Project selectedProject = projectDAO.getProjectById(projectId);
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
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not the manager of this project");
                return;
            }
            if (!wpDAO.projectContainsWP(projectId, wpId)) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Selected work package does not belong to the project");
                return;
            }

            JsonObject result = new JsonObject();
            result.add("wpTasks", gson.toJsonTree(taskDAO.getTasksOfWP(wpId)));
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid project_id or wp_id");
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
            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);

            Task selectedTask = taskDAO.getTaskById(taskId);
            if (selectedTask == null) {
                sendJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Task not found");
                return;
            }

            WP wp = wpDAO.getWPById(selectedTask.getWp_id());
            if (wp == null || !projectDAO.userIsProjectManager(user.getUsername(), wp.getProject_id())) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not allowed to view this task");
                return;
            }

            JsonObject result = new JsonObject();
            result.add("selectedTask", gson.toJsonTree(selectedTask));
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid task_id");
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleProjectMonitoringList(HttpServletResponse resp, User user) throws ServletException, IOException {
        Connection connection = null;
        try {
            connection = ConnectionHandler.getConnection(getServletContext());
            ProjectDAO projectDAO = new ProjectDAO(connection);

            JsonObject result = new JsonObject();
            result.add("projects", gson.toJsonTree(projectDAO.getProjectsByManager(user.getUsername())));
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleProjectMonitoring(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
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

            if (!projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not the manager of this project");
                return;
            }

            Project project = projectDAO.getProjectById(projectId);
            JsonObject result = new JsonObject();
            result.add("project", gson.toJsonTree(project));
            result.add("months", gson.toJsonTree(buildMonths(project.getDuration())));
            result.add("wps", gson.toJsonTree(enrichWps(project.getWorkPackages())));
            result.addProperty("canBeConcluded", projectDAO.canBeConcluded(projectId));
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid project_id");
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleCollaboratorMonitoringList(HttpServletResponse resp, User user) throws ServletException, IOException {
        Connection connection = null;
        try {
            connection = ConnectionHandler.getConnection(getServletContext());
            UserDAO userDAO = new UserDAO(connection);

            List<Map<String, Object>> collaboratorRows = new ArrayList<>();
            for (User collaborator : userDAO.getCollaboratorsByManager(user.getUsername())) {
                Map<String, Object> collaboratorMap = new LinkedHashMap<>();
                collaboratorMap.put("username", collaborator.getUsername());
                collaboratorMap.put("firstName", collaborator.getFirstName());
                collaboratorMap.put("lastName", collaborator.getLastName());
                collaboratorMap.put("fullName", collaborator.getFullName());
                collaboratorRows.add(collaboratorMap);
            }

            JsonObject result = new JsonObject();
            result.add("collaborators", gson.toJsonTree(collaboratorRows));
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleCollaboratorMonitoring(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String collaboratorParam = req.getParameter("username");
        if (collaboratorParam == null || collaboratorParam.isBlank()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing username");
            return;
        }

        Connection connection = null;
        try {
            connection = ConnectionHandler.getConnection(getServletContext());
            ProjectDAO projectDAO = new ProjectDAO(connection);
            UserDAO userDAO = new UserDAO(connection);
            TaskAssigneeDAO taskAssigneeDAO = new TaskAssigneeDAO(connection);
            WorkedHoursDAO workedHoursDAO = new WorkedHoursDAO(connection);

            List<User> collaborators = userDAO.getCollaboratorsByManager(user.getUsername());
            User selectedCollaborator = collaborators.stream()
                    .filter(c -> c.getUsername().equals(collaboratorParam))
                    .findFirst()
                    .orElse(null);
            if (selectedCollaborator == null) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not allowed to inspect this collaborator");
                return;
            }

            List<Project> assignedProjects = taskAssigneeDAO.getAssignedProjectsOfCollaborator(collaboratorParam);
            List<Map<String, Object>> projectRows = new ArrayList<>();

            for (Project assignedProject : assignedProjects) {
                if (!projectDAO.userIsProjectManager(user.getUsername(), assignedProject.getId())) {
                    continue;
                }

                Map<Integer, List<Task>> tasksByWp = taskAssigneeDAO.getAssignedTasksByWpInProject(collaboratorParam, assignedProject.getId());
                List<WP> assignedWps = taskAssigneeDAO.getAssignedWPsOfCollaboratorInProject(collaboratorParam, assignedProject.getId());
                List<Map<String, Object>> wpRows = new ArrayList<>();

                for (WP wp : assignedWps) {
                    List<Task> assignedTasks = tasksByWp.getOrDefault(wp.getId(), List.of());
                    List<Map<String, Object>> taskRows = new ArrayList<>();
                    Map<Integer, Integer> wpWorked = zeroMap(assignedProject.getDuration());

                    for (Task task : assignedTasks) {
                        Map<Integer, Integer> worked = workedHoursDAO.getWorkedHoursOfTaskForCollaborator(
                                task.getId(),
                                collaboratorParam,
                                task.getStart_month(),
                                task.getEnd_month()
                        );

                        addInto(wpWorked, worked);

                        Map<String, Object> taskMap = new LinkedHashMap<>();
                        taskMap.put("id", task.getId());
                        taskMap.put("order_number", task.getOrder_number());
                        taskMap.put("title", task.getTitle());
                        taskMap.put("description", task.getDescription());
                        taskMap.put("start_month", task.getStart_month());
                        taskMap.put("end_month", task.getEnd_month());
                        taskMap.put("worked_hours", worked);
                        taskRows.add(taskMap);
                    }

                    Map<String, Object> wpMap = new LinkedHashMap<>();
                    wpMap.put("id", wp.getId());
                    wpMap.put("order_number", wp.getOrder_number());
                    wpMap.put("title", wp.getTitle());
                    wpMap.put("start_month", wp.getStart_month());
                    wpMap.put("end_month", wp.getEnd_month());
                    wpMap.put("workedHours", wpWorked);
                    wpMap.put("tasks", taskRows);
                    wpRows.add(wpMap);
                }

                Map<String, Object> projectMap = new LinkedHashMap<>();
                projectMap.put("id", assignedProject.getId());
                projectMap.put("title", assignedProject.getTitle());
                projectMap.put("duration", assignedProject.getDuration());
                projectMap.put("status", assignedProject.getStatus());
                projectMap.put("months", buildMonths(assignedProject.getDuration()));
                projectMap.put("wps", wpRows);
                projectRows.add(projectMap);
            }

            JsonObject result = new JsonObject();
            result.addProperty("username", selectedCollaborator.getUsername());
            result.addProperty("fullName", selectedCollaborator.getFullName());
            result.add("selectedCollaborator", gson.toJsonTree(selectedCollaborator));
            result.add("projects", gson.toJsonTree(projectRows));
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleSaveAssignment(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        Map<String, String> invalid = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        String projectIdParam = trim(req.getParameter("project_id"));
        String wpIdParam = trim(req.getParameter("wp_id"));
        String taskIdParam = trim(req.getParameter("task_id"));
        String[] collaboratorParams = req.getParameterValues("collaborators");

        if (projectIdParam == null) missing.add("project_id");
        if (wpIdParam == null) missing.add("wp_id");
        if (taskIdParam == null) missing.add("task_id");
        if (collaboratorParams == null || collaboratorParams.length == 0) missing.add("collaborators");

        Connection connection = null;

        try {
            Integer projectId = parsePositiveInt(projectIdParam, "project_id", invalid);
            Integer wpId = parsePositiveInt(wpIdParam, "wp_id", invalid);
            Integer taskId = parsePositiveInt(taskIdParam, "task_id", invalid);

            connection = ConnectionHandler.getConnection(getServletContext());
            ProjectDAO projectDAO = new ProjectDAO(connection);
            WPDAO wpDAO = new WPDAO(connection);
            TaskDAO taskDAO = new TaskDAO(connection);
            UserDAO userDAO = new UserDAO(connection);
            PlannedHoursDAO plannedHoursDAO = new PlannedHoursDAO(connection);

            if (projectId != null && !projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                invalid.put("project_id", "You are not the manager of this project");
            }
            if (projectId != null && wpId != null && !wpDAO.projectContainsWP(projectId, wpId)) {
                invalid.put("wp_id", "Selected work package does not belong to the chosen project");
            }

            Task task = null;
            if (taskId != null) {
                task = taskDAO.getTaskById(taskId);
                if (task == null) {
                    invalid.put("task_id", "Selected task does not exist");
                } else if (wpId != null && task.getWp_id() != wpId) {
                    invalid.put("task_id", "Selected task does not belong to the chosen work package");
                }
            }

            List<String> collaborators = new ArrayList<>();
            if (collaboratorParams != null) {
                for (String collaboratorParam : collaboratorParams) {
                    String collaborator = trim(collaboratorParam);
                    if (collaborator == null) {
                        continue;
                    }

                    User collaboratorUser = userDAO.getUserByUsername(collaborator);
                    if (collaboratorUser == null) {
                        invalid.put("collaborators", "One or more selected collaborators do not exist");
                        break;
                    }
                    if (collaborator.equals(user.getUsername())) {
                        invalid.put("collaborators", "Self-assignment is not allowed");
                        break;
                    }
                    if (!collaborators.contains(collaborator)) {
                        collaborators.add(collaborator);
                    }
                }
            }

            Map<Integer, Integer> hoursByMonth = new LinkedHashMap<>();
            if (task != null) {
                for (int month = task.getStart_month(); month <= task.getEnd_month(); month++) {
                    String paramName = "m" + month;
                    String rawValue = trim(req.getParameter(paramName));
                    if (rawValue == null) {
                        missing.add(paramName);
                        continue;
                    }

                    try {
                        int hours = Integer.parseInt(rawValue);
                        if (hours < 0) {
                            invalid.put(paramName, "Hours must be greater than or equal to zero");
                        } else {
                            hoursByMonth.put(month, hours);
                        }
                    } catch (NumberFormatException e) {
                        invalid.put(paramName, "Hours must be an integer value");
                    }
                }
            }

            if (!missing.isEmpty() || !invalid.isEmpty()) {
                sendValidationError(resp, missing, invalid);
                return;
            }

            for (Map.Entry<Integer, Integer> entry : hoursByMonth.entrySet()) {
                plannedHoursDAO.saveOrUpdatePlannedHours(taskId, entry.getKey(), entry.getValue());
            }

            taskDAO.deleteAssignments(taskId);
            for (String collaboratorUsername : collaborators) {
                taskDAO.createAssignment(taskId, collaboratorUsername);
            }

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Assignment saved successfully");
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleAssignProject(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String projectIdParam = trim(req.getParameter("project_id"));
        if (projectIdParam == null) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing project_id");
            return;
        }

        Connection connection = null;
        try {
            int projectId = Integer.parseInt(projectIdParam);
            connection = ConnectionHandler.getConnection(getServletContext());
            ProjectDAO projectDAO = new ProjectDAO(connection);

            if (!projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not the manager of this project");
                return;
            }
            if (!projectDAO.canBeAssigned(projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "The project cannot be assigned yet. Make sure every task has planned hours.");
                return;
            }

            projectDAO.assignProject(projectId);

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Project assigned successfully");
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid project_id");
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private void handleCompleteProject(HttpServletRequest req, HttpServletResponse resp, User user) throws ServletException, IOException {
        String projectIdParam = trim(req.getParameter("project_id"));
        if (projectIdParam == null) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing project_id");
            return;
        }

        Connection connection = null;
        try {
            int projectId = Integer.parseInt(projectIdParam);
            connection = ConnectionHandler.getConnection(getServletContext());
            ProjectDAO projectDAO = new ProjectDAO(connection);

            if (!projectDAO.userIsProjectManager(user.getUsername(), projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You are not the manager of this project");
                return;
            }
            if (!projectDAO.canBeConcluded(projectId)) {
                sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "The project cannot be concluded yet.");
                return;
            }

            projectDAO.completeProject(projectId);

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "Project concluded successfully");
            writeJson(resp, HttpServletResponse.SC_OK, result);
        } catch (NumberFormatException e) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid project_id");
        } catch (SQLException e) {
            sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private List<Map<String, Object>> enrichWps(List<WP> wps) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (WP wp : wps) {
            Map<String, Object> wpMap = new LinkedHashMap<>();
            wpMap.put("id", wp.getId());
            wpMap.put("order_number", wp.getOrder_number());
            wpMap.put("title", wp.getTitle());
            wpMap.put("plannedHours", wp.getTotalPlannedHours());
            wpMap.put("workedHours", wp.getTotalWorkedHours());
            wpMap.put("tasks", wp.getTasks());
            result.add(wpMap);
        }
        return result;
    }

    private Map<Integer, Integer> zeroMap(int duration) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int month = 1; month <= duration; month++) {
            result.put(month, 0);
        }
        return result;
    }

    private void addInto(Map<Integer, Integer> target, Map<Integer, Integer> source) {
        if (source == null) return;
        for (Map.Entry<Integer, Integer> entry : source.entrySet()) {
            target.put(entry.getKey(), target.getOrDefault(entry.getKey(), 0) + (entry.getValue() == null ? 0 : entry.getValue()));
        }
    }

    private List<Integer> buildMonths(int duration) {
        List<Integer> months = new ArrayList<>();
        for (int i = 1; i <= duration; i++) months.add(i);
        return months;
    }

    private Integer parsePositiveInt(String raw, String field, Map<String, String> invalid) {
        if (raw == null) return null;
        try {
            int value = Integer.parseInt(raw);
            if (value <= 0) {
                invalid.put(field, "Value must be greater than zero");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            invalid.put(field, "Value must be an integer");
            return null;
        }
    }

    private String trim(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void sendValidationError(HttpServletResponse resp, List<String> missing, Map<String, String> invalid) throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.add("missingFields", gson.toJsonTree(missing));
        result.add("invalidFields", gson.toJsonTree(invalid));
        writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, result);
    }

    private void sendJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("error", message);
        writeJson(resp, status, result);
    }

    private void writeJson(HttpServletResponse resp, int status, JsonObject jsonObject) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(jsonObject));
    }

    private void closeQuietly(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
