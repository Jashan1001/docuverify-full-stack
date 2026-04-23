package com.docuverify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentRequest {

    @NotBlank(message = "Title is required")
    @jakarta.validation.constraints.Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @jakarta.validation.constraints.Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;
}
