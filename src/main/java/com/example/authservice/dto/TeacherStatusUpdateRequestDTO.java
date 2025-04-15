package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherStatusUpdateRequestDTO {
    private Long userId;
    private boolean selectStatus;
}