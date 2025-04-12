package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.UserApiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class UserApiController {

    private final UserApiService userApiService;

    @PutMapping("/nickname")
    public NicknameUpdateResponseDTO updateNickname(@RequestBody NicknameUpdateRequestDTO nicknameUpdateRequestDTO) {
        return userApiService.updateNickname(nicknameUpdateRequestDTO.getAccessToken(), nicknameUpdateRequestDTO.getNickname());
    }

    @PutMapping("/password")
    public PasswordUpdateResponseDTO updatePassword(@RequestBody PasswordUpdateRequestDTO passwordUpdateRequestDTO) {
        return userApiService.updatePassword(
                passwordUpdateRequestDTO.getAccessToken(),
                passwordUpdateRequestDTO.getCurrentPassword(),
                passwordUpdateRequestDTO.getNewPassword()
        );
    }

    @PutMapping("/delete")
    public DeleteAccountResponseDTO deleteAccount(@RequestBody DeleteAccountRequestDTO deleteAccountRequestDTO,
                                                  HttpServletRequest request, HttpServletResponse response) {
        return userApiService.deleteAccount(deleteAccountRequestDTO.getAccessToken(), request, response);
    }

}