package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserMapper userMapper;

    //get currently logged user for the token that was checked in JwtAuthFilter
    @GetMapping("/me")
    public UserResponse getCurrentLoggedUser(@AuthenticationPrincipal AppUserDetails userDetails){
        return userMapper.toRespond(userDetails.getUser());
    }

    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminTest(){
        return "You are an admin";
    }
}
