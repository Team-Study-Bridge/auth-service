package com.example.authservice.mapper;

import com.example.authservice.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OAuth2UserMapper {

    void insertOAuthUser(User newUser);

    User findByUserIdAndProvider(@Param("providerId") String providerId, @Param("provider") String provider);
}
