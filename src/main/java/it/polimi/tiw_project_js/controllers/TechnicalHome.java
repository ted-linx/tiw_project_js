package it.polimi.tiw_project_js.controllers;

import it.polimi.tiw_project_js.beans.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/technical-home")
public class TechnicalHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("user");

        if (user.getRole() == User.Role.ADMINISTRATIVE) {
            resp.sendRedirect(req.getContextPath() + "/admin-home");
            return;
        }

        if (user.isManager() && user.isAssignee()) {
            resp.sendRedirect(req.getContextPath() + "/choose-role");
        } else if (user.isManager()) {
            resp.sendRedirect(req.getContextPath() + "/manager-home");
        } else if (user.isAssignee()) {
            resp.sendRedirect(req.getContextPath() + "/assignee-home");
        } else {
            resp.sendRedirect(req.getContextPath() + "/no-assignments");
        }
    }
}
