package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.TeacherService;
import com.example.authservice.type.TeacherStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "강사회원 관리", description = "회원의 강사신청과, 관리자의 강사관리 API")
@RestController
@RequestMapping("/auths/teacher")
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

    @PutMapping("/status")
    public ResponseEntity<TeacherStatusUpdateResponseDTO> updateStatus(
            @RequestHeader("Authorization") String accessToken,
            @ModelAttribute TeacherStatusUpdateRequestDTO request
    ) {
        return teacherService.updateTeacherStatus(accessToken, request);
    }


    @GetMapping("/admin/getAdminTeacherDetailPage")
    public ResponseEntity<TeacherListResponseDTO> getTeacherList(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)TeacherStatus status // 선택 필터
    ) {
        return teacherService.getTeacherList(accessToken, page, size, status);
    }


    @PostMapping("/admin/getAdminTeacherDetail")
    public ResponseEntity<TeacherDetailResponseDTO> getAdminTeacherDetail(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody UserIdRequestDTO userIdRequestDTO
    ) {
        return teacherService.getAdminTeacherDetail(accessToken, userIdRequestDTO.getId());
    }

    @PostMapping("/api/teacher-name")
    public ResponseEntity<TeacherGetNameResponseDTO> getTeacherName(
            @RequestBody UserIdRequestDTO userIdRequestDTO
    ) {
        return teacherService.getTeacherName(userIdRequestDTO.getId());
    }

    @GetMapping("/role")
    public ResponseEntity<IsApprovedInstructorResponseDTO> getTeacherRole(
            @RequestHeader("Authorization") String accessToken
    ) {
        return teacherService.getTeacherRole(accessToken);
    }

    @GetMapping("/api/teachers/{id}/profile")
    public InstructorProfileResponseDTO getInstructorProfile(@PathVariable Long id) {
        return teacherService.getInstructorProfile(id);
    }
}
