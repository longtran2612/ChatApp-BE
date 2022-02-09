package com.iuh.ChatAppValo.services;

import com.iuh.ChatAppValo.entity.Message;
import com.iuh.ChatAppValo.entity.Reaction;

public interface MessageService {
    void saveMessage(Message message);
    void reactMessage(Message message, Reaction reaction);
}
