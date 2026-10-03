package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AgentCategoryRepository agentCategoryRepository;
    private final CategoryRepository categoryRepository;

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

    public UserResponse reactivateUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId + " was not found"));

        user.setStatus(UserStatus.ACTIVE);
        User save = userRepository.save(user);
        return userMapper.toRespond(save);
    }

    public void assignCategoryToAgent(Long agentId, List<Long> categoryIds){
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + agentId + " is not found"));
        if(agent.getRole() != Role.AGENT){
            throw new BusinessRuleException("Categories can only be assigned to agents");
        }

        for(Long categoryId : categoryIds){
            //checks table if it exists
            if(agentCategoryRepository.existsByAgentIdAndCategoryId(agentId, categoryId)){
                continue;
            }
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + categoryId + "is not found"));
            AgentCategory assign = new AgentCategory();
            assign.setAgent(agent);
            assign.setCategory(category);
            agentCategoryRepository.save(assign);
        }
    }
}
