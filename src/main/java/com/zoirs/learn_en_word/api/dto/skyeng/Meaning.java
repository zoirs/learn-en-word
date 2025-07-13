package com.zoirs.learn_en_word.api.dto.skyeng;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Meaning {
    private String id;
    @JsonProperty("wordId")
    private Integer wordId;
    @JsonProperty("difficultyLevel")
    private Integer difficultyLevel;
    @JsonProperty("partOfSpeechCode")
    private String partOfSpeechCode;
    private String prefix;
    private String text;
    @JsonProperty("soundUrl")
    private String soundUrl;
    private String transcription;
    private Properties properties;
    @JsonProperty("updatedAt")
    private String updatedAt;
    private String mnemonics;
    private Translation translation;
    private List<Image> images;
    private Definition definition;
    private List<Example> examples;
    @JsonProperty("meaningsWithSimilarTranslation")
    private List<MeaningWithSimilarTranslation> meaningsWithSimilarTranslation;
    @JsonProperty("alternativeTranslations")
    private List<AlternativeTranslation> alternativeTranslations;
}
