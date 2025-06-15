package com.debate.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "comment")
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @ManyToOne
    @JoinColumn(name = "debate_id")
    private Debate debate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String createdAt;
    private Long replyCnt;
    private Long heart;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now().toString();
    }

    @Builder
    public Comment(Long commentId, Debate debate, User user, String createdAt, Long replyCnt, Long heart) {
        this.commentId = commentId;
        this.debate = debate;
        this.user = user;
        this.createdAt = createdAt;
        this.replyCnt = replyCnt;
        this.heart = heart;
    }
}
