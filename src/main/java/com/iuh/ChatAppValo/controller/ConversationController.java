package com.iuh.ChatAppValo.controller;

import com.iuh.ChatAppValo.dto.ParticipantDTO;
import com.iuh.ChatAppValo.dto.request.ConversationCreateDTO;
import com.iuh.ChatAppValo.dto.response.ResponseConversation;
import com.iuh.ChatAppValo.dto.response.ResponseMessage;
import com.iuh.ChatAppValo.entity.*;
import com.iuh.ChatAppValo.entity.enumEntity.ConversationType;
import com.iuh.ChatAppValo.entity.enumEntity.MessageType;
import com.iuh.ChatAppValo.jwt.response.MessageResponse;
import com.iuh.ChatAppValo.repositories.*;
import com.iuh.ChatAppValo.services.AmazonS3Service;
import com.iuh.ChatAppValo.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/conversations")
public class ConversationController {
    @Autowired
    private AmazonS3Service s3Service;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatService chatService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ReadTrackingRepository readTrackingRepository;

    @Autowired
    private FriendRepository friendRepository;

    private static final Logger logger = Logger.getLogger(ConversationController.class.getName());

//    /**
//     * lấy danh sách hội thoại
//     * @param account
//     * @param pageable
//     * @return
//     */
//    @GetMapping
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<?> getConversationsOfUser(@AuthenticationPrincipal Account account, Pageable pageable){
//        User user = userRepository.findDistinctByPhone(account.getUsername())
//                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
//        Page<Conversation> conversationPage = conversationRepository.getConversationsOfUser(user.getId(), pageable);
//        if (conversationPage == null)
//            return ResponseEntity.badRequest().body(new MessageResponse("conversation null"));
//        return ResponseEntity.ok(conversationPage);
//    }

    /**
     * lấy danh sách hội thoại với tin nhắn mới nhất và số tin nhắn mới
     * @param account
     * @param pageable
     * @return responseConversationPage - Page ResponseConversation giảm dần theo ngày có tin nhắn mới
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getConversationsOfUser(@AuthenticationPrincipal Account account, Pageable pageable){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Page<Conversation> conversationPage = conversationRepository.getConversationsOfUser(user.getId(), pageable);
        if (conversationPage == null)
            return ResponseEntity.badRequest().body(new MessageResponse("conversation null"));
        List<Conversation> conversationList = conversationPage.getContent();
        //Tạo responseConversationList
        List<ResponseConversation> responseConversationList = new ArrayList<ResponseConversation>();
        for (Conversation conversation : conversationList) {
            if (isParticipant(conversation, user)){
                Message lastMessage = messageRepository.getLastMessageOfConversation(conversation.getId()).get();
                ReadTracking readTracking = readTrackingRepository.findByConversationIdAndUserId(conversation.getId(), user.getId());
                if(lastMessage.getMessageType() !=MessageType.SYSTEM){
                    User messageSender = userRepository.findById(lastMessage.getSenderId()).get();
                    ResponseMessage responseMessage = ResponseMessage.builder()
                            .message(lastMessage)
                            .userImgUrl(messageSender.getImgUrl())
                            .userName(messageSender.getName())
                            .build();
                    ResponseConversation responseConversation = ResponseConversation.builder()
                            .conversation(conversation)
                            .lastMessage(responseMessage)
                            .unReadMessage(readTracking.getUnReadMessage())
                            .build();
                    responseConversationList.add(responseConversation);
                }else {
                    ResponseMessage responseMessage = ResponseMessage.builder()
                            .message(lastMessage)
                            .userImgUrl(null)
                            .userName(null)
                            .build();
                    ResponseConversation responseConversation = ResponseConversation.builder()
                            .conversation(conversation)
                            .lastMessage(responseMessage)
                            .unReadMessage(readTracking.getUnReadMessage())
                            .build();
                    responseConversationList.add(responseConversation);
                }

            }
        }
        //Sắp xếp lại responseConversationList để trả về danh sách conversation theo ngày có tin nhắn mới giảm dần
        Collections.sort(responseConversationList, new Comparator<ResponseConversation>() {
            @Override
            public int compare(ResponseConversation o1, ResponseConversation o2) {
                return o2.getLastMessage().getMessage().getSendAt().compareTo(o1.getLastMessage().getMessage().getSendAt());
            }
        });
        //Tạo Page
        Page<ResponseConversation> responseConversationPage = new PageImpl<>(responseConversationList, conversationPage.getPageable(), conversationPage.getTotalElements());
        return ResponseEntity.ok(responseConversationPage);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createGroup(@AuthenticationPrincipal Account account, @RequestBody ConversationCreateDTO conversation){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));

        // Tạo danh sách participants
        Set<Participant> participants = new HashSet<>();
        for (Participant participant : conversation.getParticipants()) {
            User userToAdd = userRepository.findById(participant.getUserId()).get();
            //kiểm tra người dùng được thêm có tồn tại hay không
            if (userToAdd == null)
                logger.log(Level.INFO, "userId = {} không tồn tại", userToAdd.getName());
            Participant newParticipant = Participant.builder()
                    .isAdmin(false)
                    .userId(participant.getUserId())
                    .addByUserId(user.getId())
                    .addTime(new Date())
                    .build();
            participants.add(newParticipant);
        }

        // kiểm tra danh sách rỗng hay không
        if (participants.isEmpty())
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Chưa có thành viên cho nhóm, vui lòng chọn thành viên được thêm vào"));

        // thêm người tạo nhóm vào danh sách thành viên
        Participant newParticipant = Participant.builder()
                .isAdmin(true)
                .userId(user.getId())
                .addByUserId(null)
                .addTime(new Date())
                .build();
        participants.add(newParticipant);

        // Tạo group
        Conversation newConversation = Conversation.builder()
                .name(conversation.getName())
                .conversationType(ConversationType.GROUP)
                .createdByUserId(user.getId())
                .participants(participants)
                .imageUrl("https://chatappvalo.s3.ap-southeast-1.amazonaws.com/5809830.png")
                .build();
        conversationRepository.save(newConversation);

        System.out.println(newConversation);

        // gửi tin nhắn hệ thống
        for (Participant participant: participants) {
            User userToAdd = userRepository.findById(participant.getUserId()).get();
            if (!userToAdd.getId().equals(user.getId())){
                String messageContent = user.getName() + " đã thêm " + userToAdd.getName() + " vào nhóm";
                sendSystemMessage(newConversation, messageContent);
            }

        }

        return ResponseEntity.ok(newConversation);
    }

    /**
     * Xuất danh sách bạn để thêm vào conversation
     * @param account
     * @param pageable
     * @return
     */
    @GetMapping("/add/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFriendListToAdd(@AuthenticationPrincipal Account account,
                                                @PathVariable("conversationId") String conversationId, Pageable pageable){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NullPointerException("Hội thoại không tồn tại"));
        Page<Friend> friendPage = friendRepository.getFriendListOfUserWithUserId(user.getId(), pageable);
        List<Friend> friendList = friendPage.getContent();
        System.out.println(friendList);
        if (friendList.isEmpty())
            return ResponseEntity.ok(new ArrayList<>());
        List<Friend> responseList = new ArrayList<>();
        for (Friend friend :friendList) {
            User userToCheck = userRepository.findById(friend.getFriendId())
                    .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
            if (!isParticipant(conversation, userToCheck)){
                responseList.add(friend);
            }
        }
        if (responseList.isEmpty())
            logger.log(Level.INFO, "No more friend to add in this conversation");
        return ResponseEntity.ok(new PageImpl<Friend>(responseList, pageable, responseList.size()));
    }

