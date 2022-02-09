package com.iuh.ChatAppValo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadDTO {
    private String messageId;
    private String conversationId;
    private String userId;
    private Date readAt;
}
