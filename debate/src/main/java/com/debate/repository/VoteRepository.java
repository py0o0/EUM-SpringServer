package com.debate.repository;

import com.debate.entity.Vote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    Vote findByDebate_DebateIdAndUser_UserId(Long debateId, Long userId);

    List<Vote> findByDebate_DebateId(Long debateId);

    Page<Vote> findByUser_UserId(long userId, Pageable pageable);
}
