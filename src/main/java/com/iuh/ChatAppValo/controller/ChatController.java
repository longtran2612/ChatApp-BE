package com.iuh.ChatAppValo.controller;

import com.iuh.ChatAppValo.chat_authen.UserPrincipal;
import com.iuh.ChatAppValo.dto.MessageDTO;
import com.iuh.ChatAppValo.dto.ReadDTO;
import com.iuh.ChatAppValo.dto.request.PinMessageDTO;
import com.iuh.ChatAppValo.dto.request.ReactMessageDTO;
import com.iuh.ChatAppValo.dto.response.MessagePinnedInfo;
import com.iuh.ChatAppValo.dto.response.ReactionInfo;
import com.iuh.ChatAppValo.dto.response.ReadInfo;
import com.iuh.ChatAppValo.entity.*;
import com.iuh.ChatAppValo.entity.enumEntity.MessageStatus;
import com.iuh.ChatAppValo.jwt.JwtUtils;
import com.iuh.ChatAppValo.repositories.ConversationRepository;
import com.iuh.ChatAppValo.repositories.MessageRepository;
import com.iuh.ChatAppValo.repositories.UserRepository;
import com.iuh.ChatAppValo.services.ChatService;
import com.iuh.ChatAppValo.services.ReadTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

@Controller
@Slf4j
public class ChatController {
    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ReadTrackingService readTrackingService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     *  process message method
     *  "/app/chat" is the endpoint that this method is mapped to handle
     * @param messageDTO contains conversationId, messageType, content, replyId
     * @param userPrincipal contains username and access token
     */
    @MessageMapping("/chat")
    public void sendMessage(@Payload MessageDTO messageDTO, UserPrincipal userPrincipal){
        log.info("getting user info...");
        String username = userPrincipal.getName();
        String token = userPrincipal.getToken();
        if (username != null && token != null && jwtUtils.validateJwtToken(token)
                && username.equals(jwtUtils.getUserNameFromJwtToken(token))){
            Conversation conversation = conversationRepository.findById(messageDTO.getConversationId()).get();
            User user = userRepository.findDistinctByPhone(username).get();
            if (conversation != null && user != null){
                // kiểm tra người dùng có phải thành viên của nhóm hay không?
                if (isParticipant(conversation, user)){
                    Message message = Message.builder()
                            .conversationId(conversation.getId())
                            .senderId(messageDTO.getSenderId())
                            .sendAt(new Date())
                            .messageType(messageDTO.getMessageType())
                            .content(messageDTO.getContent())
                            .replyId(messageDTO.getReplyId())
                            .reactions(new ArrayList<Reaction>())
                            .pin(false)
                            .messageStatus(MessageStatus.SENT)
                            .build();
                    log.info("sending message {} sent to websocket server with endpoint users/queue/messages",message);
                    chatService.sendMessage(message, conversation);

                } else {
                    log.info("conversation does not contain participant with username: {}", user.getName());
                }
            } else {
                log.info("conversation or user is null");
            }
        } else {
            log.info("invalid username or access token");
        }
    }

    /**
     * process when a user read message
     * "/app/read" is the endpoint that this method is mapped to handle
     * @param readDTO contains messageId, conversationId, userId, readAt
     * @param userPrincipal contains username and access token
     */
    @MessageMapping("/read")
    public void readMessage(@Payload ReadDTO readDTO, UserPrincipal userPrincipal){
        log.info("getting user info...");
        String username = userPrincipal.getName();
        String token = userPrincipal.getToken();
        if (username != null && token != null && jwtUtils.validateJwtToken(token)
                && username.equals(jwtUtils.getUserNameFromJwtToken(token))){
            Conversation conversation = conversationRepository.findById(readDTO.getConversationId()).get();
            User user = userRepository.findDistinctByPhone(username).get();
            if (conversation != null && user != null){
                // kiểm tra người dùng có phải thành viên của nhóm hay không?
                if (isParticipant(conversation, user)){
                    ReadInfo readInfo = ReadInfo.builder()
                            .readByUser(user)
                            .messageId(readDTO.getMessageId())
                            .readAt(dateFormat.format(readDTO.getReadAt()))
                            .conversationId(conversation.getId())
                            .build();
                    log.info("updating message read tracking...");
                    readTrackingService.updateMessageReadTracking(readInfo.getConversationId(), readInfo.getMessageId(), readInfo.getReadByUser().getId());
                    log.info("sending read tracking of {} to all participants...", user.getName());
                    for (Participant p: conversation.getParticipants()) {
                        User userToSend = userRepository.findById(p.getUserId())
                                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
                        if (!user.getId().equals(p.getUserId()))
                            simpMessagingTemplate.convertAndSendToUser(userToSend.getPhone(), "/queue/read", readInfo);
                    }
                    log.info("read tracking of {} have been send to all participants in users/queue/read", user.getName());
                } else {
                    log.info("conversation does not contain participant with username: {}", username);
                }
            } else {
                log.info("conversation or user is null");
            }
        } else {
            log.info("invalid username or access token");
        }
    }

