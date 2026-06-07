package it.polimi.tiw_project_js.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.polimi.tiw_project_js.beans.Project;
import it.polimi.tiw_project_js.beans.User;
import it.polimi.tiw_project_js.dao.ProjectDAO;
import it.polimi.tiw_project_js.dao.UserDAO;
import it.polimi.tiw_project_js.utils.ConnectionHandler;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin-home")
public class AdminHome extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");

        String action = req.getParameter("action");
        System.out.println(action);

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

                List<Project> createdProjects = projectDAO.getProjectByStatusAndCreator(Project.Status.CREATED, user.getUsername());

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
                result.add("createdProjects", gson.toJsonTree(createdProjects));

                writeJson(resp, HttpServletResponse.SC_OK, result);
                return;

            } catch (SQLException e) {
                sendJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + escapeJson(e.getMessage()));
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
        String action = req.getParameter("action");
        if (action == null || action.isBlank()) {
            sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing action");
            return;
        }

        switch (action) {
            case "save" -> sendJsonError(resp, HttpServletResponse.SC_NOT_IMPLEMENTED,
                    "Save endpoint not implemented yet: UI refactor applied, persistence endpoint still to complete.");
            default -> sendJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
        }
    }

    private void sendJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        writeJson(resp, status, "{\"error\":\"" + escapeJson(message) + "\"}");
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
