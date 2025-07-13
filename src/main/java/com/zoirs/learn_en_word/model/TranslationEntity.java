package com.zoirs.learn_en_word.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "translations")
@Getter
@Setter
public class TranslationEntity extends BaseEntity {
    
    private String text;
    private String note;
    
    @JsonBackReference
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meaning_id")
    private MeaningEntity meaning;
}
