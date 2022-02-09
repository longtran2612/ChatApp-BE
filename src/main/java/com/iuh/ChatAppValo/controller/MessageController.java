package com.iuh.ChatAppValo.controller;

import com.iuh.ChatAppValo.dto.response.MessageInfo;
import com.iuh.ChatAppValo.dto.response.ResponseMessage;
import com.iuh.ChatAppValo.entity.*;
import com.iuh.ChatAppValo.entity.enumEntity.MessageStatus;
import com.iuh.ChatAppValo.entity.enumEntity.MessageType;
import com.iuh.ChatAppValo.entity.enumEntity.ReactionType;
import com.iuh.ChatAppValo.jwt.response.MessageResponse;
import com.iuh.ChatAppValo.repositories.ConversationRepository;
import com.iuh.ChatAppValo.repositories.MessageRepository;
import com.iuh.ChatAppValo.repositories.ReadTrackingRepository;
import com.iuh.ChatAppValo.repositories.UserRepository;
import com.iuh.ChatAppValo.services.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/messages")
public class MessageController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ReadTrackingRepository readTrackingRepository;

    @Autowired
    private ChatService chatService;

    @GetMapping("/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMessagesOfConversationWithId(@PathVariable("conversationId") String conversationId
            , Pageable pageable, @AuthenticationPrincipal Account account){
        Conversation conversation = conversationRepository.findById(conversationId).get();
        if (conversation == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Conversation null"));
        Page<Message> messagePage = messageRepository.getMessageOfConversation(conversationId, pageable);
        Page<ResponseMessage> responseMessagePage = getMessages(messagePage, pageable);
        return ResponseEntity.ok(responseMessagePage);
    }

    @GetMapping("/type/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMessagesOfConversationWithIdAndType(@PathVariable("conversationId") String conversationId
            , Pageable pageable, @AuthenticationPrincipal Account account, @RequestParam("type") String messageType){
        Conversation conversation = conversationRepository.findById(conversationId).get();
        if (conversation == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Conversation null"));
        for (MessageType type:MessageType.values()) {
            if (messageType.equals(type.name())){
                Page<Message> messagePage = messageRepository.getMessageOfConversationWithType(conversationId, messageType, pageable);
                Page<ResponseMessage> responseMessagePage = getMessages(messagePage, pageable);
                return ResponseEntity.ok(responseMessagePage);
            }
        }
        return ResponseEntity.ok(new ArrayList<>());
    }

    @GetMapping("/info/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMessageInfo(@PathVariable("messageId") String messageId, @AuthenticationPrincipal Account account){
        Message message = messageRepository.findById(messageId).get();
        if (message == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Message null"));
        List<ReadTracking> readTrackingList = readTrackingRepository.findAllByMessageId(messageId);
        MessageInfo messageInfo = MessageInfo.builder()
                .message(message)
                .build();
        if (readTrackingList.isEmpty())
            return ResponseEntity.ok(messageInfo);
        messageInfo.setReadTrackingList(readTrackingList);
        return ResponseEntity.ok(messageInfo);
    }

    @DeleteMapping("/cancel/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelMessageSent(@PathVariable("messageId") String messageId, @AuthenticationPrincipal Account account){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Message message = messageRepository.findById(messageId).get();
        if (message == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Message null"));
        if (user.getId().equals(message.getSenderId())){
            String cancelContent = "Tin nhắn đã được thu hồi";
            message.setMessageStatus(MessageStatus.CANCELED);
            message.setContent(cancelContent);
            messageRepository.saveMessage(message);
            log.info("sending message {} sent to websocket server with endpoint users/queue/messages/delete/",message);
            chatService.sendCanceledMessage(message, message.getConversationId());
            return ResponseEntity.ok(new MessageResponse(cancelContent));
        }
        return ResponseEntity.badRequest().body(new MessageResponse("Message cancelling failed"));
    }

    @PostMapping("/react/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reactMessage(@PathVariable("messageId") String messageId, @RequestParam("reaction") ReactionType reactionType,
                                          @AuthenticationPrincipal Account account){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Message message = messageRepository.findById(messageId).get();
        if (message == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Message null"));
        Reaction reaction = Reaction.builder()
                .userId(user.getId())
                .reactionType(reactionType)
                .build();
        messageRepository.reactMessage(message, reaction);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pin/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> pinMessage(@PathVariable("messageId") String messageId, @AuthenticationPrincipal Account account){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Message message = messageRepository.findById(messageId).get();
        if (message == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Message null"));
        if (message.getMessageType().equals(MessageType.SYSTEM))
            return ResponseEntity.badRequest().body(new MessageResponse("Không thể pin tin nhắn hệ thống"));
        if (message.isPin()){
            // nếu message đã dc pin thì sẽ unpin message
            message.setPin(false);
        } else {
            // nếu message chưa pin thì sẽ pin message
            message.setPin(true);
        }
        messageRepository.save(message);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pin/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPinnedMessage(@PathVariable("conversationId") String conversationId, @AuthenticationPrincipal Account account){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Conversation conversation = conversationRepository.findById(conversationId).get();
        if (conversation == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Conversation null"));
        List<Message> pinnedMessage = messageRepository.getPinnedMessage(conversationId, true);
        if (pinnedMessage == null)
            return ResponseEntity.ok(new ArrayList<Message>());
        return ResponseEntity.ok(pinnedMessage);
    }

    @GetMapping("/react/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getReactionOfMessage(@PathVariable("messageId") String messageId, @AuthenticationPrincipal Account account){
        Message message = messageRepository.findById(messageId).get();
        if (message == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Message null"));
        if (message.getReactions() == null){
            return ResponseEntity.ok(new ArrayList<Reaction>());
        }
        return ResponseEntity.ok(message.getReactions());
    }

    private Page<ResponseMessage> getMessages(Page<Message> messagePage, Pageable pageable){
        List<ResponseMessage> responseMessageList = new ArrayList<>();
        // message on db
        List<Message> messageList = messagePage.getContent();
        for (Message message:messageList) {
            if (message.getMessageType() != MessageType.SYSTEM){
                User messageSender = userRepository.findById(message.getSenderId()).get();
                ResponseMessage responseMessage = ResponseMessage.builder()
                        .message(message)
                        .userImgUrl(messageSender.getImgUrl())
                        .userName(messageSender.getName())
                        .build();
                responseMessageList.add(responseMessage);
            } else {
                // với system message thì không có sender
                ResponseMessage responseMessage = ResponseMessage.builder()
                        .message(message)
                        .userName(null)
                        .userImgUrl(null)
                        .build();
                responseMessageList.add(responseMessage);
            }
        }
        Page<ResponseMessage> responseMessagePage = new PageImpl<ResponseMessage>(responseMessageList, pageable, responseMessageList.size());
        return responseMessagePage;
    }
}
