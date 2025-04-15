package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.mapper.TeacherMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.Teacher;
import com.example.authservice.type.Role;
import com.example.authservice.type.TeacherStatus;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeacherService {

    private final TokenProviderService tokenProviderService;
    private final RedisTemplate<String, String> redisTemplate;
    private final S3Service s3Service;
    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;

    public ResponseEntity<TeacherApplyResponseDTO> applyForTeacher(
            String accessToken,
            TeacherApplyRequestDTO teacherApplyRequestDTO
    ) {
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        Long userId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        if (savedToken == null || !savedToken.equals(accessToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    TeacherApplyResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 액세스 토큰입니다.")
                            .build()
            );
        }

        if (teacherMapper.existsByUserId(userId)) {
            return ResponseEntity.badRequest().body(
                    TeacherApplyResponseDTO.builder()
                            .success(false)
                            .message("이미 강사 신청을 하셨습니다.")
                            .build()
            );
        }

        String profileImageUrl = null;
        String resumeFileUrl = null;

        MultipartFile profileImage = teacherApplyRequestDTO.getProfileImage();
        MultipartFile resume = teacherApplyRequestDTO.getResume();

        try {
            if (profileImage != null && !profileImage.isEmpty()) {
                profileImageUrl = s3Service.upload(profileImage, "teacher/profile");
            }

            if (resume != null && !resume.isEmpty()) {
                resumeFileUrl = s3Service.upload(resume, "teacher/resume");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    TeacherApplyResponseDTO.builder()
                            .success(false)
                            .message("파일 업로드 중 오류가 발생했습니다.")
                            .build()
            );
        }

        teacherMapper.insertTeacher(
                userId,
                teacherApplyRequestDTO.getName(),
                teacherApplyRequestDTO.getBio(),
                teacherApplyRequestDTO.getCategory(),
                profileImageUrl,
                resumeFileUrl
        );

        return ResponseEntity.ok(
                TeacherApplyResponseDTO.builder()
                        .success(true)
                        .message("강사 신청이 정상적으로 접수되었습니다.")
                        .build()
        );
    }

    public ResponseEntity<TeacherStatusUpdateResponseDTO> updateTeacherStatus(
            String accessToken,
            TeacherStatusUpdateRequestDTO teacherStatusUpdateRequestDTO
    ) {
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        Long admimId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + admimId);
        if (savedToken == null || !savedToken.equals(accessToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    TeacherStatusUpdateResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 액세스 토큰입니다.")
                            .build()
            );
        }

        Role isAdmin = userMapper.isAdmin(admimId);
        if (isAdmin != Role.ADMIN) {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body(
                    TeacherStatusUpdateResponseDTO.builder()
                            .success(false)
                            .message("권환이 없습니다.")
                            .build()
            );
        }

        if (!teacherMapper.existsByUserId(teacherStatusUpdateRequestDTO.getUserId())) {
            return ResponseEntity.badRequest().body(
                    TeacherStatusUpdateResponseDTO.builder()
                            .success(false)
                            .message("해당 유저는 강사 신청을 하지 않았습니다.")
                            .build()
            );
        }

        TeacherStatus status = teacherStatusUpdateRequestDTO.isSelectStatus() ?
                TeacherStatus.APPROVED : TeacherStatus.REJECTED;
        try {
            teacherMapper.updateTeacherStatus(teacherStatusUpdateRequestDTO.getUserId(), status);

            return ResponseEntity.ok(
                    TeacherStatusUpdateResponseDTO.builder()
                            .success(true)
                            .message(teacherStatusUpdateRequestDTO.isSelectStatus() ?
                                    "회원의 강사권한이 승인되었습니다." : "회원의 강사권한이 거절되었습니다.")
                            .build()
            );

        } catch (Exception e) {
            log.error("강사 권한 변경 중 예외 발생", e);
            return ResponseEntity.internalServerError().body(
                    TeacherStatusUpdateResponseDTO.builder()
                            .success(false)
                            .message("서버 오류로 인해 권한 변경에 실패했습니다.")
                            .build()
            );
        }
    }

    public ResponseEntity<TeacherListResponseDTO> getTeacherList(
            String accessToken,
            int page,
            int size,
            TeacherStatus teacherStatus
    ) {
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        Long adminId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + adminId);
        if (savedToken == null || !savedToken.equals(accessToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    TeacherListResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 액세스 토큰입니다.")
                            .build()
            );
        }

        if (userMapper.isAdmin(adminId) != Role.ADMIN) {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body(
                    TeacherListResponseDTO.builder()
                            .success(false)
                            .message("관리자 권한이 없습니다.")
                            .build()
            );
        }

        int offset = (page - 1) * size;
        List<TeacherSummaryDTO> teachers = teacherMapper.findTeachersWithPaging(offset, size, teacherStatus);
        int totalCount = teacherMapper.countTeachers(teacherStatus);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        return ResponseEntity.ok(
                TeacherListResponseDTO.builder()
                        .success(true)
                        .message("강사 목록 조회 성공")
                        .teachers(teachers)
                        .currentPage(page)
                        .totalPages(totalPages)
                        .build()
        );
    }

    public ResponseEntity<TeacherDetailResponseDTO> getTeacherDetail(
            String accessToken,
            Long userId
    ) {
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(accessToken);
        Long adminId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + adminId);
        if (savedToken == null || !savedToken.equals(accessToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    TeacherDetailResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 액세스 토큰입니다.")
                            .build()
            );
        }

        if (userMapper.isAdmin(adminId) != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    TeacherDetailResponseDTO.builder()
                            .success(false)
                            .message("관리자 권한이 없습니다.")
                            .build()
            );
        }

        Teacher teacher = teacherMapper.findByUserId(userId);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    TeacherDetailResponseDTO.builder()
                            .success(false)
                            .message("해당 사용자의 강사 신청 정보가 없습니다.")
                            .build()
            );
        }

        return ResponseEntity.ok(
                TeacherDetailResponseDTO.builder()
                        .success(true)
                        .message("강사 상세 조회 성공")
                        .userId(teacher.getUserId())
                        .name(teacher.getName())
                        .bio(teacher.getBio())
                        .category(teacher.getCategory())
                        .profileImage(teacher.getProfileImage())
                        .resumeFile(teacher.getResumeFile())
                        .status(teacher.getTeacherStatus())
                        .build()
        );
    }
}