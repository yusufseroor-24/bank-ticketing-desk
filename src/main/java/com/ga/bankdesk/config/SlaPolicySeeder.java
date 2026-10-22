package com.ga.bankdesk.config;

import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.SlaPolicy;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.SlaPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeding one SLA policy row per category and priority
 * Runs after categories are seeded
 */

@RequiredArgsConstructor
@Component
@Order(2)
public class SlaPolicySeeder implements CommandLineRunner {

    private final SlaPolicyRepository slaPolicyRepository;
    private final CategoryRepository categoryRepository;

    //security related categories that gets a tighter SLA and urgency
    private static final Set<String> URGENT_CATEGORIES = Set.of("AML", "FRAUD", "IT_SECURITY");

    @Override
    public void run(String... args){
        if(slaPolicyRepository.count() > 0){
            return;
        }

        for(Category category : categoryRepository.findAll()){
            boolean urgent = URGENT_CATEGORIES.contains(category.getName());
            for(TicketPriority priority : TicketPriority.values()){
                SlaPolicy policy = new SlaPolicy();
                policy.setCategory(category);
                policy.setPriority(priority);
                policy.setFirstResponseMinutes(firstResponseMinutes(priority, urgent));
                policy.setResolutionMinutes(resolutionMinutes(priority, urgent));
            }
        }
    }

    private int firstResponseMinutes(TicketPriority priority, boolean urgent){
        return switch (priority){
            case CRITICAL -> urgent ? 30 : 60;
            case HIGH -> urgent ? 120 : 240;
            case MEDIUM -> urgent ? 240 : 480;
            case LOW -> urgent ? 720 : 1440;
        };
    }

    private int resolutionMinutes(TicketPriority priority, boolean urgent){
        return switch (priority) {
            case CRITICAL -> urgent ? 120 : 240; // 2h / 4h
            case HIGH -> urgent ? 720 : 1440; // 12h / 24h
            case MEDIUM -> urgent ? 2160 : 4320; // 36h / 72h
            case LOW -> urgent ? 4320 : 7200; // 72h / 120h
        };
    }
}
