package com.ga.bankdesk.service;

import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> listAllCategories(){
        return categoryRepository.findAll();
    }

    public List<Category> listVisibilityToCustomers(){
        return categoryRepository.findByVisibilityToCustomersTrue();
    }

}
