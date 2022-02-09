package com.iuh.ChatAppValo.dto.request;

import lombok.Data;

@Data
public class PinMessageDTO {
    private String messageId;
    private String conversationId;
    private String userId;
    private boolean pin;
}
