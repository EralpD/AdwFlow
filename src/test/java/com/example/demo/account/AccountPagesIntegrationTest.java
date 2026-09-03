package com.example.demo.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:account-pages;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.openai.api-key=test-key",
        "app.security.redis.hmac-secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.redis.verify-startup=false",
        "management.otlp.metrics.export.enabled=false",
        "management.tracing.export.otlp.enabled=false",
        "management.opentelemetry.logging.export.enabled=false"
})
@AutoConfigureMockMvc
class AccountPagesIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean AccountDeliveryService delivery;
    @MockitoBean AccountRecoveryService recovery;

    @Test
    void publicAccountPagesRenderAndCarrySecurityHeaders() throws Exception {
        mvc.perform(get("/verify-email"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Enter your six-digit code")));
        mvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Send reset link")));
        mvc.perform(get("/reset-password")
                        .param("challenge", "c".repeat(43))
                        .param("token", "t".repeat(43)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Change password")));
    }

    @Test
    void accountMutationEndpointsRequireCsrf() throws Exception {
        mvc.perform(post("/forgot-password").param("email", "ada@example.com"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/verify-email").param("challenge", "c".repeat(43)).param("code", "042817"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/reset-password").param("challenge", "c".repeat(43)).param("token", "t".repeat(43))
                        .param("password", "correct horse battery staple")
                        .param("confirmPassword", "correct horse battery staple"))
                .andExpect(status().isForbidden());
    }

    @Test
    void newlyRegisteredAccountIsRedirectedAndCannotLoginUntilVerified() throws Exception {
        when(delivery.sendEmailVerification(anyString(), anyString()))
                .thenReturn(new AccountDeliveryService.DispatchResult("z".repeat(43), true));

        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "Ada Test")
                        .param("email", "new-account@example.com")
                        .param("password", "correct horse battery staple")
                        .param("confirmPassword", "correct horse battery staple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-email?sent"));

        mvc.perform(post("/login").with(csrf())
                        .param("email", "new-account@example.com")
                        .param("password", "correct horse battery staple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}
