package com.example.authservice.mapper;

import com.example.authservice.model.EmailVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Mapper
public interface EmailVerificationMapper {

    void insertEmailVerification(EmailVerification verification);

    EmailVerification findByEmail(@Param("email") String email);

    void updateEmailCode(@Param("email") String email, @Param("code") String code, @Param("isVerified") Boolean isVerified,  @Param("createdAt") Timestamp createdAt);

    void updateEmailVerification(EmailVerification verification);

    void deleteByEmail(@Param("email") String email);
}
