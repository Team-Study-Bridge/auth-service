package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.UserApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "회원 정보 관리", description = "회원의 닉네임, 비밀번호, 프로필 이미지 수정 및 탈퇴, 정보 조회 API")
@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class UserApiController {

    private final UserApiService userApiService;

    @Operation(summary = "닉네임 수정", description = "회원의 닉네임을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 수정 성공",
                    content = @Content(schema = @Schema(implementation = NicknameUpdateResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"닉네임이 성공적으로 변경되었습니다.\",\n" +
                                            "  \"nickname\": \"newNickname\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "닉네임 유효성 검사 실패",
                    content = @Content(examples = @ExampleObject(
                            name = "형식 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"닉네임은 2~10자이며, 공백이나 특수문자를 포함할 수 없습니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"유효하지 않은 토큰입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "422", description = "금지된 닉네임",
                    content = @Content(examples = @ExampleObject(
                            name = "금지어",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"사용할 수 없는 닉네임입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "서버 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"닉네임 변경 중 오류가 발생했습니다: ...\"\n" +
                                    "}"
                    )))
    })
    @PutMapping("/nickname")
    public ResponseEntity<NicknameUpdateResponseDTO> updateNickname(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody NicknameUpdateRequestDTO nicknameUpdateRequestDTO) {

        return userApiService.updateNickname(accessToken, nicknameUpdateRequestDTO.getNickname());
    }

    @Operation(summary = "비밀번호 변경", description = "회원의 비밀번호를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공",
                    content = @Content(schema = @Schema(implementation = PasswordUpdateResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"비밀번호가 성공적으로 변경되었습니다.\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 새 비밀번호 유효성 실패",
                    content = @Content(examples = @ExampleObject(
                            name = "불일치 또는 형식 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"현재 비밀번호가 일치하지 않습니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"유효하지 않은 토큰입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "서버 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"비밀번호 변경 중 오류가 발생했습니다: ...\"\n" +
                                    "}"
                    )))
    })
    @PutMapping("/password")
    public ResponseEntity<PasswordUpdateResponseDTO> updatePassword(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody PasswordUpdateRequestDTO passwordUpdateRequestDTO
    ) {
        return userApiService.updatePassword(
                accessToken,
                passwordUpdateRequestDTO.getCurrentPassword(),
                passwordUpdateRequestDTO.getNewPassword()
        );
    }

    @Operation(summary = "프로필 이미지 변경", description = "회원의 프로필 이미지를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 이미지 변경 성공",
                    content = @Content(schema = @Schema(implementation = ProfileImageUpdateResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"프로필 이미지가 성공적으로 업데이트되었습니다.\",\n" +
                                            "  \"profileImage\": \"https://s3.amazonaws.com/mybucket/profile123.jpg\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "이미지가 비어 있음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "이미지 없음",
                                    value = "{\n" +
                                            "  \"success\": false,\n" +
                                            "  \"message\": \"프로필 이미지가 비어 있습니다.\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "토큰 오류",
                                    value = "{\n" +
                                            "  \"success\": false,\n" +
                                            "  \"message\": \"유효하지 않은 액세스 토큰입니다.\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "500", description = "S3 업로드 또는 서버 오류",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "S3 업로드 실패",
                                    value = "{\n" +
                                            "  \"success\": false,\n" +
                                            "  \"message\": \"이미지 업로드 중 오류가 발생했습니다.\"\n" +
                                            "}"
                            ))
            )
    })
    @PutMapping(value = "/profile-image", consumes = "multipart/form-data")
    public ResponseEntity<ProfileImageUpdateResponseDTO> updateProfileImage(
            @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "업로드할 프로필 이미지 파일 (JPG, PNG 등)")
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        return userApiService.updateProfileImage(accessToken, profileImage);
    }

    @Operation(summary = "회원 탈퇴", description = "회원 계정을 탈퇴 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "계정 삭제 성공",
                    content = @Content(schema = @Schema(implementation = DeleteAccountResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"계정이 정상적으로 삭제되었습니다.\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"유효하지 않은 토큰입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "서버 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"계정 삭제 중 오류가 발생했습니다: ...\"\n" +
                                    "}"
                    )))
    })
    @PutMapping("/delete")
    public ResponseEntity<DeleteAccountResponseDTO> deleteAccount(
            @RequestHeader("Authorization") String accessToken,
            HttpServletRequest request,
            HttpServletResponse response) {

        return userApiService.deleteAccount(accessToken, request, response);
    }

    @Operation(summary = "회원 정보 조회", description = "회원의 이메일, 닉네임, 역할 등의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"사용자의 정보를 정상적으로 불러왔습니다.\",\n" +
                                            "  \"email\": \"user@example.com\",\n" +
                                            "  \"nickname\": \"user123\",\n" +
                                            "  \"role\": \"USER\",\n" +
                                            "  \"phoneNumber\": \"010-1234-5678\",\n" +
                                            "  \"profileImage\": \"https://s3.amazonaws.com/mybucket/user.png\",\n" +
                                            "  \"provider\": \"local\",\n" +
                                            "  \"status\": \"ACTIVE\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"유효하지 않은 토큰입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "서버 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"서버 오류로 사용자 정보를 불러오지 못했습니다.\"\n" +
                                    "}"
                    )))
    })
    @GetMapping("/info")
    public ResponseEntity<UserInfoResponseDTO> userInfo(
            @RequestHeader("Authorization") String accessToken
    ) {
        return userApiService.userInfo(accessToken);
    }
}