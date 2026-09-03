package com.example.demo.account;

import com.example.demo.security.temporary.TemporarySecurityUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AccountFlowControllerTest {
    private UserAccountService accounts;
    private AccountRecoveryService recovery;
    private AccountDeliveryService delivery;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        accounts = mock(UserAccountService.class);
        recovery = mock(AccountRecoveryService.class);
        delivery = mock(AccountDeliveryService.class);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = standaloneSetup(new AccountController(accounts, recovery, delivery))
                .setValidator(validator)
                .build();
    }

    @Test
    void registrationIssuesCodeStoresChallengeAndRedirectsToVerification() throws Exception {
        var account = new UserAccount("Ada", "ada@example.com", "{bcrypt}hash", Role.USER);
        when(accounts.register(any())).thenReturn(account);
        when(delivery.sendEmailVerification("ada@example.com", "127.0.0.1"))
                .thenReturn(new AccountDeliveryService.DispatchResult("a".repeat(43), true));

        mvc.perform(post("/register").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("displayName", "Ada")
                        .param("email", "ADA@example.com")
                        .param("password", "correct horse battery staple")
                        .param("confirmPassword", "correct horse battery staple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-email?sent"))
                .andExpect(request().sessionAttribute(
                        "com.example.demo.account.AccountController.verificationChallenge", "a".repeat(43)))
                .andExpect(request().sessionAttribute(
                        "com.example.demo.account.AccountController.verificationEmail", "ada@example.com"));
    }

    @Test
    void registrationStillRedirectsWhenMailDeliveryIsNotConfigured() throws Exception {
        var account = new UserAccount("Ada", "ada@example.com", "{bcrypt}hash", Role.USER);
        when(accounts.register(any())).thenReturn(account);
        when(delivery.sendEmailVerification(anyString(), anyString()))
                .thenReturn(new AccountDeliveryService.DispatchResult("b".repeat(43), false));

        mvc.perform(post("/register")
                        .param("displayName", "Ada")
                        .param("email", "ada@example.com")
                        .param("password", "correct horse battery staple")
                        .param("confirmPassword", "correct horse battery staple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-email?deliveryError"));
    }

    @Test
    void correctVerificationCodeRedirectsToLogin() throws Exception {
        when(recovery.verifyEmail("c".repeat(43), "042817", "127.0.0.1")).thenReturn(true);

        mvc.perform(post("/verify-email")
                        .param("challenge", "c".repeat(43))
                        .param("code", "042817"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verified"));
    }

    @Test
    void forgotPasswordAlwaysReturnsTheGenericSuccessRedirect() throws Exception {
        doThrow(new TemporarySecurityUnavailableException()).when(delivery)
                .sendPasswordReset("unknown@example.com", "127.0.0.1");

        mvc.perform(post("/forgot-password").param("email", "unknown@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password?requested"));
    }

    @Test
    void validSingleUseResetRedirectsToLogin() throws Exception {
        when(delivery.resetPassword("d".repeat(43), "e".repeat(43),
                "correct horse battery staple", "correct horse battery staple", "127.0.0.1")).thenReturn(true);

        mvc.perform(post("/reset-password")
                        .param("challenge", "d".repeat(43))
                        .param("token", "e".repeat(43))
                        .param("password", "correct horse battery staple")
                        .param("confirmPassword", "correct horse battery staple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordReset"));
    }
}
