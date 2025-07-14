package com.zoirs.learn_en_word.mapper;

import com.zoirs.learn_en_word.api.dto.skyeng.Word;
import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.Translation;
import com.zoirs.learn_en_word.api.dto.skyeng.Image;
import com.zoirs.learn_en_word.api.dto.skyeng.Definition;
import com.zoirs.learn_en_word.api.dto.skyeng.Example;
import com.zoirs.learn_en_word.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

//@Mapper(componentModel = "spring")
public interface WordMapper {
    
    WordMapper INSTANCE = Mappers.getMapper(WordMapper.class);
    
    // DTO to Entity mappings
    WordEntity toEntity(Word dto);
    
    //@Mapping(target = "word", ignore = true)
    MeaningEntity toEntity(Meaning dto, Integer id);
    
    //@Mapping(target = "meaning", ignore = true)
    TranslationEntity toEntity(Translation dto);
    
    //@Mapping(target = "meaning", ignore = true)
    ImageEntity toEntity(Image dto);
    
    //@Mapping(target = "meaning", ignore = true)
    DefinitionEntity toEntity(Definition dto);
    
    //@Mapping(target = "meaning", ignore = true)
    ExampleEntity toEntity(Example dto);
    
    // Entity to DTO mappings
    Word toDto(WordEntity entity);
    
    Meaning toDto(MeaningEntity entity);
    
    Translation toDto(TranslationEntity entity);
    
    Image toDto(ImageEntity entity);
    
    Definition toDto(DefinitionEntity entity);
    
    Example toDto(ExampleEntity entity);
    
    // List mappings
    List<Word> toDtoList(List<WordEntity> entities);
    
    List<Meaning> toMeaningDtoList(List<MeaningEntity> entities);
    
    // Update entity from DTO
    void updateWordFromDto(Word dto, @MappingTarget WordEntity entity);
    
    void updateMeaningFromDto(Meaning dto, @MappingTarget MeaningEntity entity);
}
