package com.iuh.ChatAppValo.dto.response;

import com.iuh.ChatAppValo.entity.Message;
import com.iuh.ChatAppValo.entity.ReadTracking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageInfo {
    private Message message;
    private List<ReadTracking> readTrackingList;
}
