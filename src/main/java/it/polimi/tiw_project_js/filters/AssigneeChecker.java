package it.polimi.tiw_project_js.filters;

import it.polimi.tiw_project_js.beans.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(urlPatterns = {
        "/assignee-home"
})
public class AssigneeChecker implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        User user = (User) req.getSession().getAttribute("user");

        if(user == null) {
            chain.doFilter(request, response);
            return;
        }

        if (user.getRole() == User.Role.ADMINISTRATIVE) {
            res.sendRedirect(req.getContextPath() + "/admin-home");
            return;
        }

        if (!user.isAssignee()) {
            res.sendRedirect(req.getContextPath() + "/technical-home");
            return;
        }

        chain.doFilter(request, response);
    }
}
