package com.debate.repository;

import com.debate.entity.Debate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebateRepository extends JpaRepository<Debate, Long> {
    @Query("SELECT d FROM Debate d WHERE " +
            "(:category = '전체' OR d.category = :category)")
    Page<Debate> findByCategory(String category, Pageable pageable);

    @Query("SELECT d FROM Debate d WHERE d.createdAt LIKE CONCAT(:today, '%')")
    List<Debate> findAllByCreatedAtToday(@Param("today") String today);

    @Query("""
    SELECT d FROM Debate d
    WHERE d.createdAt BETWEEN :start AND :end
    ORDER BY (d.views + d.commentCnt) DESC
    Limit 1
    """)
    Debate findTopDebateInLastWeek(@Param("start") String start, @Param("end") String end);

    @Query(value = """
    SELECT * FROM debate 
    WHERE created_at BETWEEN :start AND :end
    AND (agree_cnt + disagree_cnt) >= 10
    ORDER BY ABS(agree_cnt - disagree_cnt) ASC 
    LIMIT 1
    """, nativeQuery = true)
    Debate findMostBalancedDebateThisWeek(@Param("start") String start, @Param("end") String end);
}
