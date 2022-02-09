package com.iuh.ChatAppValo.dto.response;

import com.iuh.ChatAppValo.entity.Conversation;
import com.iuh.ChatAppValo.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseConversation {
    private Conversation conversation;
    private ResponseMessage lastMessage;
    private long unReadMessage;

}
