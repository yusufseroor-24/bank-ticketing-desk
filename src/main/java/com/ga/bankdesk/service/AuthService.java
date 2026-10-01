package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.LoginRequest;
import com.ga.bankdesk.dto.LoginResponse;
import com.ga.bankdesk.dto.RegisterRequest;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.enums.TokenType;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.model.Token;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.repository.TokenRepository;
import com.ga.bankdesk.repository.UserRepository;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    public UserResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new ConcurrentModificationException("An account with this email already exists.");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.CUSTOMER); //self reg is always customer
        user.setStatus(UserStatus.ACTIVE);

        User save = userRepository.save(user);
        createAndSendVerificationToken(save);
        return userMapper.toRespond(save);
    }

    public LoginResponse login(LoginRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        if(!userDetails.getUser().isEmailVerified()){
            throw new BusinessRuleException("Please verify your email before logging in");
        }
        String token = jwtUtils.generateToken(userDetails.getUsername());
        return new LoginResponse(token);
    }

    public void verifyEmail(String tokenString){
        Token verificationToken = tokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new BusinessRuleException("Invalid verification link"));
        if(verificationToken.getTokenType() != TokenType.EMAIL_VERIFICATION){
            throw new BusinessRuleException("Invalid verification link");
        }
        if(verificationToken.isUsed()){
            throw new BusinessRuleException("This verification link is already being used or has been used");
        }
        if(verificationToken.getExpireAt().isBefore(LocalDateTime.now())){
            throw new BusinessRuleException("This verification link has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
    }

    public void resendVerification(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email"));
        if(user.isEmailVerified()){
            throw new BusinessRuleException("This account email is already verified");
        }
        createAndSendVerificationToken(user);
    }

    private void createAndSendVerificationToken(User user){
        Token verificationToken = new Token();
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setUser(user);
        verificationToken.setTokenType(TokenType.EMAIL_VERIFICATION);
        verificationToken.setExpireAt(LocalDateTime.now().plusHours(24));
        tokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());
    }

    public void forgetPassword(String email){
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    Token resetToken = new Token();
                    resetToken.setToken(UUID.randomUUID().toString());
                    resetToken.setUser(user);
                    resetToken.setTokenType(TokenType.PASSWORD_RESET);
                    resetToken.setExpireAt(LocalDateTime.now().plusMinutes(30));
                    tokenRepository.save(resetToken);

                    emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
        });
    }

    public void resetPassword(String tokenString, String newPassword){
        Token resetPassToken = tokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new BusinessRuleException("Invalid verification link"));
        if(resetPassToken.getTokenType() != TokenType.PASSWORD_RESET){
            throw new BusinessRuleException("Invalid reset password link");
        }
        if(resetPassToken.isUsed()){
            throw new BusinessRuleException("This reset password link is already being used or has been used");
        }
        if(resetPassToken.getExpireAt().isBefore(LocalDateTime.now())){
            throw new BusinessRuleException("This reset password link has expired");
        }

        User user = resetPassToken.getUser();
        user.setPassword(newPassword);
        userRepository.save(user);

        resetPassToken.setUsed(true);
        tokenRepository.save(resetPassToken);
    }



}
