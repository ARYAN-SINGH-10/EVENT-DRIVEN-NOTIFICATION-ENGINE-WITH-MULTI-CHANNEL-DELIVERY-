package com.notificationengine;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Event-Driven Notification Engine.
 *
 * <p>This application provides an enterprise-grade, multi-channel notification
 * delivery platform with event-driven architecture, Kafka integration (simulation-ready),
 * Redis caching, DLQ support, compliance management, and rich analytics.</p>
 *
 * <p>Swagger UI: <a href="http://localhost:8080/api/swagger-ui.html">/api/swagger-ui.html</a></p>
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableJpaAuditing
@OpenAPIDefinition(
        info = @Info(
                title = "Event-Driven Notification Engine API",
                version = "1.0.0",
                description = """
                        Enterprise-grade notification engine supporting multi-channel delivery
                        (Email, SMS, Push, Slack, Webhook) with event-driven architecture,
                        Kafka integration, Redis caching, DLQ retry mechanisms,
                        compliance management and rich analytics.
                        """,
                contact = @Contact(
                        name = "Notification Engine Team",
                        email = "support@notification-engine.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        servers = {
                @Server(url = "/api", description = "Default Server"),
                @Server(url = "http://localhost:8080/api", description = "Local Development")
        }
)
public class NotificationEngineApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NotificationEngineApplication.class);
        var context = app.run(args);
        log.info("========================================================");
        log.info("  Event-Driven Notification Engine started successfully  ");
        log.info("  Swagger UI : http://localhost:8080/swagger-ui.html     ");
        log.info("  API Docs   : http://localhost:8080/v3/api-docs         ");
        log.info("========================================================");
    }
}
