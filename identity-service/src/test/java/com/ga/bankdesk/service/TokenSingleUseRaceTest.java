package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.TokenType;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.model.Token;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.TokenRepository;
import com.ga.bankdesk.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TokenSingleUseRaceTest{

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void resetTokenWorksExactlyOnceWhenUsedByMultipleRequestsAtOnce() throws Exception{
        int attempts = 6;

        User user = new User();
        user.setEmail("token.race." + UUID.randomUUID() + "@bankdesk.com");
        user.setPassword(passwordEncoder.encode("OriginalPass1!"));
        user.setFullName("Token Race User");
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        User savedUser = userRepository.save(user);

        Token token = new Token();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(savedUser);
        token.setTokenType(TokenType.PASSWORD_RESET);
        token.setExpireAt(LocalDateTime.now().plusMinutes(30));
        tokenRepository.save(token);
        String tokenString = token.getToken();

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch allReady = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<String>> outcomes = new ArrayList<>();
        for(int i=0; i < attempts; i++ ){
            //create new passwords
            String attemptedPassword = "NewPassword" + i + "!";
            outcomes.add(pool.submit(() -> {
                allReady.countDown();
                start.await();
                try{
                    authService.resetPassword(tokenString, attemptedPassword);
                    return "SUCCESS:" + attemptedPassword;
                }catch(BusinessRuleException e){
                    return "REFUSED";
                }
            }));
        }
        allReady.await();
        start.countDown();

        int successes = 0;
        String chosenPassword = null;
        for(Future<String> outcome : outcomes){
            String result = outcome.get(30, TimeUnit.SECONDS);
            if(result.startsWith("SUCCESS:")){
                successes++;
                chosenPassword = result.substring("SUCCESS:".length());
            }
        }
        pool.shutdown();

        assertEquals(1, successes);

        //checks if the chosen password get changed to used in database
        assertTrue(tokenRepository.findByToken(tokenString).orElseThrow().isUsed());
        //reload user from database
        User reloaded = userRepository.findById(savedUser.getId()).orElseThrow();
        //check the chosen password was saved
        assertTrue(passwordEncoder.matches(chosenPassword, reloaded.getPassword()));
    }
}
