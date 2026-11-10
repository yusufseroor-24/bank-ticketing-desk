package com.ga.bankdesk.service;

import com.ga.bankdesk.client.AiClient;
import com.ga.bankdesk.client.IdentityClient;
import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.dto.CommentResponse;
import com.ga.bankdesk.dto.TicketCreationResponse;
import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.enums.KycStatus;
import com.ga.bankdesk.exception.BusinessRuleException;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AiAssistService {

    private static final int MAX_COMMENTS = 30;
    private static final int MAX_COMMENT_CHARS = 1000;
    private static final int MAX_DESCRIPTION_CHARS = 3000;

    private final AiClient aiClient;
    private final AiRateLimiter rateLimiter;
    private final CategoryRepository categoryRepository;
    private final IdentityClient identityClient;
    private final TicketService ticketService;

    public DraftResponse draftTicket(CurrentUser customer, DraftTicketRequest request) {
        rateLimiter.check(customer.getId());

        List<Category> categories = categoryRepository.findByVisibilityToCustomersTrue().stream()
                .filter(Category::isActive)
                .toList();
        //customers that are not KYC verified can only pick KYC_REVIEW
        KycStatus kyc = identityClient.findUser(customer.getId()).map(UserSummary::kycStatus).orElse(null);
        if (kyc != KycStatus.APPROVED) {
            categories = categories.stream()
                    .filter(c -> c.getName().equals("KYC_REVIEW"))
                    .toList();
        }
        if (categories.isEmpty()) {
            throw new BusinessRuleException("No ticket categories are available right now");
        }
        return aiClient.draft(new DraftCall(request.text(), request.title(), options(categories)));
    }

    public TriageResponse triage(CurrentUser agent, Long ticketId) {
        rateLimiter.check(agent.getId());
        TicketCreationResponse ticket = ticketService.getTicketById(agent, ticketId);
        List<Category> categories = categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .toList();
        return aiClient.triage(new TriageCall(ticket.title(), cut(ticket.description(), MAX_DESCRIPTION_CHARS),
                ticket.categoryName(), String.valueOf(ticket.priority()), comments(agent, ticketId), options(categories)));
    }

    public SummaryResponse summarize(CurrentUser agent, Long ticketId) {
        rateLimiter.check(agent.getId());
        TicketCreationResponse ticket = ticketService.getTicketById(agent, ticketId);
        return aiClient.summarize(new SummaryCall(ticket.title(), cut(ticket.description(), MAX_DESCRIPTION_CHARS),
                String.valueOf(ticket.status()), ticket.categoryName(), String.valueOf(ticket.priority()),
                comments(agent, ticketId)));
    }

    //last 30 comments, each cut to 1000 characters
    private List<CommentItem> comments(CurrentUser user, Long ticketId) {
        List<CommentResponse> all = ticketService.listComments(user, ticketId);
        List<CommentResponse> last = all.subList(Math.max(0, all.size() - MAX_COMMENTS), all.size());
        return last.stream()
                .map(c -> new CommentItem(c.authorEmail(), cut(c.commentContent(), MAX_COMMENT_CHARS),
                        String.valueOf(c.createdAt())))
                .toList();
    }

    private List<CategoryOption> options(List<Category> categories) {
        return categories.stream().map(c -> new CategoryOption(c.getId(), c.getName())).toList();
    }

    private String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}