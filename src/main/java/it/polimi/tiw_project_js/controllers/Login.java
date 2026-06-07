package it.polimi.tiw_project_js.controllers;

import it.polimi.tiw_project_js.beans.User;
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

@WebServlet("/login")
public class Login extends HttpServlet {

    public Login() {
        super();
    }

    /**
     * GET /login → inoltra internamente a WEB-INF/login.jsp
     * mantenendo /login come URL visibile nel browser.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/login.jsp");
        dispatcher.forward(req, resp);
    }

    /**
     * POST /login → valida le credenziali e risponde con JSON.
     *
     * Risposta 200: { "role": "ADMINISTRATIVE" | "TECHNICAL" }
     * Risposta 400: { "error": "Missing parameters" }
     * Risposta 401: { "error": "Invalid username or password" }
     * Risposta 500: { "error": "<messaggio DB>" }
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, "{\"error\": \"Missing parameters\"}");
            return;
        }

        User user;
        Connection connection = null;
        try {
            connection = ConnectionHandler.getConnection(getServletContext());
            UserDAO userDAO = new UserDAO(connection);
            user = userDAO.checkCredentials(username, password);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, "{\"error\": \"Database error: " + escapeJson(e.getMessage()) + "\"}");
            return;
        } finally {
            try {
                ConnectionHandler.closeConnection(connection);
            } catch (SQLException ignore) {
            }
        }

        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(resp, "{\"error\": \"Invalid username or password\"}");
            return;
        }

        req.getSession().setAttribute("user", user);
        resp.setStatus(HttpServletResponse.SC_OK);
        writeJson(resp, "{\"role\": \"" + user.getRole().name() + "\"}");
    }

    private void writeJson(HttpServletResponse resp, String json) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print(json);
        out.flush();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
