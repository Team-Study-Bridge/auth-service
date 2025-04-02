package com.example.authservice.mapper;

import com.example.authservice.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    void insertUser(User user);

    User findByEmail(String email);

    void updateUser(User user);

    void deleteUserById(Long id);


}
