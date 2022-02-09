package com.iuh.ChatAppValo.repositories;

import com.iuh.ChatAppValo.entity.Message;
import com.iuh.ChatAppValo.services.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String>, MessageService {
    /**
     * @return danh sách tin nhắn của conversationId
     */
    @Query(value = "{conversationId: ?0}", sort = "{sendAt: -1}")
    Page<Message> getMessageOfConversation(String conversationId, Pageable pageable);

    /**
     * @return danh sách tin nhắn của conversationId with chosen type
     */
    @Query(value = "{'conversationId': ?0, 'messageType': ?1}", sort = "{sendAt: -1}")
    Page<Message> getMessageOfConversationWithType(String conversationId, String type, Pageable pageable);

    /**
     * @return danh sách tin nhắn được pin của conversationId
     */
    @Query(value = "{'conversationId': ?0, 'pin': ?1}", sort = "{sendAt: -1}")
    List<Message> getPinnedMessage(String conversationId, boolean pin);

    /**
     * lấy tin nhắn mới nhất trong đoạn hội thoại
     * @param conversationId
     * @return
     */
    @Aggregation(pipeline = {
            "{$match: {conversationId: ?0}}",
            "{$sort: {sendAt: -1}}",
            "{$limit: 1}" })
    Optional<Message> getLastMessageOfConversation(String conversationId);
}
