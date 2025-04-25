package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherGetNameResponseDTO {
    private boolean success;
    private String message;
    private String instructorName;
}
