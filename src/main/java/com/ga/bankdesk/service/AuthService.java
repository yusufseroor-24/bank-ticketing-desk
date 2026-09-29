package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.RegisterRequest;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ConcurrentModificationException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

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
        return userMapper.toRespond(save);
    }

}
