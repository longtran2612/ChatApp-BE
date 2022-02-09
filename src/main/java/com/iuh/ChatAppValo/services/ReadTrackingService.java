package com.iuh.ChatAppValo.services;

import com.iuh.ChatAppValo.entity.Conversation;

public interface ReadTrackingService {

    /**
     * update read tracking for userId in conversationId at messageId
     * @param conversationId
     * @param messageId
     * @param userId
     */
    void updateMessageReadTracking(String conversationId, String messageId, String userId);

    /**
     * increase number of unread message for other participants of conversation when having new message
     * except userId
     * @param conversation
     * @param userId
     */
    void increaseUnreadMessageForOtherParticipantsExceptUserId(Conversation conversation, String userId);

    /**
     * increase number of unread message for all participants of conversation when having new message
     * @param conversation
     */
    void increaseUnreadMessageForAllParticipants(Conversation conversation);

}
