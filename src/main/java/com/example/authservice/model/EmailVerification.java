package com.example.authservice.model;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@Builder
public class EmailVerification {

    private Long id;
    private String email;
    private String code;
    private boolean isVerified;
    private Timestamp createdAt;
}
