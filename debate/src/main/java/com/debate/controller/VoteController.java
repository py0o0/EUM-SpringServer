package com.debate.controller;

import com.debate.dto.VoteReqDto;
import com.debate.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/debate/vote")
public class VoteController {
    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<?> reactToVote(@RequestHeader("Authorization") String token,
                                         @RequestBody VoteReqDto voteReqDto) {
        return voteService.reactToVote(token, voteReqDto);
    }

    @GetMapping("/{debateId}")
    public ResponseEntity<?> getVotes(@PathVariable Long debateId) {
        return voteService.getVotes(debateId);
    }
}
