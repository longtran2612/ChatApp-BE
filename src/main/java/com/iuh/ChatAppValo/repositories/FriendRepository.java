package com.iuh.ChatAppValo.repositories;

import com.iuh.ChatAppValo.entity.Friend;
import com.iuh.ChatAppValo.services.FriendService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendRepository extends MongoRepository<Friend, String>, FriendService {

    /**
     *  Kiểm tra người dùng userId với friendId có là bạn hay không
     *  exist = yes
      */
    @Query(value = "{$or: [{userId: ?0, friendId: ?1}, {friendId: ?0, userId: ?1}]}", exists = true)
    boolean isFriend(String userId, String friendId);

    /**
     * @return danh sách bạn bè của userId
     */
    @Query(value = "{userId: ?0}", sort = "{addDateAt: -1}")
    Page<Friend> getFriendListOfUserWithUserId(String userId, Pageable pageable);
    
    /**
     * @return Friend
     */
    @Query(value = "{userId: ?0, friendId: ?1}")
    Friend findFriend(String userId, String friendId);

}
