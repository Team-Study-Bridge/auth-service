package com.example.authservice.controller;

import com.example.authservice.dto.EmailRequestDTO;
import com.example.authservice.dto.SendCodeResponseDTO;
import com.example.authservice.dto.VerifyCodeResponseDTO;
import com.example.authservice.service.EmailVerificationService;
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

@Tag(name = "이메일 인증", description = "이메일 인증 관련 API")
@RestController
@RequestMapping("/auths/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "이메일 인증 코드 전송", description = "입력한 이메일로 6자리 인증 코드를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코드 전송 성공 또는 이미 인증됨",
                    content = @Content(schema = @Schema(implementation = SendCodeResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "코드 전송 성공", value = "{ \"success\": true, \"message\": \"이메일에서 코드를 확인해주세요.\" }"),
                                    @ExampleObject(name = "이미 인증됨", value = "{ \"success\": true, \"message\": \"이미 인증된 이메일입니다.\" }")
                            })),
            @ApiResponse(responseCode = "400", description = "이미 가입된 이메일",
                    content = @Content(examples = @ExampleObject(
                            name = "가입된 이메일",
                            value = "{ \"success\": false, \"message\": \"이미 가입된 이메일입니다.\" }"
                    ))),
            @ApiResponse(responseCode = "500", description = "이메일 전송 실패",
                    content = @Content(examples = @ExampleObject(
                            name = "메일 전송 오류",
                            value = "{ \"success\": false, \"message\": \"이메일 발송중 오류가 발생하였습니다\" }"
                    )))
    })
    @PostMapping("/send-code")
    public ResponseEntity<SendCodeResponseDTO> sendVerificationCode(@RequestBody EmailRequestDTO emailRequestDTO) {
        return emailVerificationService.sendVerificationCode(emailRequestDTO.getEmail());
    }

    @Operation(summary = "이메일 인증 코드 검증", description = "입력한 이메일과 인증 코드를 비교하여 인증을 완료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 성공 또는 이미 인증됨",
                    content = @Content(schema = @Schema(implementation = VerifyCodeResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "검증 완료", value = "{ \"success\": true, \"message\": \"인증이 완료되었습니다.\" }"),
                                    @ExampleObject(name = "이미 인증됨", value = "{ \"success\": true, \"message\": \"이미 인증된 이메일입니다.\" }")
                            })),
            @ApiResponse(responseCode = "400", description = "검증 실패",
                    content = @Content(examples = {
                            @ExampleObject(name = "정보 없음", value = "{ \"success\": false, \"message\": \"인증 정보가 존재하지 않습니다. 처음부터 다시 시도해주세요.\" }"),
                            @ExampleObject(name = "코드 불일치", value = "{ \"success\": false, \"message\": \"코드가 일치하지 않습니다.\" }"),
                            @ExampleObject(name = "시간 초과", value = "{ \"success\": false, \"message\": \"인증 시간이 초과되었습니다. 다시 시도해주세요.\" }")
                    }))
    })
    @PostMapping("/verify-code")
    public ResponseEntity<VerifyCodeResponseDTO> verifyCode(@RequestBody EmailRequestDTO emailRequestDTO) {
        return emailVerificationService.verifyCode(emailRequestDTO.getEmail(), emailRequestDTO.getCode());
    }
}
