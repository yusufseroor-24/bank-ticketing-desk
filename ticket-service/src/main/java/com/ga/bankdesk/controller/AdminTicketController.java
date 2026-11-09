package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.AssignCategoriesRequest;
import com.ga.bankdesk.dto.CategoryRequest;
import com.ga.bankdesk.dto.ReassignTicketRequest;
import com.ga.bankdesk.dto.ReopenTicketRequest;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.dto.UpdateSlaPolicyRequest;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.SlaPolicy;
import com.ga.bankdesk.security.CurrentUser;
import com.ga.bankdesk.service.AdminTicketService;
import com.ga.bankdesk.service.CategoryService;
import com.ga.bankdesk.service.SlaService;
import com.ga.bankdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class AdminTicketController {

    private final AdminTicketService adminService;
    private final CategoryService categoryService;
    private final TicketService ticketService;
    private final SlaService slaService;

    @PostMapping("/categories")
    public Category createCategory(@Valid @RequestBody CategoryRequest request){
        return categoryService.create(request.name(), request.visibilityToCustomers());
    }

    @PutMapping("/categories/{categoryId}")
    public Category updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(categoryId, request.visibilityToCustomers(), true);
    }

    @PostMapping("/users/{agentId}/categories")
    public ResponseEntity<Void> assignCategory(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long agentId, @Valid @RequestBody AssignCategoriesRequest request){
        adminService.assignCategoryToAgent(currentUser, agentId, request.categoryIds());
        return ResponseEntity.noContent().build(); //updates but nothing to hand back (204, No content)
    }

    @PutMapping("/tickets/{ticketId}/reopen")
    public TicketCreationResponse reopenTicket(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long ticketId, @Valid @RequestBody ReopenTicketRequest request){
        return ticketService.reopenTicket(currentUser, ticketId, request.reason());
    }

    @PutMapping("/tickets/{ticketId}/reassign")
    public TicketCreationResponse reassignTicket(@PathVariable Long ticketId, @Valid @RequestBody ReassignTicketRequest request){
        return ticketService.reassignTicket(ticketId, request.agentId());
    }

    @GetMapping("/sla-policies")
    public List<SlaPolicy> listSlaPolicies(){
        return slaService.listPolicies();
    }

    @PutMapping("/sla-policies/{policyId}")
    public SlaPolicy updateSlaPolicy(@PathVariable Long policyId, @Valid @RequestBody UpdateSlaPolicyRequest request){
        return slaService.updatePolicy(policyId, request.firstResponseMinutes(), request.resolutionMinutes());
    }
}
