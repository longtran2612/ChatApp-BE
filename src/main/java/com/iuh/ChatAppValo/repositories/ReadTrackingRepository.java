package com.iuh.ChatAppValo.repositories;

import com.iuh.ChatAppValo.entity.ReadTracking;
import com.iuh.ChatAppValo.services.ReadTrackingService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadTrackingRepository extends MongoRepository<ReadTracking, String> {

    /**
     * tìm ReadTracking dựa trên conversationId và userId
      */
    ReadTracking findByConversationIdAndUserId(String conversationId, String userId);

    /**
     *  tìm tất cả read tracking của message với messageId
     */
    List<ReadTracking> findAllByMessageId(String messageId);
}
