package com.iuh.ChatAppValo.services.impl;

import com.iuh.ChatAppValo.services.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConversationServiceImpl implements ConversationService {
    @Autowired
    private MongoTemplate mongoTemplate;
}
