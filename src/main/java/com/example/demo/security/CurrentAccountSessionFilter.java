package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Invalidates stale sessions on their next request, including admin and otherwise public pages. */
public final class CurrentAccountSessionFilter extends OncePerRequestFilter {
    private final CurrentAccountAccess accounts;
    public CurrentAccountSessionFilter(CurrentAccountAccess accounts) { this.accounts = accounts; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal) {
            try {
                if (!accounts.isAllowed(authentication)) {
                    SecurityContextHolder.clearContext();
                    var session = request.getSession(false);
                    if (session != null) session.invalidate();
                }
            } catch (DataAccessException exception) {
                response.setStatus(503);
                response.setHeader("Cache-Control", "no-store");
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Account verification is temporarily unavailable.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
