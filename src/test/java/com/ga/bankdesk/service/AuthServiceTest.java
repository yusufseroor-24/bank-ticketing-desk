package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.LoginRequest;
import com.ga.bankdesk.dto.RegisterRequest;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional //rolls back after each test
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @MockitoBean
    private EmailService emailService; //real emails never sent during testing

    @Test
    void registerCreatesUnverifiedCustomer(){
        UserResponse response = authService.register(new RegisterRequest("newperson@bank.com", "password123", "New Person"));
        assertEquals(Role.CUSTOMER, response.role());
        assertFalse(response.emailVerified());
    }

    @Test
    void registerDuplicateEmailThrowsConflict() {
        authService.register(new RegisterRequest("duplicated@bank.com", "password123", "First dupe"));
        RegisterRequest duplicate = new RegisterRequest("duplicated@bank.com", "password456", "Second dupe");
        assertThrows(ConflictException.class, () -> authService.register(duplicate));
    }

    @Test
    void loginUnverifiedAccountThrowsBusinessRule() {
        authService.register(new RegisterRequest("unverified@bank.com", "password123", "Not Verified"));
        LoginRequest login = new LoginRequest("unverified@bank.com", "password123");
        assertThrows(BusinessRuleException.class, () -> authService.login(login));
    }

    @Test
    void loginWrongPasswordThrowsAuthenticationException() {
        LoginRequest login = new LoginRequest("admin@bankdesk.com", "wrong-password");
        assertThrows(AuthenticationException.class, () -> authService.login(login));
    }

}