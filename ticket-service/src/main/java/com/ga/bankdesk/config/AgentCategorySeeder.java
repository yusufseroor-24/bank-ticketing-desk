package com.ga.bankdesk.config;

import com.ga.bankdesk.client.IdentityClient;
import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import com.ga.bankdesk.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Profile("dev")
@RequiredArgsConstructor
@Component
@Order(3)
public class AgentCategorySeeder implements CommandLineRunner {

    private final IdentityClient identityClient;
    private final CategoryRepository categoryRepository;
    private final AgentCategoryRepository agentCategoryRepository;

    @Override
    public void run(String... args){
        if(agentCategoryRepository.count() > 0){
            return;
        }
        assignCategories("practice24in+agent.security@gmail.com", "AML", "FRAUD", "IT_SECURITY");
        assignCategories("practice24in+agent.general@gmail.com", "KYC_REVIEW", "CARD_DISPUTE", "LOAN_ACCOUNT", "COMPLAINT");
    }

    private void assignCategories(String agentEmail, String... categoryNames) {
        UserSummary agent = identityClient.findByEmail(agentEmail).orElseThrow();
        for (String name : categoryNames) {
            Optional<Category> category = categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equals(name))
                    .findFirst();
            category.ifPresent(c -> {
                AgentCategory link = new AgentCategory();
                link.setAgentId(agent.id());
                link.setCategory(c);
                agentCategoryRepository.save(link);
            });
        }
    }
}
