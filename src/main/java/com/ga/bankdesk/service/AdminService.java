package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.mapper.UserMapper;
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
}
