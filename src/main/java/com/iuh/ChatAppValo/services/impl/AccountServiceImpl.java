package com.iuh.ChatAppValo.services.impl;

import com.iuh.ChatAppValo.entity.Account;
import com.iuh.ChatAppValo.entity.enumEntity.RoleType;
import com.iuh.ChatAppValo.entity.User;
import com.iuh.ChatAppValo.jwt.request.SignupRequest;
import com.iuh.ChatAppValo.repositories.AccountRepository;
import com.iuh.ChatAppValo.repositories.UserRepository;
import com.iuh.ChatAppValo.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class AccountServiceImpl implements AccountService, UserDetailsService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    private static final Logger logger = Logger.getLogger(AccountService.class.getName());

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Không tồn tại tài khoản " + username));
        return Account.builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .roles(RoleType.ROLE_USER.toString())
                .build();
    }

    /**
     *  set refresh token for account
     * @param username
     * @param refreshToken
     */
    @Override
    public void setRefreshToken(String username, String refreshToken){
        Account account = accountRepository.findByUsername(username).get();
        account.setRefreshToken(refreshToken);
        accountRepository.save(account);
    }

    /**
     *  set state online for account
     * @param username
     */
    @Override
    public void setOnlineStatus(String username){
        User user = userRepository.findDistinctByPhone(username)
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        user.setStatus("Online");
        System.out.println(user);
        userRepository.save(user);
    }

    /**
     *  set state offline for account
     * @param username
     */
    @Override
    public void setOfflineStatus(String username){
        User user = userRepository.findDistinctByPhone(username)
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));
        user.setStatus("Offline");
        System.out.println(user);
        userRepository.save(user);
    }

    public boolean signup(SignupRequest signupRequest){
        if (accountRepository.existsByUsername(signupRequest.getUsername()))
            return false;
        Account account = Account.builder()
                .username(signupRequest.getUsername())
                .password(encoder.encode(signupRequest.getPassword()))
                .roles(RoleType.ROLE_USER.toString())
                .build();
        accountRepository.save(account);
        return true;
    }

}
