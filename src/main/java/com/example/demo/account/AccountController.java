package com.example.demo.account;

import com.example.demo.security.AccountLoginSuccessHandler;
import com.example.demo.security.temporary.TemporarySecurityUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class AccountController {
    private static final String VERIFY_CHALLENGE = AccountController.class.getName() + ".verificationChallenge";
    private static final String VERIFY_EMAIL = AccountController.class.getName() + ".verificationEmail";
    private final UserAccountService accounts;
    private final AccountRecoveryService recovery;
    private final AccountDeliveryService delivery;

    public AccountController(UserAccountService accounts, AccountRecoveryService recovery, AccountDeliveryService delivery) {
        this.accounts = accounts;
        this.recovery = recovery;
        this.delivery = delivery;
    }

    @InitBinder("registration")
    void registrationFields(WebDataBinder binder) {
        binder.setAllowedFields("displayName", "email", "password", "confirmPassword");
    }

    @InitBinder("emailVerification")
    void verificationFields(WebDataBinder binder) { binder.setAllowedFields("challenge", "code"); }

    @InitBinder({"resendVerification", "forgotPassword"})
    void emailRequestFields(WebDataBinder binder) { binder.setAllowedFields("email"); }

    @InitBinder("passwordReset")
    void passwordResetFields(WebDataBinder binder) {
        binder.setAllowedFields("challenge", "token", "password", "confirmPassword");
    }

    @GetMapping("/login")
    String login(Authentication authentication) {
        return authentication == null ? "login" : "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
    }

    @GetMapping("/register")
    String register(Model model, Authentication authentication) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        model.addAttribute("registration", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    String register(@Valid @ModelAttribute("registration") RegistrationForm form, BindingResult errors,
            Authentication authentication, HttpServletRequest request, HttpSession session) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        if (!errors.hasErrors()) {
            try {
                var account = accounts.register(form);
                session.setAttribute(VERIFY_EMAIL, account.getEmail());
                try {
                    var result = delivery.sendEmailVerification(account.getEmail(), clientAddress(request));
                    if (result.issued()) session.setAttribute(VERIFY_CHALLENGE, result.challengeId());
                    return "redirect:/verify-email" + (result.deliveryAccepted() ? "?sent" : "?deliveryError");
                } catch (TemporarySecurityUnavailableException exception) {
                    return "redirect:/verify-email?unavailable";
                }
            } catch (DuplicateEmailException | DataIntegrityViolationException exception) {
                // This handler is outside the service transaction, including when a unique-key race rolls it back.
                errors.reject("registration.failed", "We couldn't create an account with those details. Try another email or log in.");
            }
        }
        form.setPassword("");
        form.setConfirmPassword("");
        return "register";
    }

    @GetMapping("/verify-email")
    String verifyEmail(@RequestParam(required = false) String challenge, Model model,
            Authentication authentication, HttpSession session, HttpServletResponse response) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        noStore(response);
        String selected = validCredential(challenge) ? challenge : sessionValue(session, VERIFY_CHALLENGE);
        if (selected != null) session.setAttribute(VERIFY_CHALLENGE, selected);
        var verification = new EmailVerificationForm();
        verification.setChallenge(selected);
        var resend = new EmailRequestForm();
        resend.setEmail(sessionValue(session, VERIFY_EMAIL));
        model.addAttribute("emailVerification", verification);
        model.addAttribute("resendVerification", resend);
        return "verify-email";
    }

    @PostMapping("/verify-email")
    String verifyEmail(@Valid @ModelAttribute("emailVerification") EmailVerificationForm form, BindingResult errors,
            Model model, Authentication authentication, HttpServletRequest request, HttpSession session,
            HttpServletResponse response) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        noStore(response);
        if (!errors.hasErrors()) {
            try {
                if (recovery.verifyEmail(form.getChallenge(), form.getCode(), clientAddress(request))) {
                    session.removeAttribute(VERIFY_CHALLENGE);
                    session.removeAttribute(VERIFY_EMAIL);
                    return "redirect:/login?verified";
                }
                errors.reject("verification.invalid", "The code is incorrect, expired, or has already been used.");
            } catch (TemporarySecurityUnavailableException exception) {
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                errors.reject("verification.unavailable", "Verification is temporarily unavailable. Please try again.");
            }
        }
        var resend = new EmailRequestForm();
        resend.setEmail(sessionValue(session, VERIFY_EMAIL));
        model.addAttribute("resendVerification", resend);
        return "verify-email";
    }

    @PostMapping("/verify-email/resend")
    String resendVerification(@Valid @ModelAttribute("resendVerification") EmailRequestForm form, BindingResult errors,
            Model model, Authentication authentication, HttpServletRequest request, HttpSession session,
            HttpServletResponse response) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        noStore(response);
        if (errors.hasErrors()) {
            var verification = new EmailVerificationForm();
            verification.setChallenge(sessionValue(session, VERIFY_CHALLENGE));
            model.addAttribute("emailVerification", verification);
            return "verify-email";
        }
        session.setAttribute(VERIFY_EMAIL, form.getEmail());
        try {
            var result = delivery.sendEmailVerification(form.getEmail(), clientAddress(request));
            if (result.issued()) session.setAttribute(VERIFY_CHALLENGE, result.challengeId());
            return "redirect:/verify-email?resent";
        } catch (TemporarySecurityUnavailableException exception) {
            return "redirect:/verify-email?unavailable";
        }
    }

    @GetMapping("/forgot-password")
    String forgotPassword(Model model, Authentication authentication, HttpServletResponse response) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        noStore(response);
        model.addAttribute("forgotPassword", new EmailRequestForm());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    String forgotPassword(@Valid @ModelAttribute("forgotPassword") EmailRequestForm form, BindingResult errors,
            Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        noStore(response);
        if (errors.hasErrors()) return "forgot-password";
        try {
            delivery.sendPasswordReset(form.getEmail(), clientAddress(request));
        } catch (TemporarySecurityUnavailableException ignored) {
            // The public response stays identical so infrastructure state cannot reveal account existence.
        }
        return "redirect:/forgot-password?requested";
    }

    @GetMapping("/reset-password")
    String resetPassword(@RequestParam(required = false) String challenge, @RequestParam(required = false) String token,
            Model model, Authentication authentication, HttpServletResponse response) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        noStore(response);
        var form = new PasswordResetForm();
        form.setChallenge(challenge);
        form.setToken(token);
        model.addAttribute("passwordReset", form);
        model.addAttribute("resetLinkValid", validCredential(challenge) && validCredential(token));
        return "reset-password";
    }

    @PostMapping("/reset-password")
    String resetPassword(@Valid @ModelAttribute("passwordReset") PasswordResetForm form, BindingResult errors,
            Model model, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        noStore(response);
        model.addAttribute("resetLinkValid", true);
        if (errors.hasErrors()) return "reset-password";
        try {
            if (delivery.resetPassword(form.getChallenge(), form.getToken(), form.getPassword(),
                    form.getConfirmPassword(), clientAddress(request))) {
                return "redirect:/login?passwordReset";
            }
            errors.reject("reset.invalid", "This reset link is invalid, expired, or has already been used.");
        } catch (IllegalArgumentException exception) {
            errors.reject("reset.password", "Choose a password that meets all requirements.");
        } catch (TemporarySecurityUnavailableException exception) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            errors.reject("reset.unavailable", "Password reset is temporarily unavailable. Please try again.");
        } finally {
            form.setPassword("");
            form.setConfirmPassword("");
        }
        return "reset-password";
    }

    @GetMapping("/admin")
    String admin() { return "admin"; }

    @RequestMapping("/error/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String forbidden() { return "error/403"; }

    private static String clientAddress(HttpServletRequest request) { return request.getRemoteAddr(); }
    private static boolean validCredential(String value) { return value != null && value.matches("[A-Za-z0-9_-]{43}"); }
    private static String sessionValue(HttpSession session, String name) {
        Object value = session.getAttribute(name);
        return value instanceof String string && !string.isBlank() ? string : null;
    }
    private static void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }
}
