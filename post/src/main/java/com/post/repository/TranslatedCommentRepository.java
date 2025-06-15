package com.post.repository;

import com.post.entity.TranslatedComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslatedCommentRepository extends JpaRepository<TranslatedComment, Long> {
    TranslatedComment findByComment_CommentIdAndLanguage(Long commentId, String language);
}
