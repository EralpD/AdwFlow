package com.example.demo.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import java.io.IOException;

public final class AccountLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final RequestCache requestCache;

    public AccountLoginSuccessHandler(RequestCache requestCache) {
        this.requestCache = requestCache;
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public static String landingPage(Authentication authentication) {
        return isAdmin(authentication) ? "/admin" : "/";
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String target = landingPage(authentication);
        var saved = requestCache.getRequest(request, response);
        if (saved instanceof DefaultSavedRequest original && "GET".equals(original.getMethod())) {
            // Never redirect to a saved Host header, an arbitrary URL, or an untrusted query parameter.
            String path = original.getRequestURI();
            String context = request.getContextPath();
            if (path.equals(context + "/dashboard")) {
                target = "/dashboard";
            } else if (path.equals(context + "/dashboard/generate") || path.equals(context + "/generate")) {
                target = "/dashboard/generate";
            } else if (path.equals(context + "/admin") && isAdmin(authentication)) {
                target = "/admin";
            }
        }
        requestCache.removeRequest(request, response);
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
