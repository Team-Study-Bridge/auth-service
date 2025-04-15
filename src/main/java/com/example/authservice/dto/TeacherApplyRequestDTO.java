package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Builder
public class TeacherApplyRequestDTO {
    private String name;
    private String bio;
    private String category;
    private MultipartFile profileImage;
    private MultipartFile resume;
}
