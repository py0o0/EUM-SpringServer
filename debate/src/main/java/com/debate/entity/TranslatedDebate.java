package com.debate.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "translated_debate")
public class TranslatedDebate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long translationDebateId;

    @ManyToOne
    @JoinColumn(name = "debate_id")
    private Debate debate;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String language;

    @Builder
    public TranslatedDebate(Long translationDebateId, Debate debate, String content, String title, String language) {
        this.translationDebateId = translationDebateId;
        this.debate = debate;
        this.content = content;
        this.title = title;
        this.language = language;
    }
}
