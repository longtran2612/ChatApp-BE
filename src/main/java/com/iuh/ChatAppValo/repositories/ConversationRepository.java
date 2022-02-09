package com.iuh.ChatAppValo.repositories;

import com.iuh.ChatAppValo.entity.Conversation;
import com.iuh.ChatAppValo.services.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String>, ConversationService {

    /**
     * tìm conversation có dạng ONE_ONE giữa hai user
     */
    @Aggregation(pipeline = {
            "{$match: {$and: [{'participants.userId': ?0}, {'participants.userId': ?1}, {'participants': {$size: 2}}, {type: 'ONE_ONE'}]}}",
            "{$sort: {createAt: -1}}",
            "{$limit: 1}" })
    Conversation findOneOneConversationBetween(String userId1, String userId2);

    /**
     * @return danh sách conversation của userId
     */
    @Query(value = "{'participants.userId': ?0}", sort = "{createAt: -1}")
    Page<Conversation> getConversationsOfUser(String userId, Pageable pageable);

}
