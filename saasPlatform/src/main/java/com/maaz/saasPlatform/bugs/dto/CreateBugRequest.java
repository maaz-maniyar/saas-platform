package com.maaz.saasPlatform.bugs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBugRequest {
    @NotBlank
    private String title;
    private String description;
    private String assignedTo;
}
