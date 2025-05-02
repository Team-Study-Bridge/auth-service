package com.example.authservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InstructorProfileResponseDTO {
    private String name;
    private String profileImage;
    private Float rating;
}
