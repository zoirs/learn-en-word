package com.zoirs.learn_en_word.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "definitions")
@Getter
@Setter
public class DefinitionEntity extends BaseEntity {
    
    private String text;
    
    @Column(name = "sound_url")
    private String soundUrl;
    
    @JsonBackReference
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meaning_id")
    private MeaningEntity meaning;

    public DefinitionEntity(Integer id) {
        super(Long.valueOf(id));
    }

    private DefinitionEntity() {
    }
}
