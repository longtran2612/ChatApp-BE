package com.iuh.ChatAppValo.services.impl;

import com.iuh.ChatAppValo.dto.response.ResponseMessage;
import com.iuh.ChatAppValo.entity.Conversation;
import com.iuh.ChatAppValo.entity.Message;
import com.iuh.ChatAppValo.entity.Participant;
import com.iuh.ChatAppValo.entity.User;
import com.iuh.ChatAppValo.entity.enumEntity.MessageType;
import com.iuh.ChatAppValo.repositories.ConversationRepository;
import com.iuh.ChatAppValo.repositories.MessageRepository;
import com.iuh.ChatAppValo.repositories.ReadTrackingRepository;
import com.iuh.ChatAppValo.repositories.UserRepository;
import com.iuh.ChatAppValo.services.ChatService;
import com.iuh.ChatAppValo.services.ReadTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatServiceImpl implements ChatService {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private ReadTrackingService readTrackingService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = Logger.getLogger(ChatService.class.getName());

    /**
     *  gửi tin nhắn
     * @param message
     * @param conversation
     */
    @Override
    public void sendMessage(Message message, Conversation conversation) {
        messageRepository.save(message);
        if (sendMessageToAllParticipant(message, conversation)){
            readTrackingService.increaseUnreadMessageForOtherParticipantsExceptUserId(conversation, message.getSenderId());
            readTrackingService.updateMessageReadTracking(conversation.getId(), message.getId(), message.getSenderId());
        } else {
            logger.log(Level.INFO, "sending message failed");
        }
    }



    @Override
    public void sendSystemMessage(Message message, Conversation conversation) {
        messageRepository.save(message);
        logger.log(Level.INFO, "sending system message");
        if (sendMessageToAllParticipant(message, conversation)){
            logger.log(Level.INFO, "system message sent");
            readTrackingService.increaseUnreadMessageForAllParticipants(conversation);
        } else {
            logger.log(Level.INFO, "sending message failed");
        }
    }

    /**
     *  gửi tin nhắn đến thành viên trong nhóm
     * @param message
     * @param conversation
     * @return
     */
    public boolean sendMessageToAllParticipant(Message message, Conversation conversation){
        Message sendingMessage = messageRepository.findById(message.getId()).get();
        if (sendingMessage == null){
            logger.log(Level.INFO, "Message Not Found");
            return false;
        }
        Set<Participant> participants =conversation.getParticipants();
        // handle system message
        if (sendingMessage.getMessageType() == MessageType.SYSTEM){
            ResponseMessage responseMessage = ResponseMessage.builder()
                    .message(sendingMessage)
                    .userImgUrl(null)
                    .userName(null)
                    .build();
            if (participants != null && !(participants.isEmpty())){
                for (Participant participant: participants){
                    User user = userRepository.findById(participant.getUserId())
                            .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
                    simpMessagingTemplate.convertAndSendToUser(user.getPhone(), "/queue/messages", responseMessage);
                    logger.log(Level.INFO, "message sent to " + user.getPhone());
                }
                return true;
            }
        }
        // handle normal message
        User messageSender = userRepository.findById(sendingMessage.getSenderId()).get();
        ResponseMessage responseMessage = ResponseMessage.builder()
                .message(sendingMessage)
                .userImgUrl(messageSender.getImgUrl())
                .userName(messageSender.getName())
                .build();
        if (participants != null && !(participants.isEmpty())){
            for (Participant participant: participants){
                User user = userRepository.findById(participant.getUserId())
                        .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
                simpMessagingTemplate.convertAndSendToUser(user.getPhone(), "/queue/messages", responseMessage);
                logger.log(Level.INFO, "message sent to " + user.getPhone());
            }
            return true;
        }
        return false;
    }

    /**
     * gửi tin nhắn sau khi thu hồi 1 tin nhắn đến thành viên trong nhóm
     * @param message
     * @param conversationId
     */
    public void sendCanceledMessage(Message message, String conversationId){
        Message sendingMessage = messageRepository.findById(message.getId())
                .orElseThrow(() -> new NullPointerException("Message null"));
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NullPointerException("Conversation null"));
        Set<Participant> participantSet = conversation.getParticipants();
        ResponseMessage responseMessage = ResponseMessage.builder()
                .message(sendingMessage)
                .userImgUrl(null)
                .userName(null)
                .build();
        if (!participantSet.isEmpty()){
            for (Participant participant:participantSet) {
                User user = userRepository.findById(participant.getUserId())
                        .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
                simpMessagingTemplate.convertAndSendToUser(user.getPhone(), "/queue/messages/delete", responseMessage);
                logger.log(Level.INFO, "message sent to " + user.getPhone());
            }
        }
    }
}
