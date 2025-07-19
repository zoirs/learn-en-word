package com.zoirs.learn_en_word.mapper;

import com.zoirs.learn_en_word.api.dto.skyeng.*;
import com.zoirs.learn_en_word.model.*;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
//@Mapper(componentModel = "spring")
public class WordMapperImpl implements WordMapper {


    @Override
    public TranslationEntity toEntity(Translation dto, MeaningEntity id) {
        if ( dto == null && id == null ) {
            return null;
        }

        TranslationEntity translationEntity = new TranslationEntity();

        if ( dto != null ) {
            translationEntity.setText( dto.getText() );
            translationEntity.setNote( dto.getNote() );
        }
        translationEntity.setMeaning( id );

        return translationEntity;
    }

    @Override
    public ImageEntity toEntity(Image dto, MeaningEntity id) {
        if ( dto == null && id == null ) {
            return null;
        }

        ImageEntity imageEntity = new ImageEntity();

        if ( dto != null ) {
            imageEntity.setUrl( dto.getUrl() );
        }
        imageEntity.setMeaning(id);
//        if ( id != null ) {
//            imageEntity.setId( id );
//        }

        return imageEntity;
    }

    @Override
    public DefinitionEntity toEntity(Definition dto, MeaningEntity id) {
        if ( dto == null && id == null ) {
            return null;
        }

        DefinitionEntity definitionEntity = new DefinitionEntity();
        definitionEntity.setMeaning(id);
        if ( dto != null ) {
            definitionEntity.setText( dto.getText() );
            definitionEntity.setSoundUrl( dto.getSoundUrl() );
        }

        return definitionEntity;
    }

    @Override
    public ExampleEntity toEntity(Example dto, MeaningEntity id) {
        if ( dto == null && id == null ) {
            return null;
        }

        ExampleEntity exampleEntity = new ExampleEntity();
        exampleEntity.setMeaning(id);
//        exampleEntity.setMeaning();ExternalId(id);
        if ( dto != null ) {
            exampleEntity.setText( dto.getText() );
            exampleEntity.setSoundUrl( dto.getSoundUrl() );
        }

        return exampleEntity;
    }

    @Override
    public Word toDto(WordEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Word word = new Word();

        if ( entity.getId() != null ) {
            word.setId( entity.getId().intValue() );
        }
        word.setText( entity.getText() );
        List<Meaning> m=new ArrayList<>();
        for (MeaningEntity meaningEntity : entity.getMeaningEntities()) {
            m.add(toDto(meaningEntity));
        }
        word.setMeanings(m);

        return word;
    }

    @Override
    public Meaning toDto(MeaningEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Meaning meaning = new Meaning();

        if ( entity.getId() != null ) {
            meaning.setId( entity.getId().intValue() );
        }
        meaning.setWordId( entity.getWordId() );
        meaning.setDifficultyLevel( entity.getDifficultyLevel() );
        meaning.setPartOfSpeechCode( entity.getPartOfSpeechCode() );
        meaning.setPrefix( entity.getPrefix() );
        meaning.setText( entity.getText() );
        meaning.setSoundUrl( entity.getSoundUrl() );
        meaning.setTranscription( entity.getTranscription() );
        if ( entity.getUpdatedAt() != null ) {
            meaning.setUpdatedAt( DateTimeFormatter.ISO_LOCAL_DATE_TIME.format( entity.getUpdatedAt() ) );
        }
        meaning.setMnemonics( entity.getMnemonics() );
        meaning.setTranslation(toDto(entity.getTranslationEntity()));

        return meaning;
    }

    @Override
    public Translation toDto(TranslationEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Translation translation = new Translation();

        translation.setText( entity.getText() );
        translation.setNote( entity.getNote() );

        return translation;
    }

    @Override
    public Image toDto(ImageEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Image image = new Image();

        image.setUrl( entity.getUrl() );

        return image;
    }

    @Override
    public Definition toDto(DefinitionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Definition definition = new Definition();

        definition.setText( entity.getText() );
        definition.setSoundUrl( entity.getSoundUrl() );

        return definition;
    }

