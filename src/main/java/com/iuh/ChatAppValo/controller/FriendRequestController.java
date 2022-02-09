package com.iuh.ChatAppValo.controller;

import com.iuh.ChatAppValo.entity.*;
import com.iuh.ChatAppValo.entity.FriendRequest;
import com.iuh.ChatAppValo.entity.enumEntity.ConversationType;
import com.iuh.ChatAppValo.entity.enumEntity.MessageType;
import com.iuh.ChatAppValo.jwt.response.MessageResponse;
import com.iuh.ChatAppValo.repositories.*;
import com.iuh.ChatAppValo.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Not finish yet
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/friend-request")
public class FriendRequestController {
    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ChatService chatService;

    /**
     * Gửi lời mời kết bạn đến toId
     * @param account
     * @param toId
     * @return
     */
    @PostMapping("/to/{toId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendFriendRequest(@AuthenticationPrincipal Account account, @PathVariable("toId") String toId){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        if (toId.equals(user.getId())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Same user"));
        }
        if (!(userRepository.existsById(toId))){
            return ResponseEntity.badRequest().body(new MessageResponse("user is not exist check"));
        }
        if (friendRepository.isFriend(user.getId(), toId)){
            return ResponseEntity.badRequest().body(new MessageResponse("users are friend check"));
        }
        if (!(friendRequestRepository.isSent(user.getId(), toId))){
            FriendRequest friendRequest = FriendRequest.builder()
                    .fromId(user.getId())
                    .toId(toId)
                    .build();
            friendRequestRepository.save(friendRequest);
            return ResponseEntity.ok().body(new MessageResponse("request send"));
        }
        return  ResponseEntity.badRequest().body(new MessageResponse("request failed"));
    }

    /**
     * lấy danh sách yêu cầu kết bạn đến người dùng
     * @param account
     * @return
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllFriendRequest(@AuthenticationPrincipal Account account, Pageable pageable){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Page<FriendRequest> requestList = friendRequestRepository.getFriendRequestReceived(user.getId(), pageable);
        if(requestList == null)
            return  ResponseEntity.ok().body(new MessageResponse("Không tìm thấy yêu cầu kết bạn nào"));
        return ResponseEntity.ok(requestList);
    }

    /**
     * Chấp nhận lời mời kết bạn từ người dùng có id;
     * @param account
     * @param id người dùng được chấp nhận yêu cầu kết bạn
     * @return
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acceptRequest(@AuthenticationPrincipal Account account, @PathVariable String id){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        User friend = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        if (friendRepository.isFriend(user.getId(), id)){
            // cả 2 đã là bạn bè
            friendRequestRepository.deleteFriendRequest(user.getId(), id);
            return ResponseEntity.badRequest().body(new MessageResponse(user.getName() + " đã là bạn bè"));
        }
        if (!(friendRequestRepository.isReceived(id, user.getId()))){
            // lời mời không tồn tại
            return ResponseEntity.badRequest().body(new MessageResponse("Yêu cầu kết bạn không tồn tại"));
        } else {
            // tạo quan hệ bạn bè cho người dùng
            friendRepository.save(Friend.builder().userId(user.getId()).friendId(id).build());
            // tạo quan hệ bạn bè cho bạn bè
            friendRepository.save(Friend.builder().userId(id).friendId(user.getId()).build());
            // xóa lời mời từ cả 2 bên nếu tồn tại
            friendRequestRepository.deleteFriendRequest(user.getId(), id);

            // add friend conversation
            Conversation conversationToCheck = conversationRepository.findOneOneConversationBetween(user.getId(), id);
            if (conversationToCheck != null){
                // gửi tin nhắn hệ thống thông báo kết bạn thành công - not yet
                String messageContent = user.getName() + " và " + friend.getName() + " đã trở thành bạn";
                sendSystemMessage(conversationToCheck, messageContent);
                return ResponseEntity.ok(new MessageResponse(messageContent));
            } else {
                Set<Participant> participants = new HashSet<>();
                participants.add(Participant.builder()
                        .userId(user.getId())
                        .build());
                participants.add(Participant.builder()
                        .userId(id)
                        .build());
                Conversation conversation =Conversation.builder()
                        .participants(participants)
                        .conversationType(ConversationType.ONE_ONE)
                        .build();
                conversationRepository.save(conversation);
                // gửi tin nhắn hệ thống thông báo kết bạn thành công
                String messageContent = user.getName() + " và " + friend.getName() + " đã trở thành bạn";
                sendSystemMessage(conversation, messageContent);
                return ResponseEntity.ok(new MessageResponse(messageContent));
            }
        }
    }

    /**
     * Xóa lời mời kết bạn
     * @param account
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteRequest(@AuthenticationPrincipal Account account, @PathVariable String id){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        User friend = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        if (!(friendRequestRepository.isReceived(id, user.getId()))) {
            // lời mời không tồn tại
            return ResponseEntity.badRequest().body(new MessageResponse("Yêu cầu kết bạn không tồn tại"));
        } else {
            friendRequestRepository.deleteFriendRequest(user.getId(), friend.getId());
            return ResponseEntity.ok(new MessageResponse("Từ chối lời mời kết bạn thành công"));
        }
    }

    public void sendSystemMessage(Conversation conversation, String messageBody){
        Message message = Message.builder()
                .senderId(null) // system message => senderId null
                .conversationId(conversation.getId())
                .messageType(MessageType.SYSTEM)
                .content(messageBody)
                .pin(false)
                .build();
        chatService.sendSystemMessage(message, conversation);
    }
}
