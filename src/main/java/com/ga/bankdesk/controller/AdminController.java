package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.model.AuditLog;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.AdminService;
import com.ga.bankdesk.service.CategoryService;
import com.ga.bankdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
//admin authorization
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final CategoryService categoryService;
    private final TicketService ticketService;

    @GetMapping("/users")
    public List<UserResponse> listAllUsers(){
        return adminService.listAllUsers();
    }

    @PutMapping("/{userId}/role")
    public UserResponse changeRole(@AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long userId, @Valid @RequestBody ChangeRoleRequest request){
        return adminService.changeRole(userDetails.getUser(), userId, request.newRole());
    }

    @PutMapping("/{userId}/deactivate")
    public UserResponse deactivateUser(@AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long userId){
        return adminService.deactivateUser(userDetails.getUser(), userId);
    }

    @PutMapping("/{userId}/reactivate")
    public UserResponse reactivateUser(@AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long userId){
        return adminService.reactivateUser(userDetails.getUser(), userId);
    }

    @PostMapping("/categories")
    public Category createCategory(@Valid @RequestBody CategoryRequest request){
        return categoryService.create(request.name(), request.visibilityToCustomers());
    }

    @PutMapping("/categories/{categoryId}")
    public Category updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(categoryId, request.visibilityToCustomers(), true);
    }

    @PostMapping("/users/{agentId}/categories")
    public ResponseEntity<Void> assignCategory(@AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long agentId, @Valid @RequestBody AssignCategoriesRequest request){
        adminService.assignCategoryToAgent(userDetails.getUser(), agentId, request.categoryIds());
        return ResponseEntity.noContent().build(); //updates but nothing to hand back (204, No content)
    }

    @PutMapping("/tickets/{ticketId}/reopen")
    public TicketCreationResponse reopenTicket(@AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long ticketId, @Valid @RequestBody ReopenTicketRequest request){
        return ticketService.reopenTicket(userDetails.getUser(), ticketId, request.reason());
    }

    @PutMapping("/tickets/{ticketId}/reassign")
    public TicketCreationResponse reassignTicket(@PathVariable Long ticketId, @Valid @RequestBody ReassignTicketRequest request){
        return ticketService.reassignTicket(ticketId, request.agentId());
    }

    @GetMapping("/audit-logs")
    public Page<AuditLog> listAuditLogs(Pageable pageable){
        return adminService.listAuditLogs(pageable);
    }

}
