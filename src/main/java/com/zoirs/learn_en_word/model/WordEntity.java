package com.zoirs.learn_en_word.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "words")
@Getter
@Setter
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "wordCache")
public class WordEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String text;

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<MeaningEntity> meaningEntities = new ArrayList<>();

    public void addMeaning(MeaningEntity meaningEntity) {
        meaningEntities.add(meaningEntity);
        meaningEntity.setWord(this);
    }

    public void removeMeaning(MeaningEntity meaningEntity) {
        meaningEntities.remove(meaningEntity);
        meaningEntity.setWord(null);
    }
}
