package com.post.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CommentResDto {
    Long commentId;
    Long like;
    Long dislike;
    Long reply;
    Long postId;
    Long userId;
    String nation;
    String isState;
    String content;
    String userName;
    String createdAt;
    String postTitle;


    @Builder
    public CommentResDto(Long commentId, Long userId, String postTitle, String createdAt, String userName, String content, String isState, Long postId, Long reply, Long dislike, Long like, String nation) {
        this.commentId = commentId;
        this.userId = userId;
        this.postTitle = postTitle;
        this.createdAt = createdAt;
        this.userName = userName;
        this.content = content;
        this.isState = isState;
        this.postId = postId;
        this.reply = reply;
        this.dislike = dislike;
        this.like = like;
        this.nation = nation;
    }
}