    /**
     * process when a user send reaction to a message
     * "/app/react" is the endpoint that this method is mapped to handle
     * @param reactionMessageDTO
     * @param userPrincipal
     */
    @MessageMapping("/react")
    public void reactMessage(@Payload ReactMessageDTO reactionMessageDTO, UserPrincipal userPrincipal){
        log.info("getting user info...");
        String username = userPrincipal.getName();
        String token = userPrincipal.getToken();
        if (username != null && token != null && jwtUtils.validateJwtToken(token)
                && username.equals(jwtUtils.getUserNameFromJwtToken(token))) {
            Conversation conversation = conversationRepository.findById(reactionMessageDTO.getConversationId()).get();
            User user = userRepository.findDistinctByPhone(username).get();
            if (conversation != null && user != null) {
                // kiểm tra người dùng có phải thành viên của nhóm hay không?
                if (isParticipant(conversation, user)){
                    // lưu reaction
                    Message message = messageRepository.findById(reactionMessageDTO.getMessageId()).get();
                    Reaction reaction = Reaction.builder()
                            .reactionType(reactionMessageDTO.getReactionType())
                            .userId(reactionMessageDTO.getUserId())
                            .build();
                    messageRepository.reactMessage(message, reaction);
                    // tạo reactionInfo để gửi lên socket server
                    ReactionInfo reactionInfo = ReactionInfo.builder()
                            .reactionType(reactionMessageDTO.getReactionType())
                            .messageId(reactionMessageDTO.getMessageId())
                            .conversationId(reactionMessageDTO.getConversationId())
                            .user(user)
                            .build();
                    for (Participant p: conversation.getParticipants()) {
                        User userToSend = userRepository.findById(p.getUserId())
                                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
                        simpMessagingTemplate.convertAndSendToUser(userToSend.getPhone(), "/queue/reactions", reactionInfo);
                    }
                    log.info("reaction of {} have been send to all participants in users/queue/reactions", user.getName());
                }else {
                    log.info("conversation does not contain participant with username: {}", username);
                }
            } else {
                log.info("conversation or user is null");
            }
        } else {
            log.info("invalid username or access token");
        }
    }

    /**
     * process when a user pin a message
     * "/app/pin" is the endpoint that this method is mapped to handle
     * @param pinMessageDTO
     * @param userPrincipal
     */
    @MessageMapping("/pin")
    public void pinMessage(@Payload PinMessageDTO pinMessageDTO, UserPrincipal userPrincipal){
        log.info("getting user info...");
        String username = userPrincipal.getName();
        String token = userPrincipal.getToken();
        if (username != null && token != null && jwtUtils.validateJwtToken(token)
                && username.equals(jwtUtils.getUserNameFromJwtToken(token))) {
            Conversation conversation = conversationRepository.findById(pinMessageDTO.getConversationId()).get();
            User user = userRepository.findDistinctByPhone(username).get();
            if (conversation != null && user != null) {
                // kiểm tra người dùng có phải thành viên của nhóm hay không?
                if (isParticipant(conversation, user)){
                    // lưu pin message
                    Message message = messageRepository.findById(pinMessageDTO.getMessageId()).get();
                    message.setPin(pinMessageDTO.isPin());
                    messageRepository.save(message);
                    // tạo MessagePinnedInfo để gửi lên socket server
                    MessagePinnedInfo messagePinnedInfo = MessagePinnedInfo.builder()
                            .pin(pinMessageDTO.isPin())
                            .conversationId(pinMessageDTO.getConversationId())
                            .messageId(pinMessageDTO.getMessageId())
                            .user(user)
                            .build();
                    for (Participant p: conversation.getParticipants()) {
                        User userToSend = userRepository.findById(p.getUserId())
                                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
                        simpMessagingTemplate.convertAndSendToUser(userToSend.getPhone(), "/queue/pinned", messagePinnedInfo);
                    }
                    log.info("Pinned message of {} have been send to all participants in users/queue/pinned", user.getName());
                }else {
                    log.info("conversation does not contain participant with username: {}", username);
                }
            } else {
                log.info("conversation or user is null");
            }
        } else {
            log.info("invalid username or access token");
        }
    }

    private boolean isParticipant(Conversation conversation, User userToCheck){
        Set<Participant> participantSet = conversation.getParticipants();
        // kiểm tra người dùng có phải thành viên của nhóm hay không?
        for (Participant participant: participantSet) {
            if (userToCheck.getId().equals(participant.getUserId())) {
                return true;
            }
        }
        return false;
    }
}
