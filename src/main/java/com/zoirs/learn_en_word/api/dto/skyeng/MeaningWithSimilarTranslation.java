package com.zoirs.learn_en_word.api.dto.skyeng;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeaningWithSimilarTranslation {
    @JsonProperty("meaningId")
    private Integer meaningId;
    @JsonProperty("frequencyPercent")
    private String frequencyPercent;
    @JsonProperty("partOfSpeechAbbreviation")
    private String partOfSpeechAbbreviation;
    private Translation translation;
}
