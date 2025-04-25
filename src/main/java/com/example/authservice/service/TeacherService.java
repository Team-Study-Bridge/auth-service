package com.example.authservice.service;

import com.example.authservice.dto.*;
import com.example.authservice.exception.ImageSizeExceededException;
import com.example.authservice.exception.InvalidImageExtensionException;
import com.example.authservice.exception.ResumeSizeExceededException;
import com.example.authservice.mapper.TeacherMapper;
import com.example.authservice.mapper.UserMapper;
import com.example.authservice.model.Teacher;
import com.example.authservice.type.FileType;
import com.example.authservice.type.Role;
import com.example.authservice.type.TeacherStatus;
import com.example.authservice.util.TokenUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeacherService {

    private final TokenProviderService tokenProviderService;
    private final RedisTemplate<String, String> redisTemplate;
    private final S3Service s3Service;
    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final TokenUtil tokenUtil;

    public ResponseEntity<TeacherApplyResponseDTO> applyForTeacher(
            String accessToken,
            TeacherApplyRequestDTO teacherApplyRequestDTO
    ) {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long userId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + userId);
        if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
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

        MultipartFile profileImage = teacherApplyRequestDTO.getProfileImage();
        MultipartFile resume = teacherApplyRequestDTO.getResume();

        String profileImageUrl = null;
        String resumeFileUrl = null;
        try {
            if (profileImage != null && !profileImage.isEmpty()) {
                profileImageUrl = s3Service.upload(profileImage, FileType.IMAGE);
            }

            if (resume != null && !resume.isEmpty()) {
                resumeFileUrl = s3Service.upload(resume, FileType.RESUME);
            }
        } catch (ImageSizeExceededException e) {
            return ResponseEntity.badRequest().body(
                    TeacherApplyResponseDTO.builder()
                            .success(false)
                            .message("이미지는 최대 1MB까지만 업로드할 수 있습니다.")
                            .build()
            );
        } catch (InvalidImageExtensionException e) {
            return ResponseEntity.badRequest().body(
                    TeacherApplyResponseDTO.builder()
                            .success(false)
                            .message("지원하지 않는 이미지 확장자입니다.")
                            .build()
            );
        } catch (ResumeSizeExceededException e) {
            return ResponseEntity.badRequest().body(
                    TeacherApplyResponseDTO.builder()
                            .success(false)
                            .message("이력서는 최대 10MB까지만 업로드할 수 있습니다.")
                            .build()
            );
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                    TeacherApplyResponseDTO.builder()
                            .success(false)
                            .message("파일 업로드 중 서버 오류가 발생했습니다.")
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
            TeacherStatusUpdateRequestDTO req
    ) {
        String cleanToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanToken);
        Long adminId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + adminId);
        if (savedToken == null || !savedToken.equals(cleanToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(TeacherStatusUpdateResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 액세스 토큰입니다.")
                            .build());
        }

        if (userMapper.isAdmin(adminId) != Role.ADMIN) {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN)
                    .body(TeacherStatusUpdateResponseDTO.builder()
                            .success(false)
                            .message("권한이 없습니다.")
                            .build());
        }

        Long targetUserId = req.getUserId();
        if (!teacherMapper.existsByUserId(targetUserId)) {
            return ResponseEntity.badRequest()
                    .body(TeacherStatusUpdateResponseDTO.builder()
                            .success(false)
                            .message("해당 유저는 강사 신청을 하지 않았습니다.")
                            .build());
        }

        Role newRole = req.isSelectStatus()
                ? Role.INSTRUCTOR
                : Role.STUDENT;

        TeacherStatus newTeacherStatus = req.isSelectStatus()
                ? TeacherStatus.APPROVED
                : TeacherStatus.REJECTED;

        try {
            // teacher 테이블 상태 변경
            teacherMapper.updateTeacherStatus(targetUserId, newTeacherStatus);
            // users 테이블 status 컬럼 업데이트
            userMapper.updateRole(targetUserId, newRole);

            String msg = req.isSelectStatus()
                    ? "회원의 강사권한이 승인되었습니다."
                    : "회원의 강사권한이 거절되었습니다.";

            return ResponseEntity.ok(
                    TeacherStatusUpdateResponseDTO.builder()
                            .success(true)
                            .message(msg)
                            .build()
            );
        } catch (Exception e) {
            log.error("강사 권한 변경 중 예외 발생", e);
            return ResponseEntity.internalServerError()
                    .body(TeacherStatusUpdateResponseDTO.builder()
                            .success(false)
                            .message("서버 오류로 인해 권한 변경에 실패했습니다.")
                            .build());
        }
    }


    public ResponseEntity<TeacherListResponseDTO> getTeacherList(
            String accessToken,
            int page,
            int size,
            TeacherStatus teacherStatus
    ) {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long adminId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + adminId);
        if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
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

    public ResponseEntity<TeacherDetailResponseDTO> getAdminTeacherDetail(
            String accessToken,
            Long userId
    ) {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long adminId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + adminId);
        if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
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


    public ResponseEntity<TeacherGetNameResponseDTO> getTeacherName(String accessToken, Long userId) {
        String cleanBearerToken = tokenUtil.cleanBearerToken(accessToken);
        ClaimsResponseDTO claims = tokenProviderService.getAuthentication(cleanBearerToken);
        Long adminId = claims.getId();

        String savedToken = redisTemplate.opsForValue().get("accessToken:" + adminId);
        if (savedToken == null || !savedToken.equals(cleanBearerToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                    TeacherGetNameResponseDTO.builder()
                            .success(false)
                            .message("유효하지 않은 액세스 토큰입니다.")
                            .build()
            );
        }
        try {
            String instructorName = teacherMapper.findTeacherByName(userId);

            return ResponseEntity.ok(
                    TeacherGetNameResponseDTO.builder()
                            .success(true)
                            .message("강사이름 조회 성공")
                            .instructorName(instructorName)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    TeacherGetNameResponseDTO.builder()
                            .success(false)
                            .message("강사이름 조회중 애러가 발생하였습니다" + e.getMessage())
                            .build()
            );
        }
    }
}