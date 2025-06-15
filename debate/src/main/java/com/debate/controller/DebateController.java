package com.debate.controller;

import com.debate.dto.DebateReqDto;
import com.debate.service.DebateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/debate")
public class DebateController {
    private final DebateService debateService;

    @PostMapping
    public ResponseEntity<?> write(@RequestBody DebateReqDto debateReqDto) {
        return debateService.write(debateReqDto);
    }

    @GetMapping
    public ResponseEntity<?> getDebates(@RequestHeader("Authorization") String token,
                                       int page, int size, String sort ,String category) {
        return debateService.getDebates(token, page, size, sort, category);
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayDebate(@RequestHeader("Authorization") String token) {
        return debateService.getTodayDebate(token);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDebate(@RequestHeader("Authorization") String token,
                                          int page, int size, String sort, String category,
                                          String keyword, String searchBy) {
        return debateService.searchDebate(token, page, size, sort, category, keyword, searchBy);
    }

    @PostMapping("/emotion/{debateId}")
    public ResponseEntity<?> reactToDebate(@RequestHeader("Authorization") String token,
                                           @PathVariable long debateId,
                                           @RequestBody DebateReqDto debateReqDto) {
        return debateService.reactToDebate(token, debateId, debateReqDto);
    }

    @GetMapping("/{debateId}")
    public ResponseEntity<?> getDebate(@RequestHeader("Authorization") String token,
                                        @PathVariable long debateId) {
        return debateService.getDebate(token, debateId);
    }

    @GetMapping("/voted")
    public ResponseEntity<?> getVotedDebate(@RequestHeader("Authorization") String token,
                                            long userId, int page, int size){
        return debateService.getVotedDebate(token, userId, page, size);
    }

    @GetMapping("/recommendation")
    public ResponseEntity<?> recommendDebate(@RequestHeader("Authorization") String token){
        return debateService.recommendDebate(token);
    }
}