    /**
     * Thêm thành viên vào nhóm
     * @param account
     * @param participantDTO
     * @return
     */
    @PutMapping("/add")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addParticipant(@AuthenticationPrincipal Account account, @RequestBody ParticipantDTO participantDTO, Pageable pageable){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        User userToAdd = userRepository.findById(participantDTO.getUserId())
                .orElseThrow(() -> new NullPointerException("Người dùng được thêm không tồn tại"));
        Conversation conversation = conversationRepository.findById(participantDTO.getConversationId())
                .orElseThrow(() -> new NullPointerException("Hội thoại không tồn tại"));
        // kiểm tra có phải hội thoại cá nhân hay không?
        if (conversation.getConversationType() == ConversationType.ONE_ONE){
            return ResponseEntity.badRequest().body(new MessageResponse("Không thể thêm thành viên vào hội thoại cá nhân"));
        }
        // kiểm tra người được thêm có phải thành viên của nhóm hay không?
        if (isParticipant(conversation, userToAdd)){
            return ResponseEntity.badRequest().body(new MessageResponse(userToAdd.getName() + " đã là thành viên của nhóm"));
        }
        Set<Participant> participantSet = conversation.getParticipants();
        // kiểm tra người dùng có phải thành viên của nhóm hay không?
        if (isParticipant(conversation, user)){
            // tạo participant cho userToAdd
            Participant newParticipant = Participant.builder()
                    .addByUserId(user.getId())
                    .addTime(new Date())
                    .isAdmin(false)
                    .userId(userToAdd.getId())
                    .build();
            participantSet.add(newParticipant);
            conversation.setParticipants(participantSet);
            conversationRepository.save(conversation);
            // gửi tin nhắn hệ thống thông báo thành viên mới được thêm
            String messageContent = user.getName() + " đã thêm " + userToAdd.getName() + " vào nhóm";
            sendSystemMessage(conversation, messageContent);
            List<Participant> listParticipant = new ArrayList<Participant>(participantSet);
            return ResponseEntity.ok(new PageImpl<Participant>(listParticipant,pageable,participantSet.size()));
        }
        return ResponseEntity.badRequest().body(new MessageResponse("Không thể thêm khi không phải thành viên của nhóm"));
    }

