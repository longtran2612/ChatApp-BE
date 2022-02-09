package com.iuh.ChatAppValo.controller;

import com.iuh.ChatAppValo.dto.request.DeleteFriendDTO;
import com.iuh.ChatAppValo.entity.Account;
import com.iuh.ChatAppValo.entity.Friend;
import com.iuh.ChatAppValo.entity.User;
import com.iuh.ChatAppValo.jwt.response.MessageResponse;
import com.iuh.ChatAppValo.repositories.FriendRepository;
import com.iuh.ChatAppValo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/friends")
public class FriendController {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = Logger.getLogger(FriendController.class.getName());

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFriendList(@AuthenticationPrincipal Account account, Pageable pageable){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        Page<Friend> friendPage = friendRepository.getFriendListOfUserWithUserId(user.getId(), pageable);
        if (friendPage.isEmpty())
            logger.log(Level.INFO, "friend list is empty");
        return ResponseEntity.ok(friendPage);
    }
    
    @DeleteMapping("/deleteFriend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteFriend(@RequestBody DeleteFriendDTO deleteFriendDTO){
    	if(!friendRepository.isFriend(deleteFriendDTO.getUseId(), deleteFriendDTO.getFriendID())) {
    		return ResponseEntity.badRequest().body(new MessageResponse("2 người không phải bạn bè!"));
    	}
    	friendRepository.delete(friendRepository.findFriend(deleteFriendDTO.getUseId(), deleteFriendDTO.getFriendID()));
    	friendRepository.delete(friendRepository.findFriend(deleteFriendDTO.getFriendID(), deleteFriendDTO.getUseId()));
    	return ResponseEntity.ok(new MessageResponse("Xóa bạn thành công"));
    }
    
}
