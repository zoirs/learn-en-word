package com.zoirs.learn_en_word.api.service.impl;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.Word;
import com.zoirs.learn_en_word.api.service.SkyengDictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Service
public class SkyengDictionaryServiceImpl implements SkyengDictionaryService {

    private static final String SEARCH_ENDPOINT = "/api/public/v1/words/search";
    private static final String MEANINGS_ENDPOINT = "/api/public/v1/meanings";

    private final RestTemplate restTemplate;
    private final String apiBaseUrl;

    @Autowired
    public SkyengDictionaryServiceImpl(RestTemplate restTemplate,
                                     @Value("${skyeng.api.base-url:https://dictionary.skyeng.ru}") String apiBaseUrl) {
        this.restTemplate = restTemplate;
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public List<Word> searchWords(String search) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + SEARCH_ENDPOINT)
                    .queryParam("search", search)
                    .toUriString();

            ResponseEntity<List<Word>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            // Log the error and return empty list
            // In a production environment, you might want to throw a custom exception
            return Collections.emptyList();
        }
    }

    @Override
    public List<Meaning> getMeanings(String ids) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + MEANINGS_ENDPOINT)
                    .queryParam("ids", ids)
                    .toUriString();

            ResponseEntity<List<Meaning>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            // Log the error and return empty list
            return Collections.emptyList();
        }
    }

    @Override
    public List<Meaning> getUpdatedMeanings(String updatedAt) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + MEANINGS_ENDPOINT)
                    .queryParam("updatedAt", updatedAt)
                    .toUriString();

            ResponseEntity<List<Meaning>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            // Log the error and return empty list
            return Collections.emptyList();
        }
    }
}
