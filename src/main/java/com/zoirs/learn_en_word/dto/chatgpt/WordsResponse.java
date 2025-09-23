package com.zoirs.learn_en_word.dto.chatgpt;

import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class WordsResponse {
    private List<String> easier;
    private List<String> same;
    private List<String> harder;

    public Set<String> getWords() {
        Set<String> all = new HashSet<>();
        if (easier != null) all.addAll(easier);
        if (same != null) all.addAll(same);
        if (harder != null) all.addAll(harder);
        return all;
    }
}
