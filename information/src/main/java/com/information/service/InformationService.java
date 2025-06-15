package com.information.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.information.dto.InformationReqDto;
import com.information.dto.InformationResDto;
import com.information.entity.*;
import com.information.repository.*;
import com.information.entity.*;
import com.information.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import util.TranslationJob;
import util.TranslationQueue;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InformationService {
    private final InformationRepository informationRepository;
    private final InformationFileRepository informationFileRepository;
    private final BookmarkRepository bookmarkRepository;
    private final TranslatedInformationRepository translatedInformationRepository;
    private final UserRepository userRepository;

    private final TranslationQueue translationQueue;
    private final AwsS3Service awsS3Service;
    private final JwtUtil jwtUtil;



    @Value("${ai.url}")
    private String aiUrl;

    public String extractKeyFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);  // 맨 마지막 파일명만 추출
    }

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

    private List<InformationResDto> informationToDto(Page<Information> informationList, User user) {
        List<InformationResDto> informationResDtoList = new ArrayList<>();

        for(Information information : informationList) {
            TranslatedInformation translatedInformation = translatedInformationRepository
                    .findByInformation_InformationIdAndLanguage(
                            information.getInformationId(), user.getLanguage());

            if(translatedInformation == null) {
                continue;
            }

            long state = bookmarkRepository.countByInformation_InformationIdAndUser_UserId
                    (information.getInformationId(), user.getUserId());

            InformationResDto informationResDto = InformationResDto.builder()
                    .category(information.getCategory())
                    .informationId(information.getInformationId())
                    .views(information.getViews())
                    .title(translatedInformation.getTitle())
                    .content(translatedInformation.getContent())
                    .userName(information.getUser().getName())
                    .createdAt(information.getCreatedAt())
                    .isState(state)
                    .build();

            informationResDtoList.add(informationResDto);
        }
        return informationResDtoList;
    }

    private List<InformationResDto> transInfoToDto(List<TranslatedInformation> transInfoList, User user) {
        List<InformationResDto> informationResDtoList = new ArrayList<>();

        for(TranslatedInformation translatedInformation : transInfoList) {
            Information information = translatedInformation.getInformation();

            long state = bookmarkRepository.countByInformation_InformationIdAndUser_UserId
                    (information.getInformationId(), user.getUserId());

            InformationResDto informationResDto = InformationResDto.builder()
                    .category(information.getCategory())
                    .informationId(information.getInformationId())
                    .views(information.getViews())
                    .title(translatedInformation.getTitle())
                    .content(translatedInformation.getContent())
                    .userName(information.getUser().getName())
                    .createdAt(information.getCreatedAt())
                    .isState(state)
                    .build();

            informationResDtoList.add(informationResDto);
        }
        return informationResDtoList;
    }

    public ResponseEntity<?> write(String token, InformationReqDto informationReqDto) throws JsonProcessingException {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(!user.get().getRole().equals("ROLE_ADMIN")) {
            return ResponseEntity.badRequest().body("관리자만 작성 가능");
        }

        Information information = Information.builder()
                .user(user.get())
                .views(0L)
                .category(informationReqDto.getCategory())
                .build();

        information = informationRepository.save(information);

        List<String> files = informationReqDto.getFiles();
        if (files != null) {
            for (String file : files) {
                InformationFile informationFile = InformationFile.builder()
                        .information(information)
                        .url(file)
                        .build();
                informationFileRepository.save(informationFile);
            }
        }

        translationQueue.enqueue(new TranslationJob(information, informationReqDto, null));

        InformationResDto informationResDto = InformationResDto.builder()
                .category(informationReqDto.getCategory())
                .content(informationReqDto.getContent())
                .title(informationReqDto.getTitle())
                .informationId(information.getInformationId())
                .views(0L)
                .createdAt(information.getCreatedAt())
                .userName(user.get().getName())
                .build();

        return ResponseEntity.ok(informationResDto);
    }

    public ResponseEntity<?> uploadFile(MultipartFile file) {
        try{
            String url = awsS3Service.upload(file);
            return ResponseEntity.ok(url);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("잘못된 파일 형식");
        }
    }

    public ResponseEntity<?> deleteFile(String url) {
        String key = extractKeyFromUrl(url);
        awsS3Service.delete(key);
        return ResponseEntity.ok().body("파일 삭제 완료");
    }

    public ResponseEntity<?> getInformationList(String token, int page, int size,
                                                String category, String sort) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        Sort sortOptions;
        switch (sort) {
            case "views":
                sortOptions = Sort.by(Sort.Direction.DESC, "views");
                break;
            default:
                sortOptions = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
        }
        Pageable pageable = PageRequest.of(page,size,sortOptions);
        Page<Information> informationList = informationRepository.findByCategory(category, pageable);

        long total = informationList.getTotalElements();

        List<InformationResDto> informationResDtoList = informationToDto(informationList, user.get());

        return ResponseEntity.ok(Map.of(
                "informationList", informationResDtoList,
                "total", total
        ));
    }

    public ResponseEntity<?> getInformation(String token, long informationId) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }
        String language = user.get().getLanguage();

        TranslatedInformation translatedInformation = translatedInformationRepository
                .findByInformation_InformationIdAndLanguage(informationId, language);

        long state = bookmarkRepository.countByInformation_InformationIdAndUser_UserId
                (translatedInformation.getInformation().getInformationId(), user.get().getUserId());


        translatedInformation.getInformation().setViews(
                translatedInformation.getInformation().getViews() + 1
        );

        informationRepository.save(translatedInformation.getInformation());

        InformationResDto informationResDto = InformationResDto.builder()
                .category(translatedInformation.getInformation().getCategory())
                .informationId(translatedInformation.getInformation().getInformationId())
                .views(translatedInformation.getInformation().getViews())
                .title(translatedInformation.getTitle())
                .content(translatedInformation.getContent())
                .userName(translatedInformation.getInformation().getUser().getName())
                .createdAt(translatedInformation.getInformation().getCreatedAt())
                .isState(state)
                .build();

        return ResponseEntity.ok(informationResDto);
    }

    @Transactional
    public ResponseEntity<?> deleteInformation(String token, long informationId) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(!user.get().getRole().equals("ROLE_ADMIN")){
            return ResponseEntity.badRequest().body("관리자만 삭제 가능");
        }

        List<InformationFile> informationFileList = informationFileRepository
                .findByInformation_InformationId(informationId);

        for(InformationFile informationFile : informationFileList){
            String key = extractKeyFromUrl(informationFile.getUrl());
            awsS3Service.delete(key);
            informationFileRepository.delete(informationFile);
        }

        informationRepository.deleteById(informationId);

        return ResponseEntity.ok().body("삭제 완료");
    }

    @Transactional
    public ResponseEntity<?> bookmarking(String token, long informationId) {
        Optional<User> user =verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }
        Information information = informationRepository.findById(informationId).get();

        Bookmark bookmark = bookmarkRepository.findByInformation_InformationIdAndUser_UserId(
                informationId, user.get().getUserId()
        );

        if(bookmark == null) {
            bookmark = Bookmark.builder()
                    .information(information)
                    .user(user.get())
                    .build();

            bookmarkRepository.save(bookmark);
            return ResponseEntity.ok().body("북마크 등록");
        }
        bookmarkRepository.delete(bookmark);
        return ResponseEntity.ok().body("북마크 삭제");
    }

    @Transactional
    public ResponseEntity<?> updateInformation(String token, long informationId, InformationReqDto informationReqDto) throws JsonProcessingException {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(!user.get().getRole().equals("ROLE_ADMIN")) {
            return ResponseEntity.badRequest().body("관리자만 작성 가능");
        }

        Information information = informationRepository.findById(informationId).orElse(null);

        if(information == null) {
            return ResponseEntity.badRequest().body("잘못된 게시글");
        }

        List<InformationFile> existFileList = informationFileRepository // 기존 파일 꺼내옴
                .findByInformation_InformationId(informationId);

        List<String> usingFileList = informationReqDto.getFiles(); // 사용된 파일 꺼내옴

        if(usingFileList == null) {
            usingFileList = new ArrayList<>();
        }

        Set<String> existUrls = new HashSet<>();

        for(InformationFile existFile : existFileList){
            if(!usingFileList.contains(existFile.getUrl())){ // 사용중인 파일에 기존 파일에 없을 시 딜리트
                String key = extractKeyFromUrl(existFile.getUrl());
                awsS3Service.delete(key);
                informationFileRepository.delete(existFile);
            }
            existUrls.add(existFile.getUrl());
        }

        for(String usingFile : usingFileList){
            if(!existUrls.contains(usingFile)){ // 기존파일이 사용된 파일을 포함하지 않으면 사용된 파일 저장
                InformationFile informationFile = InformationFile.builder()
                        .information(information)
                        .url(usingFile)
                        .build();
                informationFileRepository.save(informationFile);
            }
        }

        translationQueue.enqueue(new TranslationJob(information, informationReqDto, informationId));

        InformationResDto informationResDto = InformationResDto.builder()
                .content(informationReqDto.getContent())
                .title(informationReqDto.getTitle())
                .build();

        return ResponseEntity.ok(informationResDto);
    }

    public ResponseEntity<?> searchInformation(String token, String keyword, int page, int size, String category, String sort) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        Sort sortOption;
        switch(sort) {
            case "views":
                sortOption = Sort.by(Sort.Direction.DESC, "information.views");
                break;
            default:
                sortOption = Sort.by(Sort.Direction.DESC, "information.createdAt");
                break;
        }

        String language = user.get().getLanguage();
        Pageable pageable = PageRequest.of(page, size, sortOption);

        Page<TranslatedInformation> translatedInformationList = translatedInformationRepository
                .findByLanguageAndCategoryAndTitle(language, category, keyword, pageable);

        long total = translatedInformationList.getTotalElements();

        List<InformationResDto> informationList = transInfoToDto(translatedInformationList.getContent(), user.get());

        return ResponseEntity.ok(Map.of(
                "informationList", informationList,
                "total", total
        ));
    }

    public ResponseEntity<?> getBookmarking(String token, long userId, int page, int size) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Information> informationList = informationRepository.findByBookmarkingAndUser_UserId(userId, pageable);

        long total = informationList.getTotalElements();

        List<InformationResDto> informationResDtoList = informationToDto(informationList, user.get());

        return ResponseEntity.ok(Map.of(
                "informationList", informationResDtoList,
                "total", total
        ));
    }

    public ResponseEntity<?> recommendInfo(String token) {
        Optional<User> user = verifyToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        long userId = user.get().getUserId();

        String url = aiUrl + "/user/" + userId + "/preferences";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if(!response.getStatusCode().is2xxSuccessful()){
            return ResponseEntity.status(response.getStatusCode()).body("유저 선호도 불러오기 실패");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        try{
            rootNode = objectMapper.readTree(response.getBody());
        }catch(JsonProcessingException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("선호도 불러오기 실패");
        }

        JsonNode infoPreferences = rootNode.path("info_preferences");
        if(infoPreferences.isMissingNode()){
            return ResponseEntity.badRequest().body("info_preferences 항목이 존재하지 않음");
        }

        Map<String, Double> preferencesMap = new HashMap<>();
        infoPreferences.fields().forEachRemaining(field -> {
            preferencesMap.put(field.getKey(), field.getValue().doubleValue());
        });

        List<Map.Entry<String, Double>> sortedTag = preferencesMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).toList();

        Map.Entry<String, Double> bestTag = sortedTag.get(0);
        Map.Entry<String, Double> secondTag = sortedTag.get(1);
        Map.Entry<String, Double> thirdTag = sortedTag.get(2);

        String language = user.get().getLanguage();
        String sevenDaysAgo = LocalDate.now().minusDays(7).toString();

        List<List<InformationResDto>> informationResDtoList = new ArrayList<>();

        if(bestTag.getValue() >= 0.9){
            List<TranslatedInformation> infoList = translatedInformationRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            bestTag.getKey(), language, sevenDaysAgo, 2
                    );

            List<InformationResDto> informationDto = transInfoToDto(infoList, user.get());
            informationResDtoList.add(informationDto);

            infoList = translatedInformationRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            secondTag.getKey(), language, sevenDaysAgo, 1
                    );

            informationDto = transInfoToDto(infoList, user.get());
            informationResDtoList.add(informationDto);
        }
        else{
            List<TranslatedInformation> infoList = translatedInformationRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            bestTag.getKey(), language, sevenDaysAgo, 1
                    );

            List<InformationResDto> informationDto = transInfoToDto(infoList, user.get());
            informationResDtoList.add(informationDto);

            infoList = translatedInformationRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            secondTag.getKey(), language, sevenDaysAgo, 1
                    );

            informationDto = transInfoToDto(infoList, user.get());
            informationResDtoList.add(informationDto);

            infoList = translatedInformationRepository
                    .findRandomByTagAndLanguageAndRecentDayAndCount(
                            thirdTag.getKey(), language, sevenDaysAgo, 1
                    );

            informationDto = transInfoToDto(infoList, user.get());
            informationResDtoList.add(informationDto);
        }
        return ResponseEntity.ok(Map.of(
                "informationList", informationResDtoList,
                "analysis", preferencesMap
        ));
    }
}
