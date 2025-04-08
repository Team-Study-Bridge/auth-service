package com.example.authservice.controller;

import com.example.authservice.dto.NicknameUpdateRequestDTO;
import com.example.authservice.dto.NicknameUpdateResponseDTO;
import com.example.authservice.service.UserApiService;
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
    public NicknameUpdateResponseDTO updateNickname(@RequestBody NicknameUpdateRequestDTO nicknameUpdateRequestDTO, String accessToken) {
        return userApiService.updateNickname(nicknameUpdateRequestDTO.getNickname(), accessToken);
    }

}
