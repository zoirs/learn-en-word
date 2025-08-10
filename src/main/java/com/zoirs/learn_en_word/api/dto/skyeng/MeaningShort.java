package com.zoirs.learn_en_word.api.dto.skyeng;

import lombok.Data;

@Data
public class MeaningShort {
    private Integer id;
    private String partOfSpeechCode;
    private Translation translation;
    private String previewUrl;
    private String imageUrl;
    private String transcription;
    private String soundUrl;
}
