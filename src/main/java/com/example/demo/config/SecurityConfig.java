package com.example.demo.config;

import com.example.demo.security.AccountDetailsService;
import com.example.demo.security.AccountLoginSuccessHandler;
import com.example.demo.security.CurrentAccountAccess;
import com.example.demo.security.CurrentAccountSessionFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(12)));
    }

    @Bean
    HttpSessionRequestCache requestCache() {
        var cache = new HttpSessionRequestCache();
        cache.setRequestMatcher(request -> "GET".equals(request.getMethod())
                && Set.of(request.getContextPath() + "/dashboard", request.getContextPath() + "/dashboard/generate",
                        request.getContextPath() + "/generate", request.getContextPath() + "/admin")
                        .contains(request.getRequestURI()));
        return cache;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountDetailsService users,
            PasswordEncoder passwords, HttpSessionRequestCache requestCache, CurrentAccountAccess currentAccount) throws Exception {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwords);
        var htmlLogin = new LoginUrlAuthenticationEntryPoint("/login");
        var htmlDenied = new AccessDeniedHandlerImpl();
        htmlDenied.setErrorPage("/error/403");

        http.authenticationProvider(provider)
                .addFilterBefore(new CurrentAccountSessionFilter(currentAccount), AuthorizationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/login", "/register", "/verify-email",
                                "/forgot-password", "/reset-password", "/error", "/error/403",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login", "/register", "/verify-email",
                                "/verify-email/resend", "/forgot-password", "/reset-password").permitAll()
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/dashboard", "/dashboard/**", "/generate", "/api/advertisements/**")
                            .access((authentication, context) -> new AuthorizationDecision(
                                    currentAccount.isAllowed(authentication.get())))
                        .anyRequest().denyAll())
                .csrf(Customizer.withDefaults())
                .requestCache(cache -> cache.requestCache(requestCache))
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                        .usernameParameter("email").passwordParameter("password")
                        .failureUrl("/login?error")
                        .successHandler(new AccountLoginSuccessHandler(requestCache)))
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (isApi(request)) {
                                jsonError(response, 401, "AUTHENTICATION_REQUIRED",
                                        "Your session has ended. Log in again to continue.");
                            } else {
                                htmlLogin.commence(request, response, exception);
                            }
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            if (isApi(request)) {
                                boolean csrf = exception instanceof CsrfException;
                                jsonError(response, 403, csrf ? "CSRF_INVALID" : "ACCESS_DENIED", csrf
                                        ? "Your security token has expired. Refresh the page or log in again."
                                        : "You do not have permission to perform this action.");
                            } else {
                                htmlDenied.handle(request, response, exception);
                            }
                        }));
        return http.build();
    }

    private static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    private static void jsonError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // These values are fixed server messages, never user-controlled strings.
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
