package com.example.authservice.dto;

import com.example.authservice.type.TeacherStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherDetailResponseDTO {
    private boolean success;
    private String message;
    private Long id;
    private String name;
    private String bio;
    private String category;
    private String profileImage;
    private String resumeFile;
    private TeacherStatus status;
}