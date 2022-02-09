package com.iuh.ChatAppValo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iuh.ChatAppValo.entity.enumEntity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDTO implements Serializable {
    private String conversationId;
    private MessageType messageType;
    private String senderId;
    private String content;
    private String replyId;
}
