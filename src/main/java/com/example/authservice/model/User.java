package com.example.authservice.model;

import com.example.authservice.type.Provider;
import com.example.authservice.type.Role;
import com.example.authservice.type.Status;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Builder
public class User {
    private Long id;
    private String email;
    private String password;
    private String nickname;
    private String phoneNumber;
    private Role role;
    private Provider provider;
    private String providerId;
    private String profileImage;
    private Status status;
    private Timestamp statusChangedAt;
    private Timestamp createdAt;
}