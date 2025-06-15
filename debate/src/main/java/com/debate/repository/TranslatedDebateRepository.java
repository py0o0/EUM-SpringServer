package com.debate.repository;

import com.debate.entity.TranslatedDebate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface TranslatedDebateRepository extends JpaRepository<TranslatedDebate, Long> {
    TranslatedDebate findByDebate_DebateIdAndLanguage(Long debateId, String language);

    @Query("SELECT td FROM TranslatedDebate td " +
            "WHERE (:category = '전체' OR td.debate.category = :category) " +
            "AND td.language = :language " +
            "AND td.title LIKE CONCAT('%', :keyword, '%')")
    Page<TranslatedDebate> findByCategoryAndTitle(@Param("category") String category,
                                                  @Param("keyword") String keyword,
                                                  @Param("language") String language,
                                                  Pageable pageable);

    @Query("SELECT td FROM TranslatedDebate td " +
            "WHERE (:category = '전체' OR td.debate.category = :category) " +
            "AND td.language = :language " +
            "AND td.content LIKE CONCAT('%', :keyword, '%')")
    Page<TranslatedDebate> findByCategoryAndContent(@Param("category") String category,
                                                    @Param("keyword") String keyword,
                                                    @Param("language") String language,
                                                    Pageable pageable);

    @Query("SELECT td FROM TranslatedDebate td " +
            "WHERE (:category = '전체' OR td.debate.category = :category) " +
            "AND td.language = :language " +
            "AND (td.title LIKE CONCAT('%', :keyword, '%') OR td.content LIKE CONCAT('%', :keyword, '%'))")
    Page<TranslatedDebate> findByCategoryAndTitleOrContent(@Param("category") String category,
                                                           @Param("keyword") String keyword,
                                                           @Param("language") String language,
                                                           Pageable pageable);


    @Query(value =
            "select td.* from translated_debate td " +
            "join debate d on td.debate_id = d.debate_id " +
            "where d.category = :tag " +
            "and td.language = :language " +
            "and d.created_at >= :sevenDaysAgo " +
            "order by rand()" +
            "limit :i "
        ,nativeQuery = true)
    List<TranslatedDebate> findRandomByTagAndLanguageAndRecentDayAndCount(
            @Param("tag") String tag,
            @Param("language") String language,
            @Param("sevenDaysAgo") String sevenDaysAgo,
            @Param("i") int i);
}
