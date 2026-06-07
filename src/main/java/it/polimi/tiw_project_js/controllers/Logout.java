package it.polimi.tiw_project_js.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/logout")
public class Logout extends HttpServlet {
    public Logout() {
        super();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        if(session.isNew() || session.getAttribute("user") == null){
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        req.getSession().invalidate();
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
