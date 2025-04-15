package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.TeacherService;
import com.example.authservice.type.TeacherStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "강사회원 관리", description = "회원의 강사신청과, 관리자의 강사관리 API")
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "강사 신청", description = "유저가 강사 신청을 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신청 성공",
                    content = @Content(schema = @Schema(implementation = TeacherApplyResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "신청 성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"강사 신청이 정상적으로 접수되었습니다.\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "이미 신청한 사용자",
                    content = @Content(examples = @ExampleObject(
                            name = "중복 신청",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"이미 강사 신청을 하셨습니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "401", description = "토큰 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"유효하지 않은 액세스 토큰입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "500", description = "파일 업로드 실패",
                    content = @Content(examples = @ExampleObject(
                            name = "S3 실패",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"파일 업로드 중 오류가 발생했습니다.\"\n" +
                                    "}"
                    )))
    })
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

    @Operation(summary = "강사 신청 상태 변경", description = "관리자가 특정 사용자의 강사 신청을 승인 또는 거절합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공",
                    content = @Content(schema = @Schema(implementation = TeacherStatusUpdateResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "승인됨", value = "{ \"success\": true, \"message\": \"회원의 강사권한이 승인되었습니다.\" }"),
                                    @ExampleObject(name = "거절됨", value = "{ \"success\": true, \"message\": \"회원의 강사권한이 거절되었습니다.\" }")
                            })),
            @ApiResponse(responseCode = "400", description = "해당 유저가 신청하지 않음",
                    content = @Content(examples = @ExampleObject(
                            name = "강사 아님",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"해당 유저는 강사 신청을 하지 않았습니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "401", description = "토큰 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{ \"success\": false, \"message\": \"유효하지 않은 액세스 토큰입니다.\" }"
                    ))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "관리자 아님",
                            value = "{ \"success\": false, \"message\": \"권환이 없습니다.\" }"
                    ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "서버 오류",
                            value = "{ \"success\": false, \"message\": \"서버 오류로 인해 권한 변경에 실패했습니다.\" }"
                    )))
    })
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

    @Operation(summary = "강사 목록 조회", description = "관리자가 전체 강사 신청 목록을 페이지네이션하여 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = TeacherListResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "목록 조회 성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"강사 목록 조회 성공\",\n" +
                                            "  \"currentPage\": 1,\n" +
                                            "  \"totalPages\": 3,\n" +
                                            "  \"teachers\": [\n" +
                                            "    {\n" +
                                            "      \"userId\": 1,\n" +
                                            "      \"name\": \"김민지\",\n" +
                                            "      \"category\": \"프로그래밍\",\n" +
                                            "      \"status\": \"PENDING\"\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "토큰 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{ \"success\": false, \"message\": \"유효하지 않은 액세스 토큰입니다.\" }"
                    ))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "권한 없음",
                            value = "{ \"success\": false, \"message\": \"관리자 권한이 없습니다.\" }"
                    )))
    })
    @GetMapping("/admin/teachers")
    public ResponseEntity<TeacherListResponseDTO> getTeacherList(
            @RequestHeader("Authorization") String accessToken,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)TeacherStatus status // 선택 필터
    ) {
        return teacherService.getTeacherList(accessToken, page, size, status);
    }

    @Operation(summary = "강사 상세 조회", description = "관리자가 특정 유저의 강사 신청 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = TeacherDetailResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "상세 조회 성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"강사 상세 조회 성공\",\n" +
                                            "  \"userId\": 1,\n" +
                                            "  \"name\": \"김민지\",\n" +
                                            "  \"bio\": \"열정 있는 강사입니다\",\n" +
                                            "  \"category\": \"프로그래밍\",\n" +
                                            "  \"profileImage\": \"https://s3.aws.com/teacher1.jpg\",\n" +
                                            "  \"resumeFile\": \"https://s3.aws.com/resume1.pdf\",\n" +
                                            "  \"status\": \"PENDING\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "토큰 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{ \"success\": false, \"message\": \"유효하지 않은 액세스 토큰입니다.\" }"
                    ))),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "권한 없음",
                            value = "{ \"success\": false, \"message\": \"관리자 권한이 없습니다.\" }"
                    ))),
            @ApiResponse(responseCode = "404", description = "신청 정보 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "강사 정보 없음",
                            value = "{ \"success\": false, \"message\": \"해당 사용자의 강사 신청 정보가 없습니다.\" }"
                    )))
    })
    @GetMapping("/admin/teachers/{userId}")
    public ResponseEntity<TeacherDetailResponseDTO> getTeacherDetail(
            @RequestHeader("Authorization") String accessToken,
            @PathVariable Long userId
    ) {
        return teacherService.getTeacherDetail(accessToken, userId);
    }

}
