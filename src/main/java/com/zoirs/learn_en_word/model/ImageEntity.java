package com.zoirs.learn_en_word.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "images")
@Getter
@Setter
public class ImageEntity extends BaseEntity {
    
    private String url;

    @Column(name = "external_id")
    private Integer externalId;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meaning_id")
    private MeaningEntity meaning;

}
