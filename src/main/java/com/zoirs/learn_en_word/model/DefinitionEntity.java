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

    @Column(name = "external_id", unique = true)
    private Integer externalId;

    @Column(length = 2000)
    private String text;

    @Column(name = "sound_url", length = 2048)
    private String soundUrl;
    
    @JsonBackReference
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meaning_id")
    private MeaningEntity meaning;
}
