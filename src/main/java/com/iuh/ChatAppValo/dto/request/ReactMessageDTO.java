package com.iuh.ChatAppValo.dto.request;

import com.iuh.ChatAppValo.entity.enumEntity.ReactionType;
import lombok.Data;

@Data
public class ReactMessageDTO {
    private String messageId;
    private String conversationId;
    private String userId;
    private ReactionType reactionType;
}
