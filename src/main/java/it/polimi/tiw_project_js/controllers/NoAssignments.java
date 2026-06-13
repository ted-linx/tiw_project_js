package it.polimi.tiw_project_js.controllers;

import it.polimi.tiw_project_js.beans.User;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/no-assignments")
public class NoAssignments extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");

        req.setAttribute("user", user);
        RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/no_assignments.jsp");
        dispatcher.forward(req, resp);
    }
}
