package com.ga.bankdesk.config;


import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static org.hibernate.engine.internal.Versioning.seed;

//database seeding of categories
@RequiredArgsConstructor
@Component
public class CategoryData implements CommandLineRunner {
    private final CategoryRepository categoryRepository;

    private void seed(String name, int slaHours, TicketPriority defaultPriority, boolean visibilityToCustomers){
        Category category = new Category();
        category.setName(name);
        category.setSlaHours(slaHours);
        category.setDefaultPriority(defaultPriority);
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
        seed("AML", 48, TicketPriority.HIGH, false);
        seed("FRAUD", 4, TicketPriority.CRITICAL, false);
        seed("IT_SECURITY", 8, TicketPriority.HIGH, false);
        seed("KYC_REVIEW", 72, TicketPriority.MEDIUM, true);
        seed("CARD_DISPUTE", 72, TicketPriority.MEDIUM, true);
        seed("LOAN_ACCOUNT", 72, TicketPriority.MEDIUM, true);
        seed("COMPLAINT", 96, TicketPriority.LOW, true);
    }
}
