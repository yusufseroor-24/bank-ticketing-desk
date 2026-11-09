package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.model.AuditLog;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.AuditLogRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    //allows the user to view all the users of the app
    public List<UserResponse> listAllUsers(){
        return userRepository.findAll().stream()
                .map(userMapper::toRespond)
                .toList();
    }

    //allows the user to change the roles of the users since customer is the default
    public UserResponse changeRole(User admin, Long userId, Role newRole){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " was not found"));

        user.setRole(newRole);
        User save = userRepository.save(user);

        auditLogService.log(admin, "ROLE_CHANGED: " + user.getEmail() + " -> " + newRole, "User", user.getId());
        return userMapper.toRespond(save);
    }

    //allow the admin to soft delete a user account
    public UserResponse deactivateUser(User admin, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " was not found"));

        user.setStatus(UserStatus.INACTIVE);
        User save = userRepository.save(user);

        auditLogService.log(admin, "USER_DEACTIVATED: " + user.getEmail(), "User", user.getId());
        log.info("User {} deactivated by admin {}", user.getEmail(), admin.getEmail());
        return userMapper.toRespond(save);
    }

    public UserResponse reactivateUser(User admin, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " was not found"));

        user.setStatus(UserStatus.ACTIVE);
        User save = userRepository.save(user);

        auditLogService.log(admin, "USER_REACTIVATED: " + user.getEmail(), "User", user.getId());
        log.info("User {} reactivated by admin {}", user.getEmail(), admin.getEmail());
        return userMapper.toRespond(save);
    }

    public Page<AuditLog> listAuditLogs(Pageable pageable){
        return auditLogRepository.findAll(pageable);
    }
}