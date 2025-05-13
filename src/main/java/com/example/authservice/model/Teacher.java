package com.example.authservice.model;

import com.example.authservice.type.TeacherStatus;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
public class Teacher {
    private Long id;
    private String name;
    private String bio;
    private String category;
    private String profileImage;
    private String resumeFile;
    private Float rating;
    private Timestamp createdAt;
    private TeacherStatus teacherStatus;
}