    /**
     * Rời nhóm
     * @param account
     * @param conversationId
     * @return
     */
    @PutMapping("/leave/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> leaveConversation(@AuthenticationPrincipal Account account
            , @PathVariable("conversationId") String conversationId){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NullPointerException("Hội thoại không tồn tại"));
        // kiểm tra có phải hội thoại cá nhân hay không?
        if (conversation.getConversationType() == ConversationType.ONE_ONE){
            return ResponseEntity.badRequest().body(new MessageResponse("Không thể rời nhóm trong hội thoại cá nhân"));
        }
        Set<Participant> participantSet = conversation.getParticipants();
        if(participantSet.isEmpty())
            return ResponseEntity.ok(new ArrayList<>());
        // kiểm tra người dùng có phải thành viên của nhóm hay không?
        if (isParticipant(conversation, user)){
            // tạo participant cho userToAdd
            Participant leaveParticipant = Participant.builder()
                    .userId(user.getId())
                    .build();

            participantSet.removeIf(participant -> leaveParticipant.getUserId().equals(participant.getUserId()));
            conversation.setParticipants(participantSet);
            conversationRepository.save(conversation);
            // gửi tin nhắn hệ thống thông báo thành viên mới được thêm
            String messageContent = user.getName() + " đã rời khỏi nhóm";
            sendSystemMessage(conversation, messageContent);
            return ResponseEntity.ok(new MessageResponse(messageContent));
        }
        return ResponseEntity.badRequest().body(new MessageResponse("Không thể rời nhóm khi không phải thành viên của nhóm"));
    }

    /**
     * Đuổi thành viên vào nhóm
     * @param account
     * @param participantDTO
     * @return
     */
    @PutMapping("/kick")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> kickParticipant(@AuthenticationPrincipal Account account, @RequestBody ParticipantDTO participantDTO, Pageable pageable) {
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        User userToKick = userRepository.findById(participantDTO.getUserId())
                .orElseThrow(() -> new NullPointerException("Người dùng được kick không tồn tại"));
        Conversation conversation = conversationRepository.findById(participantDTO.getConversationId())
                .orElseThrow(() -> new NullPointerException("Hội thoại không tồn tại"));
        // kiểm tra có phải hội thoại cá nhân hay không?
        if (conversation.getConversationType() == ConversationType.ONE_ONE) {
            return ResponseEntity.badRequest().body(new MessageResponse("Không thể kick thành viên vào hội thoại cá nhân"));
        }
        Set<Participant> participantSet = conversation.getParticipants();
        // kiểm tra người dùng và người bị kick có phải thành viên của nhóm hay không?
        if (isParticipant(conversation, user) && isParticipant(conversation, userToKick)){
            // kiểm tra quyền admin của người dùng, vì không thể kick admin
            if (isAdmin(conversation, user) && !isAdmin(conversation, userToKick)){
                for (Participant participant: participantSet) {
                    if (userToKick.getId().equals(participant.getUserId())){
                        participantSet.remove(participant);
                        conversation.setParticipants(participantSet);
                        conversationRepository.save(conversation);
                        // gửi tin nhắn hệ thống thông báo thành viên mới được thêm
                        String messageContent = user.getName() + " đã đuổi " + userToKick.getName() + " ra khỏi nhóm";
                        sendSystemMessage(conversation, messageContent);
                        List<Participant> listParticipant = new ArrayList<Participant>(participantSet);
                        return ResponseEntity.ok(new PageImpl<Participant>(listParticipant,pageable,participantSet.size()));
                    }
                }
            }
            return ResponseEntity.badRequest().body(new MessageResponse("Không thể kick thành viên khi không phải admin hay người bị kick là admin"));
        }
        return ResponseEntity.badRequest().body(new MessageResponse("Không thể kick khi không phải thành viên của nhóm"));
    }

    /**
     * Xóa nhóm
     * @param account
     * @param conversationId
     * @return
     */
    @DeleteMapping("/delete/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteConversation(@AuthenticationPrincipal Account account
            , @PathVariable("conversationId") String conversationId){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NullPointerException("Hội thoại không tồn tại"));
        // kiểm tra có phải hội thoại cá nhân hay không?
        if (conversation.getConversationType() == ConversationType.ONE_ONE){
            return ResponseEntity.badRequest().body(new MessageResponse("Không thể xóa hội thoại cá nhân"));
        }
        if (user.getId().equals(conversation.getCreatedByUserId())){
            List<Message> messages = messageRepository.getMessageOfConversation(conversationId, Pageable.unpaged()).getContent();
            messageRepository.deleteAll(messages);
            conversationRepository.delete(conversation);
            return ResponseEntity.ok(new MessageResponse("Xóa nhóm thành công"));
        }
        return ResponseEntity.badRequest().body(new MessageResponse("Không thể xóa nhóm khi người tạo nhóm của nhóm"));
    }

    /**
     * Đổi ảnh đại diện nhóm
     * @param account
     * @param conversationId
     * @param multipartFile
     * @return
     */
    @PutMapping("/{conversationId}/changeImage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changeImage(@AuthenticationPrincipal Account account
            , @PathVariable("conversationId") String conversationId, MultipartFile multipartFile){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Conversation conversation = conversationRepository.findById(conversationId).get();
        if (conversation == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Conversation null"));
        logger.log(Level.INFO, "user = {} is changing conversation image", user.getName());
        if (multipartFile.isEmpty())
            return ResponseEntity.badRequest().body(new MessageResponse("No files is selected"));
        String newImageUrl = s3Service.uploadFile(multipartFile);
        conversation.setImageUrl(newImageUrl);
        conversationRepository.save(conversation);
        String messageContent = user.getName() + " đã đổi ảnh đại diện nhóm";
        sendSystemMessage(conversation, messageContent);
        return ResponseEntity.ok(conversation);
    }

    @PutMapping("/rename/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> renameConversation(@AuthenticationPrincipal Account account
            , @RequestParam("newName") String newName, @PathVariable("conversationId") String conversationId){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Conversation conversation = conversationRepository.findById(conversationId).get();
        if (conversation == null)
            return ResponseEntity.badRequest().body(new MessageResponse("Conversation null"));
        logger.log(Level.INFO, "user = {} is changing conversation name", user.getName());
        if (newName.equals(""))
            return ResponseEntity.badRequest().body(new MessageResponse("Nhập tên mới cho nhóm để đổi tên nhóm"));
        conversation.setName(newName);
        conversationRepository.save(conversation);
        String messageContent = user.getName() + " đã đổi tên nhóm thành "+ newName;
        sendSystemMessage(conversation, messageContent);
        return ResponseEntity.ok(conversation);
    }

    private void sendSystemMessage(Conversation conversation, String messageBody){
        Message message = Message.builder()
                .senderId(null) // system message => senderId null
                .conversationId(conversation.getId())
                .messageType(MessageType.SYSTEM)
                .content(messageBody)
                .pin(false)
                .build();
        chatService.sendSystemMessage(message, conversation);
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

    public boolean isAdmin(Conversation conversation, User userToCheck){
        Set<Participant> participantSet = conversation.getParticipants();
        // kiểm tra người dùng có phải thành viên của nhóm hay không?
        for (Participant participant: participantSet) {
            if (userToCheck.getId().equals(participant.getUserId())) {
                // kiểm tra người dùng có phải admin của nhóm hay không?
                if (participant.isAdmin())
                    return true;
            }
        }
        return false;
    }
}
