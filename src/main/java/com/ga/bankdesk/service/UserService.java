package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;

    public UserResponse updateProfile(User currentUser, String fullName){
        currentUser.setFullName(fullName);
        User updatedUser = userRepository.save(currentUser);
        return userMapper.toRespond(updatedUser); //convert UserResponse to not expose sensitive fields
    }

    public UserResponse uploadProfilePic(User currentUser, MultipartFile file){
        String fileName = fileStorageService.store(file);
        currentUser.setProfilePicPath(fileName);
        User save = userRepository.save(currentUser);
        return userMapper.toRespond(save);
    }

}
