package com.iuh.ChatAppValo.services.impl;

import com.iuh.ChatAppValo.entity.Conversation;
import com.iuh.ChatAppValo.entity.Participant;
import com.iuh.ChatAppValo.entity.ReadTracking;
import com.iuh.ChatAppValo.repositories.ReadTrackingRepository;
import com.iuh.ChatAppValo.services.ReadTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ReadTrackingServiceIml implements ReadTrackingService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ReadTrackingRepository readTrackingRepository;

    private static final Logger logger = Logger.getLogger(ReadTrackingServiceIml.class.getName());

    /**
     * update read tracking for userId in conversationId at messageId
     * @param conversationId
     * @param messageId
     * @param userId
     */
    @Override
    public void updateMessageReadTracking(String conversationId, String messageId, String userId){
        ReadTracking readTracking = readTrackingRepository.findByConversationIdAndUserId(conversationId, userId);
        if (readTracking == null){
            logger.log(Level.INFO, "creating new read tracking for user = {0} in conversation = {1}..."
                    , new Object[]{userId, conversationId});
            ReadTracking newReadTracking = ReadTracking.builder()
                    .unReadMessage(0)
                    .conversationId(conversationId)
                    .messageId(messageId)
                    .userId(userId)
                    .readAt(new Date())
                    .build();
            readTrackingRepository.save(newReadTracking);
        } else {
            // messageId not equals => new message
            if (!messageId.equals(readTracking.getMessageId())){
                logger.log(Level.INFO, "updating read tracking for user = {0} in conversation = {1}..."
                        , new Object[]{userId, conversationId});
                Criteria criteria = Criteria.where("conversationId").is(conversationId)
                        .and("userId").is(userId);
                Update update = new Update();
                update.set("messageId", messageId);
                update.set("readAt", new Date());
                update.set("unReadMessage", 0);
                mongoTemplate.updateFirst(Query.query(criteria), update, ReadTracking.class);
            }
        }
    }

    /**
     * increase number of unread message for other participants of conversation when having new message
     * except userId
     * @param conversation
     * @param userId
     */
    @Override
    public void increaseUnreadMessageForOtherParticipantsExceptUserId(Conversation conversation, String userId){
        logger.log(Level.INFO, "increasing unread message for other participants except user = {0} in conversation = {1}..."
                , new Object[]{userId, conversation.getId()});
        //Bulk operations for insert/update/remove actions on a collection.
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ReadTracking.class);
        int i = 0;
        for (Participant participant: conversation.getParticipants()) {
            if (!userId.equals(participant.getUserId())){
                if (readTrackingRepository.findByConversationIdAndUserId(conversation.getId(), participant.getUserId()) != null){
                    i = incrementUnReadMessage(conversation, bulkOperations, i, participant);
                } else {
                    createNewReadTracking(conversation, participant);
                }
            }
        }
        if (i != 0){
            bulkOperations.execute();
        }
    }

    /**
     * increase number of unread message for all participants of conversation when having new message
     * @param conversation
     */
    @Override
    public void increaseUnreadMessageForAllParticipants(Conversation conversation){
        logger.log(Level.INFO, "increasing unread message for all participants in conversation = {0}..."
                , conversation.getId());
        //Bulk operations for insert/update/remove actions on a collection.
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ReadTracking.class);
        int i = 0;
        for (Participant participant: conversation.getParticipants()) {
            if (readTrackingRepository.findByConversationIdAndUserId(conversation.getId(), participant.getUserId()) != null){
                i = incrementUnReadMessage(conversation, bulkOperations, i, participant);
            } else {
                createNewReadTracking(conversation, participant);
            }
        }
        if (i != 0){
            bulkOperations.execute();
        }
    }

    /**
     * create new read tracking for participant in conversation
     * @param conversation
     * @param participant
     */
    private void createNewReadTracking(Conversation conversation, Participant participant) {
        ReadTracking readTracking = ReadTracking.builder()
                .unReadMessage(1)
                .conversationId(conversation.getId())
                .userId(participant.getUserId())
                .build();
        readTrackingRepository.save(readTracking);
    }

    /**
     * increase participant's unread message in conversation
     * @param conversation
     * @param ops
     * @param i
     * @param participant
     * @return
     */
    private int incrementUnReadMessage(Conversation conversation, BulkOperations ops, int i, Participant participant) {
        var criteria = Criteria.where("conversationId").is(conversation.getId())
                .and("userId").is(participant.getUserId());
        var update = new Update();
        update.inc("unReadMessage", 1);
        ops.updateOne(Query.query(criteria), update);
        i++;
        if (i % 20 == 0)
            ops.execute();
        return i;
    }
}
