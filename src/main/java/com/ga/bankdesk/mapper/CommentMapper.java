package com.ga.bankdesk.mapper;

import com.ga.bankdesk.dto.CommentResponse;
import com.ga.bankdesk.model.TicketComments;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(TicketComments comment){
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getEmail(),
                comment.getCommentContent(),
                comment.getCreatedAt()
        );
    }
}
