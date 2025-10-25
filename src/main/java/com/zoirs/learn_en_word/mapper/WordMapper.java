package com.zoirs.learn_en_word.mapper;

import com.zoirs.learn_en_word.api.dto.skyeng.*;
import com.zoirs.learn_en_word.model.*;

import java.util.List;

public interface WordMapper {

    MeaningEntity toEntity(Meaning dto, Integer id);

    TranslationEntity toEntity(Translation dto, MeaningEntity id);
    
    ImageEntity toEntity(Image dto, MeaningEntity id);
    
    DefinitionEntity toEntity(Definition dto, MeaningEntity id);
    
    ExampleEntity toEntity(Example dto, MeaningEntity id);
    
    Meaning toDto(MeaningEntity entity);

    Translation toDto(TranslationEntity entity);
    
    Example toDto(ExampleEntity entity);
    
    List<Meaning> toMeaningDtoList(List<MeaningEntity> entities);
}
