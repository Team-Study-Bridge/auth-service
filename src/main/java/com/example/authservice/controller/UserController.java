package com.example.authservice.controller;

import com.example.authservice.dto.*;
import com.example.authservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auths")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public ResponseEntity<UserJoinResponseDTO> join(@RequestBody UserJoinRequestDTO userJoinRequestDTO,
                                                    @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
                                                    HttpServletResponse response) {
        return userService.save(userJoinRequestDTO, profileImage, response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDTO> login(@RequestBody UserLoginRequestDTO userLoginRequestDTO, HttpServletResponse response) {
        return userService.login(userLoginRequestDTO.getEmail(), userLoginRequestDTO.getPassword(), response);
    }
    @PostMapping("/force-login")
    public ResponseEntity<UserLoginResponseDTO> forceLogin(@RequestBody UserLoginRequestDTO userLoginRequestDTO, HttpServletResponse response) {
        return userService.forceLogin(userLoginRequestDTO.getEmail(), userLoginRequestDTO.getPassword(), response);
    }

    @DeleteMapping("/logout")
    public ResponseEntity<UserLoginResponseDTO> logout(@RequestHeader("Authorization") String accessToken,
                                                       HttpServletRequest request,
                                                       HttpServletResponse response) {
        return userService.logout(accessToken, request, response);
    }
}