package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.UserService;
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

@Tag(name = "회원", description = "회원가입, 로그인, 로그아웃 등 회원 관련 API")
@RestController
@RequestMapping("/auths")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "회원 정보를 등록하고, 프로필 이미지를 함께 업로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 및 로그인 완료",
                    content = @Content(schema = @Schema(implementation = UserJoinResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"accessToken\": \"eyJhbGci...\",\n" +
                                            "  \"message\": \"회원가입 및 로그인 완료\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "비밀번호 또는 닉네임 유효성 실패",
                    content = @Content(examples = @ExampleObject(
                            name = "형식 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"패스워드 또는 닉네임 설정이 잘못됐습니다.\",\n" +
                                    "  \"errors\": {\n" +
                                    "    \"passwordValid\": false,\n" +
                                    "    \"nicknameValid\": true\n" +
                                    "  }\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "401", description = "이메일 인증 미완료",
                    content = @Content(examples = @ExampleObject(
                            name = "인증 안됨",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"이메일 인증이 완료되지 않았습니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(examples = @ExampleObject(
                            name = "중복 이메일",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"이미 가입된 이메일입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "422", description = "금지된 닉네임 사용",
                    content = @Content(examples = @ExampleObject(
                            name = "금지어 포함",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"사용할 수 없는 닉네임입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "500", description = "서버 오류 또는 프로필 업로드 실패",
                    content = @Content(examples = @ExampleObject(
                            name = "S3 업로드 실패",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"프로필 이미지 업로드에 실패했습니다.\"\n" +
                                    "}"
                    )))
    })
    @PostMapping(value = "/join", consumes = {"multipart/form-data"})
    public ResponseEntity<UserJoinResponseDTO> join(
            @RequestPart("userJoinRequestDTO") UserJoinRequestDTO userJoinRequestDTO,
            @Parameter(description = "프로필 이미지 (선택)", required = false)
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            HttpServletResponse response
    ) {
        return userService.save(userJoinRequestDTO, profileImage, response);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 토큰을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = UserLoginResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"loggedIn\": true,\n" +
                                            "  \"accessToken\": \"eyJhbGci...\",\n" +
                                            "  \"message\": \"환영합니다 youngjin 님\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "로그인 실패",
                            value = "{\n" +
                                    "  \"loggedIn\": false,\n" +
                                    "  \"message\": \"이메일 또는 비밀번호가 일치하지 않습니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "403", description = "정지된 계정",
                    content = @Content(examples = @ExampleObject(
                            name = "정지 계정",
                            value = "{\n" +
                                    "  \"loggedIn\": false,\n" +
                                    "  \"message\": \"활동 정지된 계정입니다.\"\n" +
                                    "}"
                    ))),
            @ApiResponse(responseCode = "409", description = "이미 다른 브라우저에서 로그인 중",
                    content = @Content(examples = @ExampleObject(
                            name = "중복 로그인",
                            value = "{\n" +
                                    "  \"loggedIn\": false,\n" +
                                    "  \"message\": \"현재 계정은 다른 브라우저에서 로그인 중입니다.\\n계속 진행하시겠습니까?\\n\\n(로그인 시 기존 로그인된 계정은 로그아웃 됩니다.)\"\n" +
                                    "}"
                    )))
    })
    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserLoginResponseDTO> login(
            @RequestBody UserLoginRequestDTO userLoginRequestDTO,
            HttpServletResponse response
    ) {
        return userService.login(userLoginRequestDTO.getEmail(), userLoginRequestDTO.getPassword(), response);
    }

    @Operation(summary = "강제 로그인", description = "다른 브라우저 세션과 관계없이 강제로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = UserLoginResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"loggedIn\": true,\n" +
                                            "  \"accessToken\": \"eyJhbGci...\",\n" +
                                            "  \"message\": \"환영합니다 youngjin 님\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 오류",
                    content = @Content(examples = @ExampleObject(
                            name = "로그인 실패",
                            value = "{\n" +
                                    "  \"loggedIn\": false,\n" +
                                    "  \"message\": \"이메일 또는 비밀번호가 일치하지 않습니다.\"\n" +
                                    "}"
                    )))
    })
    @PostMapping(value = "/force-login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserLoginResponseDTO> forceLogin(
            @RequestBody UserLoginRequestDTO userLoginRequestDTO,
            HttpServletResponse response
    ) {
        return userService.forceLogin(userLoginRequestDTO.getEmail(), userLoginRequestDTO.getPassword(), response);
    }

    @Operation(summary = "로그아웃", description = "현재 사용자의 토큰을 만료시켜 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정상적으로 로그아웃됨",
                    content = @Content(schema = @Schema(implementation = UserLoginResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"loggedIn\": false,\n" +
                                            "  \"message\": \"정상적으로 로그아웃 되었습니다.\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰으로 로그아웃 시도",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{\n" +
                                    "  \"loggedIn\": true,\n" +
                                    "  \"message\": \"로그아웃 실패: Invalid token format\"\n" +
                                    "}"
                    )))
    })
    @DeleteMapping("/logout")
    public ResponseEntity<UserLoginResponseDTO> logout(
            @Parameter(description = "Authorization 헤더에 담긴 액세스 토큰", required = true)
            @RequestHeader("Authorization") String accessToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return userService.logout(accessToken, request, response);
    }
}