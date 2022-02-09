package com.iuh.ChatAppValo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iuh.ChatAppValo.entity.enumEntity.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Reaction {
    private String userId;
    private ReactionType reactionType;
}
