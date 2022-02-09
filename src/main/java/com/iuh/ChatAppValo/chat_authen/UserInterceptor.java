package com.iuh.ChatAppValo.chat_authen;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.ArrayList;
import java.util.Map;

import static org.springframework.messaging.support.NativeMessageHeaderAccessor.NATIVE_HEADERS;

/**
 * Class implement ChannelInterceptor để lưu thông tin xác thực người dùng hiện tại
 * nhằm định vị user để server gửi tin nhắn về khi có
 */
public class UserInterceptor implements ChannelInterceptor {

    /**
     * Method preSend đọc header để lấy id và token trước khi gửi lên server
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if(accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())){
            Object raw = message.getHeaders().get(NATIVE_HEADERS);

            if(raw instanceof Map<?,?>){
                //Headers ở client gửi lên server
                Object userId = ((Map<?,?>)raw).get("userId");
                Object accessToken = ((Map<?,?>)raw).get("token");
                if (userId instanceof ArrayList){
                    UserPrincipal principal = new UserPrincipal(((ArrayList<String>) userId).get(0));
                    principal.setToken(((ArrayList<String>) accessToken).get(0));
                    accessor.setUser(principal);
                }
            }
        }
        return message;
    }
}

