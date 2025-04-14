package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.UserApiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class UserApiController {

    private final UserApiService userApiService;

    @PutMapping("/nickname")
    public ResponseEntity<NicknameUpdateResponseDTO> updateNickname(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody NicknameUpdateRequestDTO nicknameUpdateRequestDTO) {

        return userApiService.updateNickname(accessToken, nicknameUpdateRequestDTO.getNickname());
    }

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

    @PutMapping("/profile-image")
    public ResponseEntity<ProfileImageUpdateResponseDTO> updateProfileImage(
            @RequestHeader("Authorization") String accessToken,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        return userApiService.updateProfileImage(accessToken, profileImage);
    }


    @PutMapping("/delete")
    public ResponseEntity<DeleteAccountResponseDTO> deleteAccount(
            @RequestHeader("Authorization") String accessToken,
            HttpServletRequest request,
            HttpServletResponse response) {

        return userApiService.deleteAccount(accessToken, request, response);
    }

    @GetMapping("/info")
    public ResponseEntity<UserInfoResponseDTO> userInfo(
            @RequestHeader("Authorization") String accessToken
    ) {
        return userApiService.userInfo(accessToken);
    }
}
