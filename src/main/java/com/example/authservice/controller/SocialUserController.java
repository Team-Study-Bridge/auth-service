package com.example.authservice.controller;

import com.example.authservice.config.security.oauth.CustomOAuth2User;
import com.example.authservice.dto.OAuthUserInfoResponseDTO;
import com.example.authservice.model.User;
import com.example.authservice.service.SocialUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "소셜로그인", description = "소셜 로그인 관련 API")
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class SocialUserController {
    private final SocialUserService socialUserService;

    @Operation(summary = "소셜 유저 정보 조회", description = "소셜 로그인 시 사용자 정보 및 연동 필요 여부를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "소셜 로그인 성공 또는 연동 필요",
                    content = @Content(schema = @Schema(implementation = OAuthUserInfoResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "로그인 성공", value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"로그인 성공\",\n" +
                                            "  \"token\": \"eyJhbGciOiJIUzI1NiIsInR5...\",\n" +
                                            "  \"needsLinking\": false,\n" +
                                            "  \"user\": {\n" +
                                            "    \"id\": 1,\n" +
                                            "    \"email\": \"youngjin@example.com\",\n" +
                                            "    \"nickname\": \"영진\",\n" +
                                            "    \"profileImage\": \"https://s3.aws.com/profile.jpg\",\n" +
                                            "    \"provider\": \"NAVER\"\n" +
                                            "  }\n" +
                                            "}"
                                    ),
                                    @ExampleObject(name = "연동 필요", value = "{\n" +
                                            "  \"success\": false,\n" +
                                            "  \"message\": \"기존 계정이 존재합니다. 연동하시겠습니까?\",\n" +
                                            "  \"token\": \"eyJhbGciOiJIUzI1NiIsInR5...\",\n" +
                                            "  \"needsLinking\": true,\n" +
                                            "  \"user\": {\n" +
                                            "    \"id\": 1,\n" +
                                            "    \"email\": \"youngjin@example.com\",\n" +
                                            "    \"nickname\": \"영진\",\n" +
                                            "    \"profileImage\": \"https://s3.aws.com/profile.jpg\",\n" +
                                            "    \"provider\": \"LOCAL\"\n" +
                                            "  }\n" +
                                            "}"
                                    )
                            })),
            @ApiResponse(responseCode = "401", description = "비인증 접근 (customOAuth2User == null)",
                    content = @Content(examples = @ExampleObject(
                            name = "비인증",
                            value = "{ \"success\": false, \"message\": \"Unauthorized\" }"
                    )))
    })
    @GetMapping("/info")
    public ResponseEntity<OAuthUserInfoResponseDTO> getUserInfo(@AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        return socialUserService.getUserInfo(customOAuth2User);
    }

    @Operation(summary = "소셜 계정 연동", description = "기존 로컬 계정에 소셜 정보를 연동합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "연동 성공",
                    content = @Content(schema = @Schema(implementation = OAuthUserInfoResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "연동 성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"message\": \"연동 완료! 로그인 성공\",\n" +
                                            "  \"token\": \"eyJhbGciOiJIUzI1NiIsInR5...\",\n" +
                                            "  \"needsLinking\": false,\n" +
                                            "  \"user\": {\n" +
                                            "    \"id\": 1,\n" +
                                            "    \"email\": \"youngjin@example.com\",\n" +
                                            "    \"nickname\": \"영진\",\n" +
                                            "    \"profileImage\": \"https://s3.aws.com/profile.jpg\",\n" +
                                            "    \"provider\": \"GOOGLE\"\n" +
                                            "  }\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "400", description = "이미 연동된 계정",
                    content = @Content(examples = @ExampleObject(
                            name = "이미 연동됨",
                            value = "{ \"success\": false, \"message\": \"이미 연동된 계정입니다.\" }"
                    )))
    })
    @PostMapping("/link")
    public ResponseEntity<OAuthUserInfoResponseDTO> linkAccount(@AuthenticationPrincipal CustomOAuth2User customUser) {
        return socialUserService.linkAccount(customUser);
    }
}
