package com.iuh.ChatAppValo.services.impl;

import com.iuh.ChatAppValo.entity.Message;
import com.iuh.ChatAppValo.entity.Reaction;
import com.iuh.ChatAppValo.repositories.ConversationRepository;
import com.iuh.ChatAppValo.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void saveMessage(Message message) {
        mongoTemplate.save(message);
    }

    @Override
    public void reactMessage(Message message, Reaction reaction){
        List<Reaction> reactionList = message.getReactions();
        if (reactionList == null){
            reactionList.add(reaction);
            message.setReactions(reactionList);
            mongoTemplate.save(message);
            return;
        }
        // Kiểm tra xem tồn tại reaction của cùng 1 người dùng hay không?
        for (Reaction reactionToCompare : reactionList) {
            if (reactionToCompare.getUserId().equals(reaction.getUserId())){
                // remove reaction cũ
                reactionList.remove(reactionToCompare);
            }
        }
        reactionList.add(reaction);
        message.setReactions(reactionList);
        mongoTemplate.save(message);
    }

}
