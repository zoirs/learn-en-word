package com.zoirs.learn_en_word.req;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.entity.UserWord;
import lombok.Data;

@Data
public class UserWordResponse {
    private final String word;
    private final Meaning meaning;

    public UserWordResponse(UserWord q, Meaning dto) {
        this.word = q.getWord().getText();
        this.meaning = dto;
    }
}
