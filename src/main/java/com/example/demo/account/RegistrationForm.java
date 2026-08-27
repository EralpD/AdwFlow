package com.example.demo.account;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RegistrationForm {
    @NotBlank(message = "Enter your name.")
    @Size(min = 2, max = 80, message = "Use between 2 and 80 characters for your name.")
    private String displayName = "";

    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email addresses must be at most 254 characters.")
    private String email = "";

    @NotBlank(message = "Enter a password.")
    @Size(min = 12, max = 64, message = "Use a password between 12 and 64 characters.")
    private String password = "";

    @NotBlank(message = "Confirm your password.")
    @Size(max = 64, message = "Use a password of at most 64 characters.")
    private String confirmPassword = "";

    @AssertTrue(message = "The passwords do not match.")
    public boolean isPasswordsMatching() {
        return Objects.equals(password, confirmPassword);
    }

    // BCrypt has a 72-byte input limit; never silently truncate Unicode passwords.
    @AssertTrue(message = "The password must be at most 72 UTF-8 bytes and contain no null characters.")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || (password.indexOf('\0') < 0
                && password.getBytes(StandardCharsets.UTF_8).length <= 72);
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value == null ? "" : value.strip(); }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = EmailAddresses.normalize(value); }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
}
