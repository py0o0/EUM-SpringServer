package com.debate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KafkaCommentDto {
    Long receiverId;
    Long senderId;
    Long postId;
    String serviceType;

    @Builder
    public KafkaCommentDto(Long receiverId, Long senderId, Long postId, String serviceType) {
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.postId = postId;
        this.serviceType = serviceType;
    }
}
