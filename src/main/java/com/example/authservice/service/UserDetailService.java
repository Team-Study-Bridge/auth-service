//package com.example.authservice.service;
//
//
//import com.example.authservice.config.security.CustomUserDetails;
//import com.example.authservice.mapper.UserMapper;
//import com.example.authservice.model.User;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class UserDetailService implements UserDetailsService {
//
//    private final UserMapper userMapper;
//
//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//            User userByEmail = userMapper.findByEmail(email);
//            System.out.println("userByEmail: " + userByEmail);
//
//            if (userByEmail == null) {
//                throw new UsernameNotFoundException(email + " not found");
//            }
//
//            return CustomUserDetails.builder()
//                    .user(userByEmail)
//                    .roles(List.of(userByEmail.getRole()))
//                    .build();
//
//    }
//}
