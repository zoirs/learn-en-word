package com.zoirs.learn_en_word.mapper;

import com.zoirs.learn_en_word.api.dto.skyeng.*;
import com.zoirs.learn_en_word.model.*;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class WordMapperImpl implements WordMapper {

    @Override
    public WordEntity toEntity(com.zoirs.learn_en_word.api.dto.skyeng.Word dto) {
        if (dto == null) {
            return null;
        }

        WordEntity wordEntity = new WordEntity();
        wordEntity.setText(dto.getText());
        wordEntity.setId(Long.valueOf(dto.getId()));

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
        meaning.setId(Long.valueOf(dto.getId()));
        meaning.setDifficultyLevel(dto.getDifficultyLevel());
        meaning.setPartOfSpeechCode(dto.getPartOfSpeechCode());
        meaning.setPrefix(dto.getPrefix());
        meaning.setText(dto.getText());
        meaning.setSoundUrl(dto.getSoundUrl());
        meaning.setTranscription(dto.getTranscription());
        meaning.setMnemonics(dto.getMnemonics());

        if (dto.getTranslation() != null) {
            TranslationEntity translation = toEntity(dto.getTranslation());
            meaning.setTranslation(translation);
        }

        if (dto.getImages() != null) {
            dto.getImages().stream()
                    .map(this::toEntity)
                    .forEach(meaning::addImage);
        }

        if (dto.getDefinition() != null) {
            DefinitionEntity definition = toEntity(dto.getDefinition());
            meaning.setDefinition(definition);
        }

        if (dto.getExamples() != null) {
            dto.getExamples().stream()
                    .map(this::toEntity)
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
