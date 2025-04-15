package com.example.authservice.dto;

import com.example.authservice.type.TeacherStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherSummaryDTO {
    private Long userId;
    private String name;
    private String bio;
    private TeacherStatus status;
}