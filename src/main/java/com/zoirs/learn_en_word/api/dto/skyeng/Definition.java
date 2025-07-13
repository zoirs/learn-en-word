package com.zoirs.learn_en_word.api.dto.skyeng;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Definition {
    private String text;
    @JsonProperty("soundUrl")
    private String soundUrl;
}
