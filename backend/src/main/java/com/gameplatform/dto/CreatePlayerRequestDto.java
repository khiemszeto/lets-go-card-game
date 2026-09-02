package com.gameplatform.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class CreatePlayerRequestDto {
    @NotBlank( message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be more than 6 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
