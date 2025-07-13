package com.zoirs.learn_en_word.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class TestConfig {
    
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
