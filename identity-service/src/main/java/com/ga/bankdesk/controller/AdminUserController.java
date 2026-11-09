package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.ChangeRoleRequest;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.model.AuditLog;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
//admin authorization
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminService;

    @GetMapping("/users")
    public List<UserResponse> listAllUsers(){
        return adminService.listAllUsers();
    }

    @PutMapping("/users/{userId}/role")
    public UserResponse changeRole(@AuthenticationPrincipal AppUserDetails userDetails,
                                   @PathVariable Long userId, @Valid @RequestBody ChangeRoleRequest request){
        return adminService.changeRole(userDetails.getUser(), userId, request.newRole());
    }

    @PutMapping("/users/{userId}/deactivate")
    public UserResponse deactivateUser(@AuthenticationPrincipal AppUserDetails userDetails,
                                       @PathVariable Long userId){
        return adminService.deactivateUser(userDetails.getUser(), userId);
    }

    @PutMapping("/users/{userId}/reactivate")
    public UserResponse reactivateUser(@AuthenticationPrincipal AppUserDetails userDetails,
                                       @PathVariable Long userId){
        return adminService.reactivateUser(userDetails.getUser(), userId);
    }

    @GetMapping("/audit-logs")
    public Page<AuditLog> listAuditLogs(Pageable pageable){
        return adminService.listAuditLogs(pageable);
    }
}