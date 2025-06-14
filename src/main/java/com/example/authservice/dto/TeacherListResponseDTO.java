package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeacherListResponseDTO {
    private boolean success;
    private String message;
    private List<TeacherSummaryDTO> teachers;
    private int totalPages;
    private int currentPage;
}