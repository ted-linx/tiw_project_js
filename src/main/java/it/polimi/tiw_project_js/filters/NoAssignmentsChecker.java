package it.polimi.tiw_project_js.filters;

import it.polimi.tiw_project_js.beans.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = "/no-assignments")
public class NoAssignmentsChecker implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        System.out.println("NoAssignmentsChecker checker executing");

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession();

        User user = (User) session.getAttribute("user");

        if(user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (user.getRole() != User.Role.TECHNICAL) {
            res.sendRedirect(req.getContextPath() + "/admin-home");
            return;
        }

        if (user.isManager() || user.isAssignee()) {
            res.sendRedirect(req.getContextPath() + "/technical-home");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

