package com.iuh.ChatAppValo.dto.request;

import com.iuh.ChatAppValo.entity.Participant;
import lombok.Data;

import java.util.Set;

@Data
public class ConversationCreateDTO {
    private String name;
    private Set<Participant> participants;
}
