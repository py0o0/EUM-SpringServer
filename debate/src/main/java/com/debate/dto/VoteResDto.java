package com.debate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class VoteResDto {
    private double agreePercent;
    private double disagreePercent;
    private long voteCnt;
    private Map<String, Double> nationPercent;

    @Builder
    public VoteResDto(double agreePercent, double disagreePercent, long voteCnt, Map<String, Double> nationPercent) {
        this.agreePercent = agreePercent;
        this.disagreePercent = disagreePercent;
        this.voteCnt = voteCnt;
        this.nationPercent = nationPercent;
    }
}
