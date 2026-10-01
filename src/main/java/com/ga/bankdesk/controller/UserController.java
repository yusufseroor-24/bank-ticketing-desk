package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.ChangePassword;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserMapper userMapper;
    private final AuthService authService;

    //get currently logged user for the token that was checked in JwtAuthFilter
    @GetMapping("/me")
    public UserResponse getCurrentLoggedUser(@AuthenticationPrincipal AppUserDetails userDetails){
        return userMapper.toRespond(userDetails.getUser());
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal AppUserDetails userDetails,
                                                 @Valid @RequestBody ChangePassword request){
        authService.changePassword(userDetails.getUser(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok("Your password has been changed successfully");
    }

    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminTest(){
        return "You are an admin";
    }
}
