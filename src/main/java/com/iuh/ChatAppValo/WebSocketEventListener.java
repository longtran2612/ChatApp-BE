package com.iuh.ChatAppValo;

import com.iuh.ChatAppValo.chat_authen.UserPrincipal;
import com.iuh.ChatAppValo.jwt.JwtUtils;
import com.iuh.ChatAppValo.repositories.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.security.Principal;

@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AccountRepository accountRepository;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        log.info("event connecting to websocket");
    }

    /**
     * kết nối thành công đến websocket
     * cập nhật trạng thái của user thành ONLINE
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.info("event connected to websocket");
        UserPrincipal userPrincipal = (UserPrincipal) event.getUser();
        System.out.println(userPrincipal);
        if (userPrincipal != null) {
            String username = userPrincipal.getName();
            String accessToken = userPrincipal.getToken();
            if (username == null) {
                log.error("username is null");
                return;
            }
            if (accessToken == null) {
                log.error("access token is null");
                return;
            }
            if (! jwtUtils.validateJwtToken(accessToken)) {
                log.error("access token invalid");
                return;
            }
            if (jwtUtils.validateJwtToken(accessToken) && username.equals(jwtUtils.getUserNameFromJwtToken(accessToken))) {
                if (accountRepository.existsByUsername(username)) {
                    log.info("username = {} is connected", username);
                    log.info("update online status for username = {}", username);
                    accountRepository.setOnlineStatus(username);
                } else
                    log.error("userId = {} not found", username);
            }
        } else
            log.error("user is null");
    }

    /**
     * sự kiện khi người dùng đóng tab
     * có thể cập nhật trạng thái truy cập cuối cùng ở đây
     * cập nhật lastOnline cho user
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info("event disconnect to websocket");
        UserPrincipal userPrincipal = (UserPrincipal) event.getUser();
        if (userPrincipal != null) {
            String username = userPrincipal.getName();
            String accessToken = userPrincipal.getToken();
            if (username != null && accessToken != null &&
                    jwtUtils.validateJwtToken(accessToken) && username.equals(jwtUtils.getUserNameFromJwtToken(accessToken))) {
                if (accountRepository.existsByUsername(username)) {
                    log.info("username = {} is disconnect", username);
                    log.info("update offline status for userId = {}", username);
                    accountRepository.setOfflineStatus(username);
                } else
                    log.error("username = {} not found", username);
            } else
                log.error("username or access token is null");
        } else
            log.error("user is null");
    }

    /**
     * sự kiện user subcribe để lắng nghe tin nhắn
     */
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            log.info("username = {} is subscribing", user.getName());
        } else
            log.error("user is null");
    }

    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            log.info("username = {} is unsubscribing", user.getName());
        } else
            log.error("user is null");
    }
}
