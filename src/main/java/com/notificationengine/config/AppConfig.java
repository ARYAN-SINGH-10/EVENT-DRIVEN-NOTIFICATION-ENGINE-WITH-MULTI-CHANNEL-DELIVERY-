package com.notificationengine.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;

/**
 * General application beans configuration.
 *
 * <p>Provides shared infrastructure beans: ObjectMapper, RestTemplate, CORS, etc.</p>
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${app.channel.webhook.timeout-ms:10000}")
    private int webhookTimeoutMs;

    // -----------------------------------------------------------------------
    // Jackson / ObjectMapper
    // -----------------------------------------------------------------------

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                .featuresToEnable(
                        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT
                )
                .build();
    }

    // -----------------------------------------------------------------------
    // RestTemplate (for webhook and external HTTP channel calls)
    // -----------------------------------------------------------------------

    @Bean
    public RestTemplate restTemplate() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) webhookTimeoutMs);
        factory.setReadTimeout((int) webhookTimeoutMs);
        return new RestTemplate(factory);
    }

    // -----------------------------------------------------------------------
    // CORS
    // -----------------------------------------------------------------------

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Total-Count", "X-Page-Number", "X-Page-Size")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
