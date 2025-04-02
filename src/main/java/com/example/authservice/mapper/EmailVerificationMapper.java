package com.example.authservice.mapper;

import com.example.authservice.model.EmailVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailVerificationMapper {

    void insertEmailVerification(EmailVerification verification);

    EmailVerification findByEmail(@Param("email") String email);

    EmailVerification findByEmailAndCode(@Param("email") String email, @Param("code") String code);

    void updateEmailVerification(EmailVerification verification);
}
