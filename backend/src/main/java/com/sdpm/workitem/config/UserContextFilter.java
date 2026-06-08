package com.sdpm.workitem.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) request;
            String user = req.getHeader("X-User");
            if (user != null && !user.isBlank()) {
                UserContext.setOperator(user.trim());
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}