    @Override
    public Example toDto(ExampleEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Example example = new Example();

        example.setText( entity.getText() );
        example.setSoundUrl( entity.getSoundUrl() );

        return example;
    }

    @Override
    public WordEntity toEntity(com.zoirs.learn_en_word.api.dto.skyeng.Word dto) {
        if (dto == null) {
            return null;
        }

        WordEntity wordEntity = new WordEntity();
        wordEntity.setExternalId(dto.getId());
        wordEntity.setText(dto.getText());

        if (dto.getMeanings() != null) {
            dto.getMeanings().stream()
                    .map(dto1 -> toEntity(dto1, dto.getId()))
                    .forEach(wordEntity::addMeaning);
        }

        return wordEntity;
    }

    @Override
    public MeaningEntity toEntity(Meaning dto, Integer id) {
        if (dto == null) {
            return null;
        }

        MeaningEntity meaning = new MeaningEntity();

        meaning.setExternalId(dto.getId());
        meaning.setWordId((dto.getWordId() != null) ? dto.getWordId() : id);
        meaning.setDifficultyLevel(dto.getDifficultyLevel());
        meaning.setPartOfSpeechCode(dto.getPartOfSpeechCode());
        meaning.setPrefix(dto.getPrefix());
        meaning.setText(dto.getText());
        meaning.setSoundUrl(dto.getSoundUrl());
        meaning.setTranscription(dto.getTranscription());
        meaning.setMnemonics(dto.getMnemonics());

        if (dto.getTranslation() != null) {
            TranslationEntity translation = toEntity(dto.getTranslation(), meaning);
            meaning.setTranslation(translation);
        }

        if (dto.getImages() != null) {
            for (Image dto1 : dto.getImages()) {
                ImageEntity entity = toEntity(dto1, meaning);
                meaning.addImage(entity);
            }
        }

        if (dto.getDefinition() != null) {
            DefinitionEntity definition = toEntity(dto.getDefinition(), meaning);
            meaning.setDefinition(definition);
        }

        if (dto.getExamples() != null) {
            dto.getExamples().stream()
                    .map(dto1 -> toEntity(dto1, meaning))
                    .forEach(meaning::addExample);
        }

        return meaning;
    }

    @AfterMapping
    protected void linkMeaningToWord(com.zoirs.learn_en_word.api.dto.skyeng.Meaning dto, @MappingTarget Meaning entity) {
        if (entity.getTranslation() != null) {
//            entity.getTranslation().setMeaning(entity);
        }
        if (entity.getImages() != null) {
//            entity.getImages().forEach(image -> image.setMeaning(entity));
        }
        if (entity.getDefinition() != null) {
//            entity.getDefinition().setMeaning(entity);
        }
        if (entity.getExamples() != null) {
//            entity.getExamples().forEach(example -> example.setMeaning(entity));
        }
    }

    @Override
    public List<com.zoirs.learn_en_word.api.dto.skyeng.Word> toDtoList(List<WordEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<com.zoirs.learn_en_word.api.dto.skyeng.Meaning> toMeaningDtoList(List<MeaningEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void updateWordFromDto(com.zoirs.learn_en_word.api.dto.skyeng.Word dto, WordEntity entity) {
        if (dto == null) {
            return;
        }
        entity.setText(dto.getText());
        // Handle updates to meanings if needed
    }

    @Override
    public void updateMeaningFromDto(com.zoirs.learn_en_word.api.dto.skyeng.Meaning dto, MeaningEntity entity) {
        if (dto == null) {
            return;
        }
        entity.setWordId(dto.getWordId());
        entity.setDifficultyLevel(dto.getDifficultyLevel());
        entity.setPartOfSpeechCode(dto.getPartOfSpeechCode());
        entity.setPrefix(dto.getPrefix());
        entity.setText(dto.getText());
        entity.setSoundUrl(dto.getSoundUrl());
        entity.setTranscription(dto.getTranscription());
        entity.setMnemonics(dto.getMnemonics());
        // Handle updates to nested objects if needed
    }
}
