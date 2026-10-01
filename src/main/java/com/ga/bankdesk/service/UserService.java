package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse updateProfile(User currentUser, String fullName){
        currentUser.setFullName(fullName);
        User updatedUser = userRepository.save(currentUser);
        return userMapper.toRespond(updatedUser); //convert UserResponse to not expose sensitive fields
    }

}
