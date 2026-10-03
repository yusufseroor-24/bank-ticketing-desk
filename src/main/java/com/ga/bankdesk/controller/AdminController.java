package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.CategoryRequest;
import com.ga.bankdesk.dto.ChangeRoleRequest;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.service.AdminService;
import com.ga.bankdesk.service.CategoryService;
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
    private final CategoryService categoryService;

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

    @PutMapping("/{userId}/reactivate")
    public UserResponse reactivateUser(@PathVariable Long userId){
        return adminService.reactivateUser(userId);
    }

    @PostMapping("/categories")
    public Category createCategory(@Valid @RequestBody CategoryRequest request){
        return categoryService.create(request.name(), request.slaHours(), request.visibilityToCustomers());
    }

    @PutMapping("/categories/{categoryId}")
    public Category updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(categoryId, request.slaHours(), request.visibilityToCustomers(), true);
    }
}
