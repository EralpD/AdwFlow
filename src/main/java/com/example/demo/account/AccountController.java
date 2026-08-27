package com.example.demo.account;

import com.example.demo.security.AccountLoginSuccessHandler;
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
    private final UserAccountService accounts;

    public AccountController(UserAccountService accounts) { this.accounts = accounts; }

    @InitBinder("registration")
    void registrationFields(WebDataBinder binder) {
        binder.setAllowedFields("displayName", "email", "password", "confirmPassword");
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
            Authentication authentication) {
        if (authentication != null) return "redirect:" + AccountLoginSuccessHandler.landingPage(authentication);
        if (!errors.hasErrors()) {
            try {
                accounts.register(form);
                return "redirect:/login?registered";
            } catch (DuplicateEmailException | DataIntegrityViolationException exception) {
                // This handler is outside the service transaction, including when a unique-key race rolls it back.
                errors.reject("registration.failed", "We couldn't create an account with those details. Try another email or log in.");
            }
        }
        form.setPassword("");
        form.setConfirmPassword("");
        return "register";
    }

    @GetMapping("/admin")
    String admin() { return "admin"; }

    @RequestMapping("/error/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String forbidden() { return "error/403"; }
}
