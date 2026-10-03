package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.CategoryRequest;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public List<Category> listCategories(@AuthenticationPrincipal AppUserDetails userDetails){
        boolean isCustomer = userDetails.getUser().getRole().name().equals("CUSTOMER");
        return isCustomer ? categoryService.listVisibilityToCustomers() : categoryService.listAllCategories();
    }
}
