package com.ga.bankdesk.config;


import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static org.hibernate.engine.internal.Versioning.seed;

//database seeding of categories
@RequiredArgsConstructor
@Component
@Order(1)
public class CategoryData implements CommandLineRunner {
    private final CategoryRepository categoryRepository;

    private void seed(String name , boolean visibilityToCustomers){
        Category category = new Category();
        category.setName(name);
        category.setVisibilityToCustomers(visibilityToCustomers);
        category.setActive(true);
        categoryRepository.save(category);
    }

    @Override
    public void run(String... args){
        //if already seeded, don't duplicated
        if(categoryRepository.count() > 0){
            return;
        }
        seed("AML", false);
        seed("FRAUD", false);
        seed("IT_SECURITY", false);
        seed("KYC_REVIEW" , true);
        seed("CARD_DISPUTE" , true);
        seed("LOAN_ACCOUNT" , true);
        seed("COMPLAINT", true);
    }
}
