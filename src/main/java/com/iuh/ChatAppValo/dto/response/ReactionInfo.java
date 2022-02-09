package com.iuh.ChatAppValo.dto.response;

import com.iuh.ChatAppValo.entity.User;
import com.iuh.ChatAppValo.entity.enumEntity.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionInfo {
    private String messageId;
    private String conversationId;
    private User user;
    private ReactionType reactionType;
}
