package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.RegisterRequest;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.model.Role;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.model.UserStatus;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse register(RegisterRequest request){
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
