package com.example.authservice.controller;

import com.example.authservice.dto.RefreshTokenResponseDTO;
import com.example.authservice.dto.ValidTokenResponseDTO;
import com.example.authservice.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "토큰 관리", description = "토큰의 재발급 토큰 유효성검사 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auths")
public class TokenController {

    private final TokenService tokenService;

    @Operation(summary = "액세스 토큰 재발급", description = "쿠키 기반 리프레시 토큰으로 새로운 액세스 토큰을 재발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "성공",
                                    value = "{\n" +
                                            "  \"success\": true,\n" +
                                            "  \"accessToken\": \"eyJhbGci...\",\n" +
                                            "  \"message\": \"토큰이 성공적으로 재발급 되었습니다.\"\n" +
                                            "}"
                            ))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰 또는 쿠키 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "토큰 오류",
                            value = "{\n" +
                                    "  \"success\": false,\n" +
                                    "  \"message\": \"리프레시 토큰이 존재하지 않습니다.\"\n" +
                                    "}"
                    )))
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @RequestHeader("Authorization") String accessToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        return tokenService.refreshToken(accessToken, response, request);
    }


    @Operation(summary = "토큰 유효성 검증", description = "토큰의 유효 여부를 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 결과 반환",
                    content = @Content(schema = @Schema(implementation = ValidTokenResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "유효함", value = "{ \"isValid\": true, \"message\": \"유효한 토큰입니다.\" }"),
                                    @ExampleObject(name = "만료됨", value = "{ \"isValid\": false, \"message\": \"만료된 토큰입니다.\" }"),
                                    @ExampleObject(name = "잘못된 토큰", value = "{ \"isValid\": false, \"message\": \"잘못된 토큰입니다.\" }")
                            }))
    })
    @PostMapping("/valid-token")
    public ValidTokenResponseDTO validToken(@RequestHeader("Authorization") String accessToken) {
        return tokenService.validateToken(accessToken);
    }

}
