package com.example.authservice.controller;

import com.example.authservice.dto.UserJoinRequestDTO;
import com.example.authservice.dto.UserJoinResponseDTO;
import com.example.authservice.dto.UserLoginRequestDTO;
import com.example.authservice.dto.UserLoginResponseDTO;
import com.example.authservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auths")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public UserJoinResponseDTO join(@RequestBody UserJoinRequestDTO userJoinRequestDTO, HttpServletResponse response) {
        return userService.save(userJoinRequestDTO, response);
    }

    @PostMapping("/login")
    public UserLoginResponseDTO login(@RequestBody UserLoginRequestDTO userLoginRequestDTO, HttpServletResponse response) {
        return userService.login(userLoginRequestDTO.getEmail(), userLoginRequestDTO.getPassword(), response);
    }

    @DeleteMapping("/logout")
    public UserLoginResponseDTO logout(@RequestBody String accessToken, HttpServletRequest request, HttpServletResponse response) {
        return userService.logout(accessToken, request, response);
    }


}
