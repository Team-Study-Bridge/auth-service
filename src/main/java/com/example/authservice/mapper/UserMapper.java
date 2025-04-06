package com.example.authservice.mapper;

import com.example.authservice.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    void insertUser(User user);

    User findByEmail(@Param("email") String email);

    void updateNickname(@Param("nickname") String nickname);

    void deleteUserById(Long id);

}
