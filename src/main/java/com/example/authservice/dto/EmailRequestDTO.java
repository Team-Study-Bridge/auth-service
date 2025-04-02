package com.example.authservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRequestDTO {
    private String email;
    private String code; // verify-code 요청에 필요한 필드
}
