package com.zoirs.learn_en_word.controller;

import lombok.Data;
import java.util.Set;

@Data
public class State {
    private String userId;
    private Set<Integer> knownWords;
    private Set<Integer> learningWords;
    private Set<String> partOfSpeechCodes = Set.of("j", "n", "r", "v");
}
