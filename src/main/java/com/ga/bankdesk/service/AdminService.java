package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserResponse> listAllUsers(){
        return userRepository.findAll().stream()
                .map(userMapper::toRespond)
                .toList();
    }

    public UserResponse changeRole(Long userId, Role newRole){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " was not found"));

        user.setRole(newRole);
        User save = userRepository.save(user);
        return userMapper.toRespond(save);
    }

    public UserResponse deactivateUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " was not found"));

        user.setStatus(UserStatus.INACTIVE);
        User save = userRepository.save(user);
        return userMapper.toRespond(save);
    }
}
