package com.ga.bankdesk.mapper;

import com.ga.bankdesk.client.IdentityClient;
import com.ga.bankdesk.dto.CommentResponse;
import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.model.TicketComments;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class CommentMapper {

    //the author's email now lives in identity-service
    private final IdentityClient identityClient;

    public CommentResponse toResponse(TicketComments comment){
        return toResponses(List.of(comment)).get(0);
    }

    //one call to identity-service for all authors
    public List<CommentResponse> toResponses(List<TicketComments> comments){
        Set<Long> ids = comments.stream().map(TicketComments::getAuthorId).collect(Collectors.toSet());
        Map<Long, String> emails = identityClient.getUsers(ids).stream()
                .collect(Collectors.toMap(UserSummary::id, UserSummary::email));
        return comments.stream()
                .map(c -> new CommentResponse(
                        c.getId(),
                        emails.getOrDefault(c.getAuthorId(), "UNKNOWN"),
                        c.getCommentContent(),
                        c.getCreatedAt()))
                .toList();
    }
}
