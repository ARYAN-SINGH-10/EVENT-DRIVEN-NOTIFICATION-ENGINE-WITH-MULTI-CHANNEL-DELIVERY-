package com.notificationengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response returned upon successful ingestion of a financial event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned upon successful event ingestion")
public class EventIngestionResponse {

    @Schema(description = "The generated unique ID of the ingested event")
    private UUID eventId;

    @Schema(description = "The correlation ID used for distributed tracing")
    private String correlationId;

    @Schema(description = "The type of the ingested event")
    private String eventType;

    @Schema(description = "Ingestion processing status", example = "ACCEPTED")
    private String status;

    @Schema(description = "Human-readable ingestion status message")
    private String message;
}
