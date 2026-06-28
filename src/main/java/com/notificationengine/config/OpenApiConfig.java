package com.notificationengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event-Driven Notification Engine API")
                        .description("Centralized multi-channel notification engine with features like preferences management, frequency capping, quiet hours, dead-letter queuing, and extensive analytics.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@notificationengine.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Environment")
                ));
    }
}
