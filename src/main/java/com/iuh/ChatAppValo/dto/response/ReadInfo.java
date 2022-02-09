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
public class ReadInfo {
    private String messageId;
    private String conversationId;
    private User readByUser;
    private String readAt;
}
