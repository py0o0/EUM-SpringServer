package com.debate.service;

import com.debate.dto.CommentReqDto;
import com.debate.dto.CommentResDto;
import com.debate.entity.*;
import com.debate.repository.*;
import util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import util.TranslationJob;
import util.TranslationQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final DebateRepository debateRepository;
    private final TranslatedCommentRepository translatedCommentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final VoteRepository voteRepository;

    private final JwtUtil jwtUtil;
    private final TranslationQueue translationQueue;

    private Optional<User> verifyToken(String token) {    // 토큰 검증 함수
        try {
            long userId = jwtUtil.getUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if(user == null) {
                return Optional.empty();
            }
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public ResponseEntity<?> addComment(String token, CommentReqDto commentReqDto) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(user.get().getBan() == 1){
            return ResponseEntity.badRequest().body("차단된 유저");
        }

        Debate debate = debateRepository.findById(commentReqDto.getDebateId()).get();
        Comment comment = Comment.builder()
                .debate(debate)
                .user(user.get())
                .replyCnt(0L)
                .heart(0L)
                .build();
        commentRepository.save(comment);

        Vote vote = voteRepository
                .findByDebate_DebateIdAndUser_UserId(
                        comment.getDebate().getDebateId(), comment.getUser().getUserId());

        String voteState = (vote != null) ? vote.getOption() : null;

        CommentResDto commentResDto = CommentResDto.builder()
                .commentId(comment.getCommentId())
                .content(commentReqDto.getContent())
                .like(0L)
                .dislike(0L)
                .reply(0L)
                .createdAt(comment.getCreatedAt())
                .userName(comment.getUser().getName())
                .nation(comment.getUser().getNation())
                .voteState(voteState)
                .build();

        debate.setCommentCnt(debate.getCommentCnt() + 1);
        debateRepository.save(debate);

        translationQueue.enqueue(new TranslationJob(comment, commentReqDto, null));

        return ResponseEntity.ok(commentResDto);
    }

    public ResponseEntity<?> getComments(String token, long debateId, String sort, int page, int size) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }
        String language = user.get().getLanguage();

        Sort sortOptions; // 정렬
        switch (sort) {
            case "oldest":
                sortOptions = Sort.by(Sort.Direction.ASC, "createdAt");
                break;
            case "heart":
                sortOptions = Sort.by(Sort.Direction.DESC, "heart");
                break;
            default:
                sortOptions = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
        }
        Pageable pageable = PageRequest.of(page,size,sortOptions);
        Page<Comment> commentList = commentRepository.findByDebate_DebateId(debateId, pageable);

        long total = commentList.getTotalElements();

        List<CommentResDto> commentResDtoList = new ArrayList<>();
        for(Comment comment : commentList){
            TranslatedComment translatedComment = translatedCommentRepository
                    .findByComment_CommentIdAndLanguage(comment.getCommentId(), language);

            long dislike = commentReactionRepository
                    .countByComment_CommentIdAndOption(comment.getCommentId(), "싫어요");

            CommentReaction commentReaction = commentReactionRepository
                    .findByComment_CommentIdAndUser_UserId(comment.getCommentId(), user.get().getUserId());
            String option = null;
            if(commentReaction != null) {
                option = commentReaction.getOption();
            }

            Vote vote = voteRepository
                    .findByDebate_DebateIdAndUser_UserId(
                            comment.getDebate().getDebateId(), comment.getUser().getUserId());

            String voteState = (vote != null) ? vote.getOption() : null;

            CommentResDto commentResDto = CommentResDto.builder()
                    .commentId(comment.getCommentId())
                    .content(translatedComment.getContent())
                    .like(comment.getHeart())
                    .dislike(dislike)
                    .reply(comment.getReplyCnt())
                    .createdAt(comment.getCreatedAt())
                    .userName(comment.getUser().getName())
                    .nation(comment.getUser().getNation())
                    .userId(comment.getUser().getUserId())
                    .isState(option)
                    .voteState(voteState)
                    .build();

            commentResDtoList.add(commentResDto);
        }
        return ResponseEntity.ok(Map.of(
                "commentList", commentResDtoList,
                "total", total
        ));
    }

    public ResponseEntity<?> updateComment(String token, long commentId, CommentReqDto commentReqDto) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(user.get().getBan() == 1){
            return ResponseEntity.badRequest().body("차단된 유저");
        }

        Comment comment = commentRepository.findById(commentId).get();

        if(user.get() != comment.getUser()) {
            return ResponseEntity.badRequest().body("작성자만 수정 가능");
        }

        translationQueue.enqueue(new TranslationJob(comment, commentReqDto, commentId));

        return ResponseEntity.ok(commentReqDto.getContent());
    }

    @Transactional
    public ResponseEntity<?> deleteComment(String token, long commentId) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(user.get().getBan() == 1){
            return ResponseEntity.badRequest().body("차단된 유저");
        }

        Comment comment = commentRepository.findById(commentId).get();

        if(user.get() != comment.getUser() && !user.get().getRole().equals("ROLE_ADMIN")) {
            return ResponseEntity.badRequest().body("작성자/관리자만 수정 가능");
        }

        comment.getDebate().setCommentCnt(comment.getDebate().getCommentCnt() - comment.getReplyCnt() - 1);
        debateRepository.save(comment.getDebate());
        commentRepository.delete(comment);
        return ResponseEntity.ok("삭제 완료");
    }

    @Transactional
    public ResponseEntity<?> reactToComment(String token, long commentId, CommentReqDto commentReqDto) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(user.get().getBan() == 1){
            return ResponseEntity.badRequest().body("차단된 유저");
        }

        long userId = user.get().getUserId();
        Comment comment = commentRepository.findById(commentId).get();

        CommentReaction commentReaction = commentReactionRepository
                .findByComment_CommentIdAndUser_UserId(commentId, userId);

        if(commentReaction == null){
            commentReaction = CommentReaction.builder()
                    .comment(comment)
                    .user(user.get())
                    .option(commentReqDto.getEmotion())
                    .build();

            if(commentReqDto.getEmotion().equals("좋아요")){
                comment.setHeart(comment.getHeart() + 1);
                commentRepository.save(comment);
            }
            commentReactionRepository.save(commentReaction);

            long like = comment.getHeart();
            long dislike = commentReactionRepository.countByComment_CommentIdAndOption(commentId, "싫어요");

            return ResponseEntity.ok(Map.of(
                    "like", like,
                    "dislike", dislike
            ));
        }
        else {
            if (commentReaction.getOption().equals(commentReqDto.getEmotion())) {
                if (commentReqDto.getEmotion().equals("좋아요")) {
                    comment.setHeart(comment.getHeart() - 1);
                    commentRepository.save(comment);
                }

                commentReactionRepository.delete(commentReaction);

                long like = comment.getHeart();
                long dislike = commentReactionRepository.countByComment_CommentIdAndOption(commentId, "싫어요");
                return ResponseEntity.ok(Map.of(
                        "like", like,
                        "dislike", dislike
                ));
            }
            return ResponseEntity.ok("좋아요와 싫어요는 동시에 등록 불가");
        }
    }

    public ResponseEntity<?> getComment(String token, long commentId) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }
        String language = user.get().getLanguage();

        TranslatedComment translatedComment = translatedCommentRepository
                .findByComment_CommentIdAndLanguage(commentId, language);

        CommentResDto commentResDto = CommentResDto.builder()
                .content(translatedComment.getContent())
                .userName(translatedComment.getComment().getUser().getName())
                .nation(translatedComment.getComment().getUser().getNation())
                .userId(translatedComment.getComment().getUser().getUserId())
                .createdAt(translatedComment.getComment().getCreatedAt())
                .build();
        return ResponseEntity.ok(commentResDto);
    }
}
