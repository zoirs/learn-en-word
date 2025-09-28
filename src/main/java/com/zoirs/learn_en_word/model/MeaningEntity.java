package com.zoirs.learn_en_word.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meanings")
@Getter
@Setter
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "meaningCache")
public class MeaningEntity extends BaseEntity {

    @Column(name = "external_id", unique = true)
    private Integer externalId;

    @Column(name = "word_id")
    private Integer wordId;
    
    @Column(name = "difficulty_level")
    private Integer difficultyLevel;
    
    @Column(name = "part_of_speech_code")
    private String partOfSpeechCode;
    
    private String prefix;
    private String text;
    
    @Column(name = "sound_url")
    private String soundUrl;
    
    private String transcription;
    private String mnemonics;
    
    @OneToOne(mappedBy = "meaning", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TranslationEntity translationEntity;

    @OneToMany(mappedBy = "meaning", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<ImageEntity> imageEntities = new ArrayList<>();
    
    @OneToOne(mappedBy = "meaning", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DefinitionEntity definitionEntity;
    
    @OneToMany(mappedBy = "meaning", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<ExampleEntity> exampleEntities = new ArrayList<>();

    private Boolean autoloaded;

    public void setTranslation(TranslationEntity translationEntity) {
        if (translationEntity == null) {
//            if (this.translationEntity != null) {
//                this.translationEntity.setMeaning(null);
//            }
        } else {
//            translationEntity.setMeaning(this);
        }
        this.translationEntity = translationEntity;
    }
    
    public void setDefinition(DefinitionEntity definitionEntity) {
//        if (definitionEntity == null) {
//            if (this.definitionEntity != null) {
//                this.definitionEntity.setMeaning(null);
//            }
//        } else {
//            definitionEntity.setMeaning(this);
//        }
        this.definitionEntity = definitionEntity;
    }
    
    public void addImage(ImageEntity imageEntity) {
        imageEntities.add(imageEntity);
//        imageEntity.setMeaning(this);
    }
    
    public void removeImage(ImageEntity imageEntity) {
        imageEntities.remove(imageEntity);
//        imageEntity.setMeaning(null);
    }
    
    public void addExample(ExampleEntity exampleEntity) {
        exampleEntities.add(exampleEntity);
//        exampleEntity.setMeaning(this);
    }
    
    public void removeExample(ExampleEntity exampleEntity) {
        exampleEntities.remove(exampleEntity);
//        exampleEntity.setMeaning(null);
    }
}
