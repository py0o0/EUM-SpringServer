package com.debate.dto;

import lombok.Data;

@Data
public class CommentReqDto {
    String content;
    String language;
    String emotion;
    Long debateId;
}
