package com.zoirs.learn_en_word.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "examples")
@Getter
@Setter
public class ExampleEntity extends BaseEntity {
    
    private String text;
    
    @Column(name = "sound_url")
    private String soundUrl;
    
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meaning_id")
    private MeaningEntity meaning;

//    @Column(name = "external_id")
//    private Integer externalId;
}
