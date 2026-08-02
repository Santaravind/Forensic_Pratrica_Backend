package com.security.forecsic.dto;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    private String organization;

    @NotBlank
    private String domain;

    @NotBlank
    private String password;

    @NotBlank
    private String mobileNo;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String role;
}
