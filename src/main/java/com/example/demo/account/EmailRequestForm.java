package com.example.demo.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailRequestForm {
    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email addresses must be at most 254 characters.")
    private String email = "";

    public String getEmail() { return email; }
    public void setEmail(String value) { email = EmailAddresses.normalize(value); }
}
