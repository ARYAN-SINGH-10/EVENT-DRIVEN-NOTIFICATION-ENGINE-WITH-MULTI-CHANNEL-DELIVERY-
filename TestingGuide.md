# Event-Driven Notification Engine - Testing Guide

This guide details how to run, write, and understand the test suite for the Event-Driven Notification Engine.

## How to Run Tests

The project is built using Maven and JUnit 5. All tests can be executed via the command line.

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test Class
```bash
mvn test -Dtest=FailureScenarioTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=FailureScenarioTest#testProviderFailureAndRetryLogic
```

### Generate Surefire Test Report
```bash
mvn surefire-report:report
```
The HTML report will be generated at `target/site/surefire-report.html`.

## Expected Results
A full successful build should output `BUILD SUCCESS` and show no test failures.
Example:
```
[INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## Unit Tests
Unit tests validate the core logic of individual services, utilizing Mockito to isolate dependencies.

### Key Unit Test Classes:
- **`ChannelRoutingServiceTest`**: Validates routing logic, cost optimization, fallback selection, and preference overrides.
- **`FrequencyCapServiceTest`**: Tests rate-limiting logic, ensuring CRITICAL priority events bypass caps while NORMAL events hit local caches.
- **`QuietHoursServiceTest`**: Checks timezone calculations and determines if an event lands within the quiet window.
- **`DlqServiceTest`**: Verifies Dead Letter Queue pagination, retry mechanisms, and mutation (delete/replay).
- **`AnalyticsServiceTest`**: Asserts correct metric aggregation calculations (CTR, Delivery Rate, Average TTD).

## Integration Tests
Integration tests test the interaction between components but generally use mocked repositories or internal buses.

### Key Integration Test Classes:
- **`NotificationEventServiceImplTest`**: Ensures event payloads properly translate to local domain events, handle priorities, and route properly.
- **`PreferenceResolutionServiceTest`**: Tests the intersection of global preferences and channel-specific preferences.

## Failure Tests
The system's resilience is tested heavily through failure simulation.

### `FailureScenarioTest`
This test suite covers disaster scenarios:
1. **Provider Failure**: Mocks `ChannelProcessor` throwing timeouts. Asserts transition to `RETRYING` and verifies exponential backoff calculation.
2. **Retry Logic**: Ensures `retryCount` increments.
3. **DLQ Routing**: Mocks max retries exhaustion and validates the payload correctly routes to `DeadLetterQueueHandler` in a terminal `DLQ` state.
4. **DND Blocking**: Asserts that `DndComplianceEngine` rejections correctly halt pipelines and save as `DND`.
5. **Frequency Cap Enforcement**: Verifies the state halts at `CAPPED`.
6. **Quiet Hour Queueing**: Mocks quiet hour hits and asserts state is set to `QUIET` with queued scheduling.
7. **State Machine Transitions**: Tracks the full flow of `NotificationEntity.NotificationStatus`.
