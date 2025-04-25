package com.example.authservice.dto;

import com.example.authservice.type.TeacherStatus;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@Builder
public class TeacherSummaryDTO {
    private Long id;
    private Long userId;
    private String name;
    private String bio;
    private String category;
    private String profileImage;
    private Timestamp createdAt;
    private TeacherStatus status;
}