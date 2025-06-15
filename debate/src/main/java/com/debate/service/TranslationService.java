package com.debate.service;

import com.debate.dto.CommentReqDto;
import com.debate.dto.DebateReqDto;
import com.debate.dto.KafkaCommentDto;
import com.debate.dto.ReplyReqDto;
import com.debate.entity.*;
import com.debate.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import util.TranslationJob;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TranslationService {
    @Value("${translation.api-key}")
    private String apiKey;

    private final TranslatedCommentRepository translatedCommentRepository;
    private final TranslatedReplyRepository translatedReplyRepository;
    private final TranslatedDebateRepository translatedDebateRepository;

    private final DebateRepository debateRepository;
    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String API_URL = "https://api-free.deepl.com/v2/translate";

    private final String[] targetLanguage = {"KO", "EN", "JA", "ZH", "DE", "FR", "ES", "RU"};

    public void handleJob(TranslationJob job){ // 맞는 번역 매서드 실행
        if (job.getEntity() instanceof Debate) {
            translateDebate((Debate) job.getEntity(), (DebateReqDto) job.getDto(), job.getOptionalId());
        } else if (job.getEntity() instanceof Comment) {
            translateComment((Comment) job.getEntity(), (CommentReqDto) job.getDto(), job.getOptionalId());
        } else if (job.getEntity() instanceof Reply) {
            translateReply((Reply) job.getEntity(), (ReplyReqDto) job.getDto(), job.getOptionalId());
        }
    }

    public Optional<String> translate(String text, String sourceLang, String targetLang) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("auth_key", apiKey);
        body.add("text", text);
        body.add("source_lang", sourceLang.toUpperCase());
        body.add("target_lang", targetLang.toUpperCase());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, request, Map.class);
            Map<String, Object> result = response.getBody();

            List<Map<String, String>> translations = (List<Map<String, String>>) result.get("translations");
            return Optional.of(translations.get(0).get("text"));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void translateDebate(Debate debate, DebateReqDto debateReqDto, Long debateId) {
        for (String language : targetLanguage) { // 9개 언어로 번역해서 저장
            TranslatedDebate translatedDebate = (debateId == null)
                    ? new TranslatedDebate()
                    : translatedDebateRepository.findByDebate_DebateIdAndLanguage(debate.getDebateId(), language);

            translatedDebate.setDebate(debate);
            translatedDebate.setLanguage(language);

            if (language.equals("KO")) {
                translatedDebate.setContent(debateReqDto.getContent());
                translatedDebate.setTitle(debateReqDto.getTitle());
                translatedDebateRepository.save(translatedDebate);
                continue;
            }

            Optional<String> translatedTitle = translate(
                    debateReqDto.getTitle() , "KO", language);

            Optional<String> translatedContent = translate(
                    debateReqDto.getContent() , "KO", language);

            if (translatedTitle.isEmpty() || translatedContent.isEmpty()) {
                debateRepository.delete(debate);
                break;
            }

            translatedDebate.setContent(translatedContent.get());
            translatedDebate.setTitle(translatedTitle.get());
            translatedDebateRepository.save(translatedDebate);
        }
    }

    public void translateComment(Comment comment, CommentReqDto commentReqDto, Long commentId){
        for (String language : targetLanguage) { // 9개 언어로 번역해서 저장
            TranslatedComment translatedComment = (commentId == null)
                    ? new TranslatedComment()
                    : translatedCommentRepository.findByComment_CommentIdAndLanguage(comment.getCommentId(), language);

            translatedComment.setComment(comment);
            translatedComment.setLanguage(language);

            if (commentReqDto.getLanguage().equals(language)) {
                translatedComment.setContent(commentReqDto.getContent());
                translatedCommentRepository.save(translatedComment);
                continue;
            }

            Optional<String> translatedContent = translate(
                    commentReqDto.getContent(), commentReqDto.getLanguage(), language);


            if (translatedContent.isEmpty()) {
                kafkaTemplate.send("failComment", String.valueOf(comment.getUser().getUserId()));
                commentRepository.delete(comment);
                break;
            }

            translatedComment.setContent(translatedContent.get());
            translatedCommentRepository.save(translatedComment);
        }
    }

    public void translateReply(Reply reply, ReplyReqDto replyReqDto, Long replyId) {
        for (String language : targetLanguage) { // 9개 언어로 번역해서 저장
            TranslatedReply translatedReply = (replyId == null)
                    ? new TranslatedReply()
                    : translatedReplyRepository.findByReply_ReplyIdAndLanguage(replyId, language);

            translatedReply.setLanguage(language);
            translatedReply.setReply(reply);

            if (replyReqDto.getLanguage().equals(language)) {
                translatedReply.setContent(replyReqDto.getContent());
                translatedReplyRepository.save(translatedReply);
                continue;
            }

            Optional<String> translatedContent = translate(
                    replyReqDto.getContent(), replyReqDto.getLanguage(), language);

            if (translatedContent.isEmpty()){
                kafkaTemplate.send("failComment", String.valueOf(reply.getUser().getUserId()));
                replyRepository.delete(reply);
                break;
            }

            translatedReply.setContent(translatedContent.get());
            translatedReplyRepository.save(translatedReply);
        }
    }

}
