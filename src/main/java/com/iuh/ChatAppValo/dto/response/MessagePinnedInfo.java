package com.iuh.ChatAppValo.dto.response;

import com.iuh.ChatAppValo.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessagePinnedInfo {
    private String messageId;
    private String conversationId;
    private User user;
    private boolean pin;
}
