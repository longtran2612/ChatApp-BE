package com.iuh.ChatAppValo.services;

import com.iuh.ChatAppValo.entity.Conversation;
import com.iuh.ChatAppValo.entity.Message;

public interface ChatService {
    void sendMessage(Message message, Conversation conversation);
    void sendSystemMessage(Message message, Conversation conversation);
    void sendCanceledMessage(Message message, String conversationId);
}
