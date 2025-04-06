package com.example.authservice.controller;

import com.example.authservice.dto.NicknameUpdateRequestDTO;
import com.example.authservice.dto.NicknameUpdateResponseDTO;
import com.example.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @PutMapping("/nickname")
    public NicknameUpdateResponseDTO updateNickname(@RequestBody NicknameUpdateRequestDTO requestDTO) {
        return userService.updateNickname(requestDTO.getNickname());
    }

}
