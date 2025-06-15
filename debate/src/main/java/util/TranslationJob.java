package util;

import com.debate.dto.DebateReqDto;
import com.debate.entity.Debate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TranslationJob {
    private final Object entity;
    private final Object dto;
    private final Long optionalId;
}
