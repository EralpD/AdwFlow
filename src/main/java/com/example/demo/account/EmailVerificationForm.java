package com.example.demo.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class EmailVerificationForm {
    @NotBlank(message = "Request a new verification code.")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Request a new verification code.")
    private String challenge = "";

    @NotBlank(message = "Enter the six-digit code.")
    @Pattern(regexp = "[0-9]{6}", message = "Enter the six-digit code.")
    private String code = "";

    public String getChallenge() { return challenge; }
    public void setChallenge(String value) { challenge = value == null ? "" : value.strip(); }
    public String getCode() { return code; }
    public void setCode(String value) { code = value == null ? "" : value.strip(); }
}
