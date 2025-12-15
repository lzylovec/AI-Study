package com.study.kgraph.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession();
        if (session.getAttribute("userId") != null) {
            return true;
        }

        // Check if AJAX or API path
        String requestedWith = request.getHeader("X-Requested-With");
        String requestUri = request.getRequestURI();

        if ("XMLHttpRequest".equals(requestedWith) ||
                (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json")) ||
                requestUri.startsWith("/api/")) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"未登录\", \"redirect\": \"/\"}");
            return false;
        }

        // Redirect to login
        response.sendRedirect("/");
        return false;
    }
}
