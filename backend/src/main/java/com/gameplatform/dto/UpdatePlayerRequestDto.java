package com.gameplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public class UpdatePlayerRequestDto {
    @NotBlank( message = "Username cannot be blank")
    private String username;

    @Min(value = 6, message = "Password must be at least 6 characters long")
    private String password;

    @Email(message = "Invalid email format")
    private String email;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
