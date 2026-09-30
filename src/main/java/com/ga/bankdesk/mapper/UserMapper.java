package com.ga.bankdesk.mapper;

import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.model.User;
import org.springframework.stereotype.Component;

//UserMapper is used to get a User from the database and build a UserResponse from it
@Component //spring create and manage the object
public class UserMapper {

    public UserResponse toRespond(User user){
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
