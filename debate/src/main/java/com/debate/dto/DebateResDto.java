package com.debate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DebateResDto {
    Long debateId;
    Long views;
    Long like;
    Long dislike;
    Long sad;
    Long angry;
    Long hm;
    Long voteCnt;
    Long commentCnt;
    double disagreePercent;
    double agreePercent;
    String isState;
    String isVotedState;
    String title;
    String content;
    String createdAt;
    String category;
    String nation;

    @Builder
    public DebateResDto(Long debateId, Long views,
                        Long like, Long dislike, Long sad, Long angry, Long hm,
                        Long voteCnt, Long commentCnt, double disagreePercent, double agreePercent,
                        String title, String content, String createdAt, String category, String nation) {
        this.debateId = debateId;
        this.views = views;
        this.like = like;
        this.dislike = dislike;
        this.sad = sad;
        this.angry = angry;
        this.hm = hm;
        this.voteCnt = voteCnt;
        this.commentCnt = commentCnt;
        this.disagreePercent = disagreePercent;
        this.agreePercent = agreePercent;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.category = category;
        this.nation = nation;
    }
}
