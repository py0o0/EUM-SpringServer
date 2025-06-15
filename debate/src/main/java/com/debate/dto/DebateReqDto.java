package com.debate.dto;

import lombok.Data;

@Data
public class DebateReqDto {
    String title;
    String content;
    String category;
    String emotion;
}
