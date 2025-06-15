package com.debate.repository;

import com.debate.entity.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    long countByComment_CommentIdAndOption(Long commentId, String 싫어요);

    CommentReaction findByComment_CommentIdAndUser_UserId(long commentId, long userId);
}
