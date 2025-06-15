package com.debate.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "debate")
@NoArgsConstructor
public class Debate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long debateId;

    private String createdAt;
    private Long views = 0L;
    private Long voteCnt = 0L;
    private Long commentCnt = 0L;
    private Long agreeCnt = 0L;
    private Long disagreeCnt = 0L;
    private String category;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now().toString();
    }

    @Builder
    public Debate(Long debateId, String createdAt, Long views, Long voteCnt, Long commentCnt, Long agreeCnt, Long disagreeCnt, String category) {
        this.debateId = debateId;
        this.createdAt = createdAt;
        this.views = views;
        this.voteCnt = voteCnt;
        this.commentCnt = commentCnt;
        this.agreeCnt = agreeCnt;
        this.disagreeCnt = disagreeCnt;
        this.category = category;
    }
}
