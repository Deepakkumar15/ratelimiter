package com.example.ratelimiter.configurations.spring;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterAppConfigs {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper instance = new ObjectMapper();
        instance.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        instance.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        instance.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        return instance;
    }
}
