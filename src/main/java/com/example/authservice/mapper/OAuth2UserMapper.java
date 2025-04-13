package com.example.authservice.mapper;

import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OAuth2UserMapper {

    void insertOAuthUser(User newUser);

    User findByUserIdAndProvider(@Param("providerId") String providerId, @Param("provider") Provider provider);

    void updateUserWithSocialInfo(Long id, String providerId, Provider provider);
}
