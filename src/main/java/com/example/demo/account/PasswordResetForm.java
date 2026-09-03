package com.example.demo.account;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class PasswordResetForm {
    @NotBlank(message = "The reset link is invalid or has expired.")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "The reset link is invalid or has expired.")
    private String challenge = "";

    @NotBlank(message = "The reset link is invalid or has expired.")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "The reset link is invalid or has expired.")
    private String token = "";

    @NotBlank(message = "Enter a password.")
    @Size(min = 12, max = 64, message = "Use a password between 12 and 64 characters.")
    private String password = "";

    @NotBlank(message = "Confirm your password.")
    @Size(max = 64, message = "Use a password of at most 64 characters.")
    private String confirmPassword = "";

    @AssertTrue(message = "The passwords do not match.")
    public boolean isPasswordsMatching() { return Objects.equals(password, confirmPassword); }

    @AssertTrue(message = "The password must be at most 72 UTF-8 bytes and contain no null characters.")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || (password.indexOf('\0') < 0
                && password.getBytes(StandardCharsets.UTF_8).length <= 72);
    }

    public String getChallenge() { return challenge; }
    public void setChallenge(String value) { challenge = value == null ? "" : value.strip(); }
    public String getToken() { return token; }
    public void setToken(String value) { token = value == null ? "" : value.strip(); }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
}
