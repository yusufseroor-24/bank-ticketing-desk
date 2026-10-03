package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.ChangeRoleRequest;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<UserResponse> listAllUsers(){
        return adminService.listAllUsers();
    }

    @PutMapping("/{userId}/role")
    public UserResponse changeRole(@PathVariable Long userId, @Valid @RequestBody ChangeRoleRequest request){
        return adminService.changeRole(userId, request.newRole());
    }

    @PutMapping("/{userId}/deactivate")
    public UserResponse deactivateUser(@PathVariable Long userId){
        return adminService.deactivateUser(userId);
    }
}
