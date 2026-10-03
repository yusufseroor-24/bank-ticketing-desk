package com.ga.bankdesk.controller;

import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<Category> listCategories(@AuthenticationPrincipal AppUserDetails userDetails){
        boolean isCustomer = userDetails.getUser().getRole().name().equals("CUSTOMER");
        return isCustomer ? categoryService.listVisibilityToCustomers() : categoryService.listAllCategories();
    }
}
