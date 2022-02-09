package com.iuh.ChatAppValo.services.impl;

import com.iuh.ChatAppValo.services.ParticipantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class ParticipantServiceImpl implements ParticipantService {
    @Autowired
    private MongoTemplate mongoTemplate;
}
