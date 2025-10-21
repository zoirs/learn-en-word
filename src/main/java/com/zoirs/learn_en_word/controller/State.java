package com.zoirs.learn_en_word.controller;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@Data
public class State {
    private String userId;
    private Set<String> knownWords;
    private Set<String> learningWords;
}
