package com.notificationengine.service;

import com.notificationengine.config.NotificationEngineHealthIndicator;
import com.notificationengine.dto.EventPayload;
import com.notificationengine.entity.DeadLetterQueueEntry;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEvent;
import com.notificationengine.provider.DeliveryProviderService;
import com.notificationengine.repository.NotificationEventRepository;
import com.notificationengine.service.impl.NotificationEventServiceImpl;
import com.notificationengine.util.NotificationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionHardeningTest {

    @Mock
    private NotificationEventRepository eventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private DeliveryProviderService providerService;

    @Mock
    private DlqService dlqService;

    @InjectMocks
    private NotificationEventServiceImpl eventService;

    // -------------------------------------------------------------------------
    // 1. PII / Sensitive Recipient Masking
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("NotificationUtil masks sensitive email addresses properly")
    void testMaskEmail() {
        String email = "john.doe@enterprise.com";
        String masked = NotificationUtil.maskRecipient(email, ChannelType.EMAIL);
        assertNotEquals(email, masked);
        assertTrue(masked.contains("***@enterprise.com"));
    }

    @Test
    @DisplayName("NotificationUtil masks sensitive phone numbers properly")
    void testMaskPhoneNumber() {
        String phone = "+12025550199";
        String masked = NotificationUtil.maskRecipient(phone, ChannelType.SMS);
        assertNotEquals(phone, masked);
        assertTrue(masked.startsWith("***"));
        assertTrue(masked.endsWith("0199"));
    }

    // -------------------------------------------------------------------------
    // 2. Idempotency & Duplicate Handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ingestEvent returns existing record when duplicate correlationId is received")
    void testIngestEventIdempotency() {
        String correlationId = "CORR-IDEMPOTENT-001";
        EventPayload payload = EventPayload.builder()
                .eventType("TXNX-001")
                .userId("usr_sarah_99")
                .correlationId(correlationId)
                .build();

        NotificationEvent existing = NotificationEvent.builder()
                .id(UUID.randomUUID())
                .eventType("TXNX-001")
                .userId("usr_sarah_99")
                .correlationId(correlationId)
                .processed(true)
                .build();

        when(eventRepository.findByCorrelationId(correlationId)).thenReturn(Optional.of(existing));

        NotificationEvent result = eventService.ingestEvent(payload);

        assertNotNull(result);
        assertEquals(existing.getId(), result.getId());
        assertEquals(correlationId, result.getCorrelationId());
        // Verify save was NOT called for duplicate event
        verify(eventRepository, never()).save(any(NotificationEvent.class));
    }

    // -------------------------------------------------------------------------
    // 3. Restart Recovery of In-Flight Unprocessed Events
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("recoverUnprocessedEvents identifies and re-triggers stale unprocessed events")
    void testRecoverUnprocessedEvents() {
        NotificationEvent staleEvent = NotificationEvent.builder()
                .id(UUID.randomUUID())
                .eventType("TXNX-001")
                .userId("usr_123")
                .correlationId("CORR-STALE-1")
                .processed(false)
                .payload("{\"eventType\":\"TXNX-001\",\"userId\":\"usr_123\"}")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(eventRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(staleEvent));
        when(eventRepository.findById(staleEvent.getId())).thenReturn(Optional.of(staleEvent));

        int recovered = eventService.recoverUnprocessedEvents();

        assertEquals(1, recovered);
    }

    // -------------------------------------------------------------------------
    // 4. Actuator Health Indicator
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("HealthIndicator reports UP when providers and DLQ are healthy")
    void testHealthIndicatorUp() {
        NotificationEngineHealthIndicator healthIndicator =
                new NotificationEngineHealthIndicator(providerService, dlqService);

        when(providerService.getAllProviders()).thenReturn(Collections.emptyList());
        when(dlqService.getStats()).thenReturn(null);

        Health health = healthIndicator.health();
        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("environmentMode"));
    }

    // -------------------------------------------------------------------------
    // 5. Concurrent Ingestion & Atomic Unique Constraint
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ingestEvent gracefully recovers when concurrent thread inserts duplicate correlationId")
    void testConcurrentDuplicateCorrelationIdHandled() {
        String correlationId = "CORR-RACE-001";
        EventPayload payload = EventPayload.builder()
                .eventType("TXNX-001")
                .userId("usr_race_1")
                .correlationId(correlationId)
                .build();

        NotificationEvent existing = NotificationEvent.builder()
                .id(UUID.randomUUID())
                .eventType("TXNX-001")
                .userId("usr_race_1")
                .correlationId(correlationId)
                .processed(false)
                .build();

        // Simulate initial check misses, but save throws DataIntegrityViolationException due to concurrent insert
        when(eventRepository.findByCorrelationId(correlationId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));

        when(eventRepository.save(any(NotificationEvent.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key value violates unique constraint"));

        NotificationEvent result = eventService.ingestEvent(payload);

        assertNotNull(result);
        assertEquals(existing.getId(), result.getId());
        assertEquals(correlationId, result.getCorrelationId());
    }

    // -------------------------------------------------------------------------
    // 6. Security Filter — API Key & JWT Validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ApiKeySecurityFilter allows access with valid API key")
    void testApiKeyValidation_Success() throws Exception {
        com.notificationengine.config.ApiKeySecurityFilter filter = new com.notificationengine.config.ApiKeySecurityFilter();
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "securityEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "configuredApiKey", "secret-test-key-12345");
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "jwtSecret", "my-jwt-test-secret-minimum-32-chars-long");

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/dashboard");
        request.addHeader("X-API-Key", "secret-test-key-12345");
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain chain = new org.springframework.mock.web.MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("ApiKeySecurityFilter rejects request with invalid API key")
    void testApiKeyValidation_Invalid_Rejected() throws Exception {
        com.notificationengine.config.ApiKeySecurityFilter filter = new com.notificationengine.config.ApiKeySecurityFilter();
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "securityEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "configuredApiKey", "secret-test-key-12345");
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "jwtSecret", "my-jwt-test-secret-minimum-32-chars-long");

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/dashboard");
        request.addHeader("X-API-Key", "wrong-key");
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain chain = new org.springframework.mock.web.MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("AUTH-401"));
    }

    @Test
    @DisplayName("ApiKeySecurityFilter validates and accepts valid HMAC SHA-256 JWT")
    void testJwtValidation_ValidSignedToken_Success() throws Exception {
        String secret = "my-jwt-test-secret-minimum-32-chars-long";
        com.notificationengine.config.ApiKeySecurityFilter filter = new com.notificationengine.config.ApiKeySecurityFilter();
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "securityEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "configuredApiKey", "secret-test-key-12345");
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "jwtSecret", secret);

        // Construct valid JWT with exp in the future
        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600;
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"usr_1\",\"exp\":" + futureExp + "}").getBytes());
        
        javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
        hmac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        String signature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hmac.doFinal((header + "." + payload).getBytes()));
        String validJwt = header + "." + payload + "." + signature;

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/dashboard");
        request.addHeader("Authorization", "Bearer " + validJwt);
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain chain = new org.springframework.mock.web.MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("ApiKeySecurityFilter rejects forged/tampered JWT")
    void testJwtValidation_ForgedSignature_Rejected() throws Exception {
        String secret = "my-jwt-test-secret-minimum-32-chars-long";
        com.notificationengine.config.ApiKeySecurityFilter filter = new com.notificationengine.config.ApiKeySecurityFilter();
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "securityEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "configuredApiKey", "secret-test-key-12345");
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "jwtSecret", secret);

        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"usr_attacker\"}".getBytes());
        String forgedJwt = header + "." + payload + ".invalidSignatureABC123";

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/dashboard");
        request.addHeader("Authorization", "Bearer " + forgedJwt);
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain chain = new org.springframework.mock.web.MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("ApiKeySecurityFilter rejects expired JWT token")
    void testJwtValidation_ExpiredToken_Rejected() throws Exception {
        String secret = "my-jwt-test-secret-minimum-32-chars-long";
        com.notificationengine.config.ApiKeySecurityFilter filter = new com.notificationengine.config.ApiKeySecurityFilter();
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "securityEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "configuredApiKey", "secret-test-key-12345");
        org.springframework.test.util.ReflectionTestUtils.setField(filter, "jwtSecret", secret);

        // Expired in past
        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        long pastExp = (System.currentTimeMillis() / 1000L) - 3600;
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"usr_1\",\"exp\":" + pastExp + "}").getBytes());
        
        javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
        hmac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        String signature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hmac.doFinal((header + "." + payload).getBytes()));
        String expiredJwt = header + "." + payload + "." + signature;

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/dashboard");
        request.addHeader("Authorization", "Bearer " + expiredJwt);
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.springframework.mock.web.MockFilterChain chain = new org.springframework.mock.web.MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
    }
}
