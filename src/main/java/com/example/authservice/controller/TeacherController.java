package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.TeacherService;
import com.example.authservice.type.TeacherStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping("/apply")
    public ResponseEntity<TeacherApplyResponseDTO> applyForTeacher(
            @RequestHeader("Authorization") String accessToken,
            @RequestPart("name") String name,
            @RequestPart("bio") String bio,
            @RequestPart("category") String category,
            @RequestPart("profileImage") MultipartFile profileImage,
            @RequestPart("resume") MultipartFile resume
    ) {
        return teacherService.applyForTeacher(accessToken,
                TeacherApplyRequestDTO.builder()
                .name(name)
                .bio(bio)
                .category(category)
                .profileImage(profileImage)
                .resume(resume)
                .build());
    }

    @PutMapping("/teachers/status")
    public ResponseEntity<TeacherStatusUpdateResponseDTO> updateStatus(
            @RequestHeader("Authorization") String accessToken,
            @RequestPart("userId") Long userId,
            @RequestPart("selectStatus") boolean selectStatus
    ) {
        return teacherService.updateTeacherStatus(accessToken,
                TeacherStatusUpdateRequestDTO.builder()
                        .userId(userId)
                        .selectStatus(selectStatus)
                        .build());
    }

    @GetMapping("/admin/teachers")
    public ResponseEntity<TeacherListResponseDTO> getTeacherList(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)TeacherStatus status // 선택 필터
    ) {
        return teacherService.getTeacherList(accessToken, page, size, status);
    }

    @GetMapping("/admin/teachers/{userId}")
    public ResponseEntity<TeacherDetailResponseDTO> getTeacherDetail(
            @RequestHeader("Authorization") String accessToken,
            @PathVariable Long userId
    ) {
        return teacherService.getTeacherDetail(accessToken, userId);
    }

}
