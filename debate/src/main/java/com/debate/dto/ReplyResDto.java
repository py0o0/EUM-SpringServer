package com.debate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReplyResDto {
    Long replyId;
    Long like;
    Long dislike;
    String isState;
    String content;
    String userName;
    String createdAt;
    String nation;
    String voteState;
    long userId;

    @Builder
    public ReplyResDto(Long replyId, Long like, Long dislike, String content, String isState, String userName, String createdAt, long userId, String nation, String voteState) {
        this.replyId = replyId;
        this.like = like;
        this.dislike = dislike;
        this.content = content;
        this.isState = isState;
        this.userName = userName;
        this.createdAt = createdAt;
        this.userId = userId;
        this.nation = nation;
        this.voteState = voteState;
    }
}
