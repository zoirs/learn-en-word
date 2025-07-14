package com.zoirs.learn_en_word.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "words")
@Data
@NoArgsConstructor
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String englishWord;
    
    @Column(nullable = false)
    private String translation;
    
    private String example;
    
    public Word(String englishWord, String translation) {
        this.englishWord = englishWord;
        this.translation = translation;
    }
}
