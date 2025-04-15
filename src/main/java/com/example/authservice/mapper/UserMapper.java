package com.example.authservice.mapper;

import com.example.authservice.model.User;
import com.example.authservice.type.Role;
import com.example.authservice.type.Status;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    void insertUser(User user);

    User findById(@Param("id") Long userId);

    User findByEmail(@Param("email") String email);

    String findEmailByEmail(@Param("email") String email);

    Status findStatusById(@Param("id") Long id);

    void updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    void updatePassword(@Param("id") Long userId, @Param("password") String encryptedPassword);

    void updateStatus(@Param("id") Long id, @Param("status") String status);

    void deleteUserById(Long userId);

    void deactivateUser(@Param("id") Long userId);

    void updateProfileImage(@Param("id") Long id, @Param("profileImage") String profileImage);

    Role isAdmin(@Param("id") Long userId);
}
