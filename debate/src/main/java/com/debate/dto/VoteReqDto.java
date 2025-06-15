package com.debate.dto;

import lombok.Data;

@Data
public class VoteReqDto {
    long debateId;
    String option;
}
