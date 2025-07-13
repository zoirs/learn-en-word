package com.zoirs.learn_en_word.api.dto.skyeng;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlternativeTranslation {
    private String text;
    private Translation translation;
}
