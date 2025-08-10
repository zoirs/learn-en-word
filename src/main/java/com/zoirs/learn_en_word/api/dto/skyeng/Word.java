package com.zoirs.learn_en_word.api.dto.skyeng;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Word {
    private Integer id;
    private String text;
    private List<MeaningShort> meanings;
}
