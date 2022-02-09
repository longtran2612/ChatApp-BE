package com.iuh.ChatAppValo.dto.response;

import com.iuh.ChatAppValo.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMessage {
    private Message message;
    private String userName;
    private String userImgUrl;
}
