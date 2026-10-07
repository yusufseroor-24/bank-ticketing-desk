package com.ga.bankdesk.config;

import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
@Order(2)
//seeding users
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AgentCategoryRepository agentCategoryRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args){
        if(userRepository.count() > 0){
            return;
        }

        User admin = seedUser("admin@bankdesk.com", "Admin User", Role.ADMIN);

        User agentInternal = seedUser("agent.security@bankdesk.com", "Agent Security", Role.AGENT);
        assignCategories(agentInternal, "AML", "FRAUD", "IT_SECURITY");

        User agentGeneral = seedUser("agent.general@bankdesk.com", "Agent General", Role.AGENT);
        assignCategories(agentGeneral, "KYC_REVIEW", "CARD_DISPUTE", "LOAN_ACCOUNT", "COMPLAINT");

        seedUser("customer1@bankdesk.com", "Customer One", Role.CUSTOMER);
        seedUser("customer2@bankdesk.com", "Customer Two", Role.CUSTOMER);
    }

    private User seedUser(String email, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true); //seeded users skip the email-verification step
        return userRepository.save(user);
    }

    private void assignCategories(User agent, String... categoryNames) {
        for (String name : categoryNames) {
            Optional<Category> category = categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equals(name))
                    .findFirst();
            category.ifPresent(c -> {
                AgentCategory link = new AgentCategory();
                link.setAgent(agent);
                link.setCategory(c);
                agentCategoryRepository.save(link);
            });
        }
    }

}
