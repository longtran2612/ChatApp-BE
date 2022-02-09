package com.iuh.ChatAppValo.repositories;

import com.iuh.ChatAppValo.entity.FriendRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {
    /**
     * kiểm tra người dùng userId đã gửi request đến toId hay chưa
     * @param userId
     * @param toId
     * @return true nếu đã gửi
     */
    @Query(value = "{fromId: ?0, toId: ?1}", exists = true)
    boolean isSent(String userId, String toId);

    /**
     * kiểm tra người dùng fromId đã có nhận được request từ toId hay không
     * @param fromId
     * @param toId
     * @return true nếu đã gửi
     */
    @Query(value = "{fromId: ?0, toId: ?1}", exists = true)
    boolean isReceived(String fromId, String toId);

    /**
     * lấy danh sách yêu cầu kết bạn của người dùng userId
     * @param userId
     * @return
     */
    @Query(value = "{toId: ?0}", sort = "{sendAt: -1}")
    Page<FriendRequest> getFriendRequestReceived(String userId, Pageable pageable);

    /**
     * xóa lời mới kết bạn từ cả 2 người dùng
     * @param userId
     * @param friendId
     */
    @Query(value = "{fromId: ?1, toId: ?0}", delete = true)
    void deleteFriendRequest(String userId, String friendId);
}
