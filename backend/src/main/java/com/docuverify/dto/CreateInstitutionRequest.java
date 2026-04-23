package com.docuverify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateInstitutionRequest {

    @NotBlank(message = "Institution name is required")
    private String name;

    @NotBlank(message = "Domain is required")
    private String domain;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;
}
