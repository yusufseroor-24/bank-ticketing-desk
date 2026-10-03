package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.exception.ResourceNotFoundException;
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

    public Category create(String name, int slaHours, TicketPriority defaultPriority, boolean visibilityToCustomers){
        Category category = new Category();
        category.setName(name);
        category.setSlaHours(slaHours);
        category.setDefaultPriority(defaultPriority);
        category.setVisibilityToCustomers(visibilityToCustomers);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    public Category update(Long categoryId, int slaHours, TicketPriority defaultPriority, boolean visibilityToCustomers, boolean active){
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + categoryId  + " was not found"));
        category.setSlaHours(slaHours);
        category.setDefaultPriority(defaultPriority);
        category.setVisibilityToCustomers(visibilityToCustomers);
        category.setActive(active);
        return categoryRepository.save(category);
    }

}
