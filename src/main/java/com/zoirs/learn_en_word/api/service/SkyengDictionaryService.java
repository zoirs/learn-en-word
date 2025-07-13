package com.zoirs.learn_en_word.api.service;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.Word;

import java.util.List;

public interface SkyengDictionaryService {
    /**
     * Search for words in the Skyeng dictionary
     * @param search Search query (word or translation)
     * @return List of words with their meanings
     */
    List<Word> searchWords(String search);
    
    /**
     * Get detailed information about specific meanings
     * @param ids Comma-separated list of meaning IDs
     * @return List of detailed meaning objects
     */
    List<Meaning> getMeanings(String ids);
    
    /**
     * Get meanings that were updated after the specified date
     * @param updatedAt Date in ISO format (UTC)
     * @return List of updated meanings
     */
    List<Meaning> getUpdatedMeanings(String updatedAt);
}
