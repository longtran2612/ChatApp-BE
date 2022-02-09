package com.iuh.ChatAppValo.controller;

import com.iuh.ChatAppValo.dto.request.PhoneNumber;
import com.iuh.ChatAppValo.dto.response.SearchUserResponse;
import com.iuh.ChatAppValo.entity.Account;
import com.iuh.ChatAppValo.entity.User;
import com.iuh.ChatAppValo.dto.request.UpdateUserRequest;
import com.iuh.ChatAppValo.jwt.response.MessageResponse;
import com.iuh.ChatAppValo.repositories.FriendRepository;
import com.iuh.ChatAppValo.repositories.UserRepository;
import com.iuh.ChatAppValo.services.AmazonS3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private AmazonS3Service s3Service;

//    @GetMapping("/{userId}")
//    @PreAuthorize("hasRole('USER')")
//    public User findUserById(@PathVariable String userId){
//        return userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("Không tồn tại người dùng!!"));
//    }
    @GetMapping("/phone={userPhone}")
    @PreAuthorize("hasRole('USER')")
    public User findUserByPhone(@PathVariable String userPhone){
        return userRepository.findDistinctByPhone(userPhone).orElseThrow(() -> new UsernameNotFoundException("Không tồn tại người dùng!!"));
    }

    /**
     * get user by id
     * @param userId
     * @return user
     */
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public User findUserById(@PathVariable String userId){
        return userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("Không tồn tại người dùng!!"));
    }

    @PostMapping("/phoneNumbers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUsersByPhoneNumbers(@AuthenticationPrincipal Account account, @RequestBody PhoneNumber phoneNumber, Pageable pageable){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        if (phoneNumber.getPhoneNumbers().isEmpty()){
            return ResponseEntity.badRequest().body(new MessageResponse("Danh bạ điện thoại rỗng"));
        }
        System.out.println(phoneNumber.getPhoneNumbers());
        List<SearchUserResponse> responseList = new ArrayList<>();
        for (String phone : phoneNumber.getPhoneNumbers()) {
            try {
                User userToCheck = userRepository.findDistinctByPhone(phone).get();
                if (userToCheck != null){
                    if (isFriend(user.getId(), userToCheck.getId())){
                        SearchUserResponse response = SearchUserResponse.builder()
                                .user(userToCheck)
                                .isFriend(true)
                                .build();
                        responseList.add(response);
                    } else {
                        SearchUserResponse response = SearchUserResponse.builder()
                                .user(userToCheck)
                                .isFriend(false)
                                .build();
                        responseList.add(response);
                    }
                }
            } catch (Exception e){
                System.out.println(e);
            }
        }
        System.out.println(responseList);
        Page<SearchUserResponse> responsePage = new PageImpl<>(responseList, pageable, responseList.size());
        return ResponseEntity.ok(responsePage);
    }

    /**
     * find all user with a specific address to return a friend suggest list depend on address
     * @param address
     * @return
     */
    @GetMapping("/suggest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> findUserByAddress(@AuthenticationPrincipal Account account, @RequestParam("address") String address,
                                               Pageable pageable){
        User userFromData = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        List<User> friendSuggestList = userRepository.findAllByAddressContainingIgnoreCase(address);
        if (friendSuggestList.isEmpty() || friendSuggestList == null) {
            friendSuggestList = userRepository.findAllByStatusContainingIgnoreCase("Online").subList(0, 10);
        }
        List<SearchUserResponse> responseList = new ArrayList<>();
        for (User userToCheck: friendSuggestList) {
            if (isFriend(userFromData.getId(), userToCheck.getId())){
                SearchUserResponse searchUserResponse = SearchUserResponse.builder()
                        .user(userToCheck)
                        .isFriend(true)
                        .build();
                responseList.add(searchUserResponse);
            } else {
                SearchUserResponse searchUserResponse = SearchUserResponse.builder()
                        .user(userToCheck)
                        .isFriend(false)
                        .build();
                responseList.add(searchUserResponse);
            }
        }
        Page<SearchUserResponse> responsePage = new PageImpl<>(responseList, pageable, responseList.size());
        return ResponseEntity.ok(responsePage);
    }

    /**
     * partial update user info
     * @param account
     * @param user
     * @return
     * @throws Exception
     */
    @PatchMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public User partialUpdateUser(@AuthenticationPrincipal Account account, @RequestBody User user) throws Exception{
        User userFromData = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        for (Field field: User.class.getDeclaredFields()){
            String fieldName = field.getName();
            if (fieldName.equals("id")){
                continue;
            }
            Method getter = User.class.getDeclaredMethod("get"+ StringUtils.capitalize(fieldName));
            Object fieldValue = getter.invoke(user);

            if (Objects.nonNull(fieldValue)){
                userRepository.partialUpdate(userFromData.getId(), fieldName, fieldValue);
            }
        }
        return userRepository.findById(userFromData.getId()).get();
    }
    
    @PutMapping("/updateUser/{userPhone}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(@PathVariable String userPhone, @RequestBody UpdateUserRequest updateUser){
        //ktra co ton tai nguoi dung?
    	if(!userRepository.findDistinctByPhone(userPhone).isPresent())
    		return ResponseEntity.badRequest().body(new MessageResponse("Người dùng không tồn tại!"));
    	User user=userRepository.findDistinctByPhone(userPhone).get();
    	
    	user.setAddress(updateUser.getAddress());
    	user.setDateOfBirth(updateUser.getDateOfBirth());
    	user.setEmail(updateUser.getEmail());
    	user.setGender(updateUser.getGender());
    	user.setImgUrl(updateUser.getImgUrl());
    	user.setName(updateUser.getName());
    	userRepository.save(user);
    	
    	return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/deleteUser/{userPhone}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUser(@PathVariable String userPhone){
        //ktra co ton tai nguoi dung?
    	if(!userRepository.findDistinctByPhone(userPhone).isPresent())
    		return ResponseEntity.badRequest().body(new MessageResponse("Người dùng không tồn tại!"));
    	userRepository.delete(userRepository.findDistinctByPhone(userPhone).get());
    	return ResponseEntity.ok(new MessageResponse("Xóa người dùng thành công"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<User> findAllUser(){
        return userRepository.findAll();
    }

    /**
     * đổi ảnh đại diện cá nhân
     * @param account
     * @param multipartFile
     * @return
     */
    @PutMapping("/me/changeImage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changeImage(@AuthenticationPrincipal Account account, MultipartFile multipartFile){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        if (multipartFile.isEmpty())
            return ResponseEntity.badRequest().body(new MessageResponse("No file is selected"));
        String newImageUrl = s3Service.uploadFile(multipartFile);
        user.setImgUrl(newImageUrl);
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchByNameAndPhone(@AuthenticationPrincipal Account account, @RequestParam String textToSearch
            , Pageable pageable){
        User user = userRepository.findDistinctByPhone(account.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        List<User> users = userRepository.findAllByNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByNameAsc(textToSearch, textToSearch);
        List<SearchUserResponse> responseList = new ArrayList<>();
        for (User userToCheck: users) {
            if (isFriend(user.getId(), userToCheck.getId())){
                SearchUserResponse searchUserResponse = SearchUserResponse.builder()
                        .user(userToCheck)
                        .isFriend(true)
                        .build();
                responseList.add(searchUserResponse);
            } else {
                SearchUserResponse searchUserResponse = SearchUserResponse.builder()
                        .user(userToCheck)
                        .isFriend(false)
                        .build();
                responseList.add(searchUserResponse);
            }
        }
        Page<SearchUserResponse> responsePage = new PageImpl<>(responseList, pageable, responseList.size());
//        if (users.isEmpty())
//            return ResponseEntity.ok(new ArrayList<SearchUserResponse>());
        return ResponseEntity.ok(responsePage);
    }

    /**
     * check user and friend are friend in system?
     * @param userId
     * @param friendId
     * @return true/false
     */
    private boolean isFriend(String userId, String friendId){
        if (friendRepository.isFriend(userId, friendId))
            return true;
        return false;
    }
}
