package com.debate.service;

import com.debate.dto.DebateReqDto;
import com.debate.dto.DebateResDto;
import com.debate.entity.*;
import com.debate.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import util.TranslationJob;
import util.TranslationQueue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebateService {
    private final JwtUtil jwtUtil;
    private final TranslationQueue translationQueue;

    private final UserRepository userRepository;
    private final DebateRepository debateRepository;
    private final TranslatedDebateRepository translatedDebateRepository;
    private final VoteRepository voteRepository;
    private final DebateReactionRepository debateReactionRepository;

    @Value("${ai.url}")
    String aiUrl;

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

    private static Map<String, Double> calculateVotePercent(Long agreeCnt, Long disagreeCnt) {
        Long voteCnt = agreeCnt + disagreeCnt;
        double agreePercent = 0;
        double disagreePercent = 0;

        if (voteCnt > 0) {
            agreePercent = (double) agreeCnt * 100 / voteCnt;
            disagreePercent = (double) disagreeCnt * 100 / voteCnt;
        }

        Map<String, Double> result = new HashMap<>();
        result.put("agreePercent", agreePercent);
        result.put("disagreePercent", disagreePercent);
        return result;
    }

    private String getTopNationByDebateId(Long debateId) {
        List<Vote> voteList = voteRepository.findByDebate_DebateId(debateId);
        if (voteList.isEmpty()) return null;

        Map<String, Long> nationCount = new HashMap<>();
        for (Vote vote : voteList) {
            String nation = vote.getUser().getNation();
            nationCount.put(nation, nationCount.getOrDefault(nation, 0L) + 1);
        }

        return nationCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private DebateResDto DebateToDto(Debate debate, String language) {
        TranslatedDebate translatedDebate = translatedDebateRepository
                .findByDebate_DebateIdAndLanguage(debate.getDebateId(), language);

        Map<String, Double> percentMap =
                calculateVotePercent(debate.getAgreeCnt(), debate.getDisagreeCnt());

        DebateResDto dto = DebateResDto.builder()
                .title(translatedDebate.getTitle())
                .debateId(debate.getDebateId())
                .views(debate.getViews())
                .createdAt(debate.getCreatedAt())
                .voteCnt(debate.getVoteCnt())
                .agreePercent(percentMap.get("agreePercent"))
                .disagreePercent(percentMap.get("disagreePercent"))
                .commentCnt(debate.getCommentCnt())
                .category(debate.getCategory())
                .nation(getTopNationByDebateId(debate.getDebateId()))
                .build();

        return dto;
    }

    private List<DebateResDto> transDebateToDto(List<TranslatedDebate> translatedDebateList) {
        List<DebateResDto> dtoList = new ArrayList<>();
        for(TranslatedDebate translatedDebate : translatedDebateList) {
            Debate debate = translatedDebate.getDebate();

            Map<String, Double> percentMap =
                    calculateVotePercent(debate.getAgreeCnt(), debate.getDisagreeCnt());

            DebateResDto dto = DebateResDto.builder()
                    .title(translatedDebate.getTitle())
                    .debateId(debate.getDebateId())
                    .views(debate.getViews())
                    .createdAt(debate.getCreatedAt())
                    .voteCnt(debate.getVoteCnt())
                    .agreePercent(percentMap.get("agreePercent"))
                    .disagreePercent(percentMap.get("disagreePercent"))
                    .commentCnt(debate.getCommentCnt())
                    .category(debate.getCategory())
                    .nation(getTopNationByDebateId(debate.getDebateId()))
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

    public ResponseEntity<?> write(DebateReqDto debateReqDto) {
        Debate debate = Debate.builder()
                .category(debateReqDto.getCategory())
                .views(0L)
                .agreeCnt(0L)
                .voteCnt(0L)
                .commentCnt(0L)
                .disagreeCnt(0L)
                .build();
        debateRepository.save(debate);

        translationQueue.enqueue(new TranslationJob(debate, debateReqDto, null));

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> getDebates(String token, int page, int size, String sort, String category) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        String language = user.get().getLanguage();

        Sort sortOption;
        switch (sort) {
            case "view":
                sortOption = Sort.by(Sort.Direction.DESC, "views");
                break;
            case "comment":
                sortOption = Sort.by(Sort.Direction.DESC, "commentCnt");
                break;
            case "vote":
                sortOption = Sort.by(Sort.Direction.DESC, "voteCnt");
                break;
            default:
                sortOption = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sortOption);
        Page<Debate> debateList = debateRepository.findByCategory(category, pageable);

        long total = debateList.getTotalElements();

        List<DebateResDto> debateResDtoList = new ArrayList<>();

        for(Debate debate : debateList) {
            debateResDtoList.add(DebateToDto(debate, language));
        }
        return ResponseEntity.ok(Map.of(
                "debateList", debateResDtoList,
                "total", total));
    }

    public ResponseEntity<?> getTodayDebate(String token) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        String language = user.get().getLanguage();

        String today = LocalDate.now().toString();
        List<Debate> todayDebateList = debateRepository.findAllByCreatedAtToday(today);

        List<DebateResDto> todayDebateResDtoList = new ArrayList<>();

        for (Debate debate : todayDebateList) {
            todayDebateResDtoList.add(DebateToDto(debate, language));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        String start = LocalDateTime.now().minusDays(7).format(formatter);
        String end = LocalDateTime.now().format(formatter);

        Debate topDebate = debateRepository.findTopDebateInLastWeek(start, end);

        DebateResDto topDebateResDto = DebateToDto(topDebate, language);

        Debate balancedDebate = debateRepository.findMostBalancedDebateThisWeek(start, end);

        if(balancedDebate == null){
            return ResponseEntity.ok(Map.of(
                    "todayDebateList", todayDebateResDtoList,
                    "topDebate", topDebateResDto
            ));
        }

        DebateResDto balancedDebateResDto = DebateToDto(balancedDebate, language);

        return ResponseEntity.ok(Map.of(
                "todayDebateList", todayDebateResDtoList,
                "topDebate", topDebateResDto,
                "balancedDebate", balancedDebateResDto
        ));
    }

    public ResponseEntity<?> searchDebate(String token, int page, int size, String sort, String category,
                                          String keyword, String searchBy) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        String language = user.get().getLanguage();

        Sort sortOption;
        switch (sort) {
            case "view":
                sortOption = Sort.by(Sort.Direction.DESC, "debate.views");
                break;
            case "comment":
                sortOption = Sort.by(Sort.Direction.DESC, "debate.commentCnt");
                break;
            case "vote":
                sortOption = Sort.by(Sort.Direction.DESC, "debate.voteCnt");
                break;
            default:
                sortOption = Sort.by(Sort.Direction.DESC, "debate.createdAt");
        }
        Pageable pageable = PageRequest.of(page, size, sortOption);
        Page<TranslatedDebate> debateList;

        if(searchBy.equals("제목")) {
            debateList = translatedDebateRepository.findByCategoryAndTitle(category, keyword, language, pageable);
        }else if (searchBy.equals("내용")) {
            debateList = translatedDebateRepository.findByCategoryAndContent(category, keyword, language, pageable);
        } else {
            debateList = translatedDebateRepository.findByCategoryAndTitleOrContent(category, keyword, language, pageable);
        }

        long total = debateList.getTotalElements();
        List<DebateResDto> debateResDtoList = transDebateToDto(debateList.getContent());

        return ResponseEntity.ok(Map.of(
                "debateList", debateResDtoList,
                "total", total
        ));
    }

    @Transactional
    public ResponseEntity<?> reactToDebate(String token, long debateId, DebateReqDto debateReqDto) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(user.get().getBan() == 1){
            return ResponseEntity.badRequest().body("차단된 유저");
        }

        Debate debate = debateRepository.findById(debateId).get();

        DebateReaction debateReaction = debateReactionRepository
                .findByDebate_DebateIdAndUser_UserId(debateId, user.get().getUserId());

        if(debateReaction == null) {
            debateReaction = DebateReaction.builder()
                    .debate(debate)
                    .user(user.get())
                    .option(debateReqDto.getEmotion())
                    .build();
            debateReactionRepository.save(debateReaction);
        }
        else{
            if(debateReaction.getOption().equals(debateReqDto.getEmotion())) {
                debateReactionRepository.delete(debateReaction);
            }
            else{
                return ResponseEntity.badRequest().body("하나의 감정표현만 가능");
            }
        }
        long like = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "좋아요");
        long dislike = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "싫어요");
        long sad = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "슬퍼요");
        long angry = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "화나요");
        long hm = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "글쎄요");

        DebateResDto debateResDto = DebateResDto.builder()
                .like(like)
                .dislike(dislike)
                .sad(sad)
                .angry(angry)
                .hm(hm)
                .build();

        return ResponseEntity.ok(debateResDto);
    }

    public ResponseEntity<?> getDebate(String token, long debateId) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }
        String language = user.get().getLanguage();

        Debate debate = debateRepository.findById(debateId).get();

        TranslatedDebate translatedDebate = translatedDebateRepository
                .findByDebate_DebateIdAndLanguage(debateId, language);

        long like = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "좋아요");
        long dislike = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "싫어요");
        long sad = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "슬퍼요");
        long angry = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "화나요");
        long hm = debateReactionRepository.countByDebate_DebateIdAndOption(debateId, "글쎄요");

        Map<String, Double> percentMap =
                calculateVotePercent(debate.getAgreeCnt(), debate.getDisagreeCnt());

        DebateResDto debateResDto = new DebateResDto(
                debateId, debate.getViews(), like, dislike, sad, angry, hm,
                debate.getVoteCnt(), debate.getCommentCnt(),
                percentMap.get("disagreePercent"), percentMap.get("agreePercent"),
                translatedDebate.getTitle(), translatedDebate.getContent(),
                debate.getCreatedAt(), debate.getCategory(),getTopNationByDebateId(debateId)
        );

        DebateReaction debateReaction = debateReactionRepository
                .findByDebate_DebateIdAndUser_UserId(debateId, user.get().getUserId());

        if(debateReaction != null) {
            debateResDto.setIsState(debateReaction.getOption());
        }

        Vote vote = voteRepository.findByDebate_DebateIdAndUser_UserId(debateId, user.get().getUserId());
        if(vote != null) {
            debateResDto.setIsVotedState(vote.getOption());
        }

        return ResponseEntity.ok(debateResDto);
    }

    public ResponseEntity<?> getVotedDebate(String token, long userId, int page, int size) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        String language = user.get().getLanguage();

        Pageable pageable = PageRequest.of(page, size);
        Page<Vote> voteList = voteRepository.findByUser_UserId(userId, pageable);

        long total = voteList.getTotalElements();

        List<DebateResDto> debateResDtoList = new ArrayList<>();

        for(Vote vote : voteList){
            debateResDtoList.add(DebateToDto(vote.getDebate(), language));
        }

        return ResponseEntity.ok(Map.of(
                "debateList", debateResDtoList,
                "total", total));
    }

    public ResponseEntity<?> recommendDebate(String token) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        long userId = user.get().getUserId();

        String url = aiUrl + "/user/" + userId + "/preferences";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if(!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(response.getStatusCode()).body("유저 선호도 불러오기 실패");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        try{
            rootNode = objectMapper.readTree(response.getBody());
        }catch(JsonProcessingException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("선호도 불러오기 실패");
        }

        JsonNode discussionPreferences = rootNode.path("discussion_preferences");
        if(discussionPreferences.isMissingNode()) {
            return ResponseEntity.badRequest().body("discussion_preferences 항목이 존재하지 않음");
        }

        Map<String, Double> preferencesMap = new HashMap<>();
        discussionPreferences.fields().forEachRemaining(field -> {
            preferencesMap.put(field.getKey(), field.getValue().doubleValue());
        });

        List<Map.Entry<String, Double>> sortedTag = preferencesMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).toList();

        Map.Entry<String, Double> bestTag = sortedTag.get(0);
        Map.Entry<String, Double> secondTag = sortedTag.get(1);
        Map.Entry<String, Double> thirdTag = sortedTag.get(2);

        String language = user.get().getLanguage();
        String sevenDaysAgo = LocalDate.now().minusDays(7).toString();

        List<List<DebateResDto>> debateResDtoList = new ArrayList<>();

        if(bestTag.getValue() >= 0.9){
            List<TranslatedDebate> debateList = translatedDebateRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            bestTag.getKey(), language, sevenDaysAgo, 2
                    );

            List<DebateResDto> debateDto = transDebateToDto(debateList);
            debateResDtoList.add(debateDto);

            debateList = translatedDebateRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            secondTag.getKey(), language, sevenDaysAgo, 1
                    );
            debateDto = transDebateToDto(debateList);
            debateResDtoList.add(debateDto);
        }
        else{
            List<TranslatedDebate> debateList = translatedDebateRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            bestTag.getKey(), language, sevenDaysAgo, 1
                    );

            List<DebateResDto> debateDto = transDebateToDto(debateList);
            debateResDtoList.add(debateDto);

            debateList = translatedDebateRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            secondTag.getKey(), language, sevenDaysAgo, 1
                    );
            debateDto = transDebateToDto(debateList);
            debateResDtoList.add(debateDto);

            debateList = translatedDebateRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            thirdTag.getKey(), language, sevenDaysAgo, 1
                    );
            debateDto = transDebateToDto(debateList);
            debateResDtoList.add(debateDto);
        }
        return ResponseEntity.ok(Map.of(
                "debateList", debateResDtoList,
                "analysis", preferencesMap
        ));
    }
}
