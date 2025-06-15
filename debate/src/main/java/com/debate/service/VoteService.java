package com.debate.service;

import com.debate.dto.VoteReqDto;
import com.debate.dto.VoteResDto;
import com.debate.entity.Debate;
import com.debate.entity.User;
import com.debate.entity.Vote;
import com.debate.repository.DebateRepository;
import com.debate.repository.UserRepository;
import com.debate.repository.VoteRepository;
import util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final DebateRepository debateRepository;

    private final JwtUtil jwtUtil;

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

    private static Map<String, Double> calculateNationPercent(List<Vote> voteList) {
        Map<String, Long> nationCount = new HashMap<>();

        for (Vote v : voteList) {
            String nation = v.getUser().getNation();
            nationCount.put(nation, nationCount.getOrDefault(nation, 0L) + 1);
        }

        long totalVotes = voteList.size();
        Map<String, Double> nationPercent = new HashMap<>();

        for (Map.Entry<String, Long> entry : nationCount.entrySet()) {
            double percent = (double) entry.getValue() * 100 / totalVotes;
            nationPercent.put(entry.getKey(), percent);
        }

        return nationPercent.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @Transactional
    public ResponseEntity<?> reactToVote(String token, VoteReqDto voteReqDto) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(user.get().getBan() == 1){
            return ResponseEntity.badRequest().body("차단된 유저");
        }

        Debate debate = debateRepository.findById(voteReqDto.getDebateId()).get();

        Vote vote = voteRepository.findByDebate_DebateIdAndUser_UserId
                (voteReqDto.getDebateId(), user.get().getUserId());
        if(vote == null){
            vote = Vote.builder()
                    .debate(debate)
                    .user(user.get())
                    .option(voteReqDto.getOption())
                    .build();
            voteRepository.save(vote);

            debate.setVoteCnt(debate.getVoteCnt() + 1);

            if(voteReqDto.getOption().equals("찬성")){
                debate.setAgreeCnt(debate.getAgreeCnt() + 1);
            }
            else{
                debate.setDisagreeCnt(debate.getDisagreeCnt() + 1);
            }
            debateRepository.save(debate);

        }else{
            if(vote.getOption().equals(voteReqDto.getOption())){
                debate.setVoteCnt(debate.getVoteCnt() - 1);
                if(voteReqDto.getOption().equals("찬성")){
                    debate.setAgreeCnt(debate.getAgreeCnt() - 1);
                }
                else{
                    debate.setDisagreeCnt(debate.getDisagreeCnt() - 1);
                }
                debateRepository.save(debate);
                voteRepository.delete(vote);
            }
            else {
                return ResponseEntity.badRequest().body("찬성과 반대 중 택 1");
            }
        }

        Map<String, Double> percentMap =
                calculateVotePercent(debate.getAgreeCnt(), debate.getDisagreeCnt());

        List<Vote> voteList = voteRepository.findByDebate_DebateId(debate.getDebateId());

        Map<String, Double> nationPercent = calculateNationPercent(voteList);

        VoteResDto voteResDto = VoteResDto.builder()
                .voteCnt(voteList.size())
                .agreePercent(percentMap.get("agreePercent"))
                .disagreePercent(percentMap.get("disagreePercent"))
                .nationPercent(nationPercent)
                .build();
        return ResponseEntity.ok(voteResDto);
    }

    public ResponseEntity<?> getVotes(long debateId) {
        Debate debate = debateRepository.findById(debateId).get();

        List<Vote> voteList = voteRepository.findByDebate_DebateId(debateId);

        Map<String, Double> percentMap =
                calculateVotePercent(debate.getAgreeCnt(), debate.getDisagreeCnt());

        Map<String, Double> nationPercent = calculateNationPercent(voteList);

        VoteResDto voteResDto = VoteResDto.builder()
                .voteCnt(voteList.size())
                .agreePercent(percentMap.get("agreePercent"))
                .disagreePercent(percentMap.get("disagreePercent"))
                .nationPercent(nationPercent)
                .build();
        return ResponseEntity.ok(voteResDto);
    }
}
