package it.polimi.tiw_project_js.filters;

import it.polimi.tiw_project_js.beans.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = {
        "/admin-home"
})
public class AdminChecker implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("Admin filter executing ..");
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String loginPath = req.getServletContext().getContextPath() + "/login";

        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        if(user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (user.getRole() != User.Role.ADMINISTRATIVE) {
            res.sendRedirect(req.getContextPath() + "/technical-home");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
