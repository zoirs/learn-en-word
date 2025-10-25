package com.zoirs.learn_en_word.mapper;

import com.zoirs.learn_en_word.api.dto.skyeng.*;
import com.zoirs.learn_en_word.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class WordMapperImpl implements WordMapper {

    private static final Logger log = LoggerFactory.getLogger(WordMapperImpl.class);

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
    public Meaning toDto(MeaningEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Meaning meaning = new Meaning();

//        if ( entity.getId() != null ) {
//            meaning.setId( entity.getId().intValue() );
//        }
        meaning.setId(entity.getExternalId());
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
        List<ExampleEntity> exampleEntities = entity.getExampleEntities();
        List<Example> collect = exampleEntities.stream().map(this::toDto).collect(Collectors.toList());
        meaning.setExamples(collect);// todo добавить
        meaning.setFrequencyPercent(entity.getFrequencyPercent());
        return meaning;
    }
    // удалил  in (12697,12698,12699,12700)

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

            List<MeaningWithSimilarTranslation> meaningsWithSimilarTranslation = dto.getMeaningsWithSimilarTranslation();
            if (meaningsWithSimilarTranslation != null && translation.getText() != null) {
                Optional<MeaningWithSimilarTranslation> exist = meaningsWithSimilarTranslation.stream()
                        .filter(q -> dto.getId().equals(q.getMeaningId()))
                        .findFirst();
                meaning.setIsValid(exist.isPresent());
                exist
                        .map(MeaningWithSimilarTranslation::getFrequencyPercent)
                        .ifPresent(s -> {
                            try {
                                int percent = (int) Double.parseDouble(s);
                                meaning.setFrequencyPercent(percent);
                            } catch (Exception e) {
                                log.error("Cant parse FrequencyPercent " + s, e);
                            }
                        });
            }
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

    @Override
    public List<com.zoirs.learn_en_word.api.dto.skyeng.Meaning> toMeaningDtoList(List<MeaningEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

}
