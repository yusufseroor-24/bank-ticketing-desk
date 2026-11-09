package com.ga.bankdesk.config;

import com.ga.bankdesk.enums.KycStatus;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Profile("dev")
@RequiredArgsConstructor
@Component
@Order(3)
//seeding demo users (dev profile only)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args){
        if(userRepository.count() > 0){
            return;
        }

        seedUser("practice24in+admin@gmail.com", "Admin User", Role.ADMIN);
        seedUser("practice24in+agent.security@gmail.com", "Agent Security", Role.AGENT);
        seedUser("practice24in+agent.general@gmail.com", "Agent General", Role.AGENT);
        seedUser("practice24in+customer1@gmail.com", "Customer One", Role.CUSTOMER);
        seedUser("practice24in+customer2@gmail.com", "Customer Two", Role.CUSTOMER);
    }

    private User seedUser(String email, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true); //seeded users skip the email-verification step
        if (role == Role.CUSTOMER) { //demo customers start verified
            user.setKycStatus(KycStatus.APPROVED);
            user.setKycVerifiedAt(Instant.now());
        }
        return userRepository.save(user);
    }
}
