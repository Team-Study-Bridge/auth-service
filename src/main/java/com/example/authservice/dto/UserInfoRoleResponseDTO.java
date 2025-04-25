package com.example.authservice.dto;

import com.example.authservice.type.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoRoleResponseDTO {
    boolean success;
    String message;
    Role role;
}
