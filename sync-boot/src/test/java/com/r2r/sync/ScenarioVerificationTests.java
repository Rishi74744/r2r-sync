package com.r2r.sync;

import com.r2r.sync.dto.RecordDTO;
import com.r2r.sync.dto.SyncEventDTO;
import com.r2r.sync.enums.EventStatus;
import com.r2r.sync.events.ConnectorWorker;
import com.r2r.sync.repository.AuditRepository;
import com.r2r.sync.repository.EventRepository;
import com.r2r.sync.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ScenarioVerificationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private com.r2r.sync.lib.service.SyncRateLimiter rateLimiter;

    @Autowired
    private com.r2r.sync.lib.service.BackpressureService backpressureService;

    @Autowired
    private ConnectorWorker connectorWorker;

    @org.junit.jupiter.api.BeforeEach
    public void setup() {
        rateLimiter.reset("connector1");
        backpressureService.release("connector1");
        // Clear repositories to ensure a fresh state for each test
        ((com.r2r.sync.db.impl.InMemoryDBConnection)((com.r2r.sync.repository.impl.EventRepositoryImpl)eventRepository).getDb()).clear("events");
        ((com.r2r.sync.db.impl.InMemoryDBConnection)((com.r2r.sync.repository.impl.AuditRepositoryImpl)auditRepository).getDb()).clear("audits");
    }

    @Test
    public void testSuccessScenario() {
        RecordDTO record = RecordDTO.builder()
                .type("USER")
                .data(Map.of("name", "Success User", "scenario", "success"))
                .build();
        ResponseEntity<RecordDTO> createResponse = restTemplate.postForEntity("/api/records", record, RecordDTO.class);
        String recordId = createResponse.getBody().getId();

        restTemplate.postForEntity("/api/sync/" + recordId + "?connectorId=connector1", null, String.class);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean hasSuccess = eventRepository.findAll().stream()
                    .anyMatch(e -> e.getRecordId().equals(recordId) && e.getStatus() == EventStatus.SUCCESS);
            assertThat(hasSuccess).as("Event for record %s should be SUCCESS", recordId).isTrue();
        });
    }

    @Test
    public void testFailureToManualCheckScenario() {
        // Thresholds in AppConfig: maxRetries=3, maxReconcileAttempts=3
        RecordDTO record = RecordDTO.builder()
                .type("USER")
                .data(Map.of("name", "Hard Failure User", "scenario", "failure"))
                .build();
        ResponseEntity<RecordDTO> createResponse = restTemplate.postForEntity("/api/records", record, RecordDTO.class);
        String recordId = createResponse.getBody().getId();

        restTemplate.postForEntity("/api/sync/" + recordId + "?connectorId=connector1", null, String.class);

        // We need to wait for (maxRetries * maxReconcileAttempts) rounds.
        // Round 1: 1s, 2s, 4s, 8s (15s) -> Reconcile 1
        // Round 2: 1s, 2s, 4s, 8s (15s) -> Reconcile 2
        // Round 3: 1s, 2s, 4s, 8s (15s) -> MANUAL_CHECK
        // Total ~45-60s
        await().atMost(300, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .untilAsserted(() -> {
            String audits = auditRepository.findAll().stream()
                    .filter(a -> {
                        var e = eventRepository.getEvent(a.getEventId());
                        return e != null && recordId.equals(e.getRecordId());
                    })
                    .sorted((a1, a2) -> Long.compare(a1.getTimestamp(), a2.getTimestamp()))
                    .map(a -> a.getStatus() + ":" + a.getAction())
                    .collect(Collectors.joining(", "));
            
            System.out.println("Current audits for " + recordId + ": " + audits);
            
            boolean hasManualCheck = eventRepository.findAll().stream()
                    .anyMatch(e -> e.getRecordId().equals(recordId) && e.getStatus() == EventStatus.MANUAL_CHECK);
            
            assertThat(hasManualCheck).as("Event for record %s should eventually reach MANUAL_CHECK. Current audits: %s", recordId, audits).isTrue();
        });
    }

    @Test
    public void testIdempotencyScenario() {
        RecordDTO record = RecordDTO.builder()
                .type("USER")
                .data(Map.of("name", "Idempotency User", "scenario", "success"))
                .build();
        ResponseEntity<RecordDTO> createResponse = restTemplate.postForEntity("/api/records", record, RecordDTO.class);
        String recordId = createResponse.getBody().getId();

        SyncEventDTO event = SyncEventDTO.builder()
                .eventId(UUID.randomUUID().toString())
                .recordId(recordId)
                .connectorId("connector1")
                .status(EventStatus.PENDING)
                .build();

        // Process same event ID twice
        connectorWorker.process(event);
        connectorWorker.process(event);

        // Check audits for "SKIPPED" and "DUPLICATE"
        boolean hasDuplicateAudit = auditRepository.findAll().stream()
                .anyMatch(a -> event.getEventId().equals(a.getEventId()) && "DUPLICATE".equals(a.getAction()));
        
        assertThat(hasDuplicateAudit).as("Second processing of same event ID should be caught by idempotency check").isTrue();
    }

    @Test
    public void testRateLimitScenario() {
        RecordDTO record = RecordDTO.builder()
                .type("USER")
                .data(Map.of("name", "RateLimit User", "scenario", "rate_limit"))
                .build();
        ResponseEntity<RecordDTO> createResponse = restTemplate.postForEntity("/api/records", record, RecordDTO.class);
        String recordId = createResponse.getBody().getId();

        restTemplate.postForEntity("/api/sync/" + recordId + "?connectorId=connector1", null, String.class);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean hasRetry = eventRepository.findAll().stream()
                    .anyMatch(e -> e.getRecordId().equals(recordId) && e.getStatus() == EventStatus.RETRY);
            assertThat(hasRetry).as("Event for record %s should be in RETRY state after rate limit", recordId).isTrue();
        });
    }
}
