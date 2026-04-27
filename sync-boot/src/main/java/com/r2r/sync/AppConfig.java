package com.r2r.sync;

import com.r2r.sync.db.CacheConnection;
import com.r2r.sync.db.DBConnection;
import com.r2r.sync.db.impl.DBConnector;
import com.r2r.sync.events.*;
import com.r2r.sync.lib.service.*;
import com.r2r.sync.lib.service.impl.*;
import com.r2r.sync.repository.*;
import com.r2r.sync.repository.impl.*;
import com.r2r.sync.entity.ConnectorConfigEntity;
import com.r2r.sync.factory.ConnectorFactory;
import com.r2r.sync.connector.MockConnector;
import com.r2r.sync.service.impl.RecordService;
import com.r2r.sync.service.RetryScheduler;
import com.r2r.sync.service.impl.RetrySchedulerImpl;
import com.r2r.sync.service.impl.SyncService;
import com.r2r.sync.utility.RetryPolicy;
import com.r2r.sync.lib.common.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public DBConnection dbConnection() {
        return DBConnector.createDBConnection("in-memory");
    }

    @Bean
    public CacheConnection cacheConnection() {
        return DBConnector.createCacheConnection("in-memory");
    }

    @Bean
    public EventRepository eventRepository(DBConnection db) {
        return new EventRepositoryImpl(db);
    }

    @Bean
    public RecordRepository recordRepository(DBConnection db) {
        return new RecordRepositoryImpl(db);
    }

    @Bean
    public AuditRepository auditRepository(DBConnection db) {
        return new AuditRepositoryImpl(db);
    }

    @Bean
    public ConnectorConfigRepository configRepository(DBConnection db) {
        ConnectorConfigRepository repo = new ConnectorConfigRepositoryImpl(db);
        repo.save(ConnectorConfigEntity.builder()
                .connectorId("connector1")
                .rateLimitPerSecond(1)
                .maxConcurrency(5)
                .maxRetries(3)
                .maxReconcileAttempts(Constants.DEFAULT_MAX_RECONCILE_ATTEMPTS)
                .retryInitialBackoffMs(1000)
                .build());
        return repo;
    }

    @Bean
    public IdempotencyService idempotencyService() {
        return new IdempotencyServiceImpl();
    }

    @Bean
    public VersionService versionService() {
        return new VersionServiceImpl();
    }

    @Bean
    public SyncRateLimiter rateLimiter() {
        return new RateLimiterImpl();
    }

    @Bean
    public RetryPolicy retryPolicy() {
        return new RetryPolicy();
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        return new CircuitBreakerImpl();
    }

    @Bean
    public BackpressureService backpressureController() {
        return new BackpressureServiceImpl();
    }

    @Bean
    public ConnectorFactory connectorFactory() {
        ConnectorFactory factory = new ConnectorFactory();
        factory.registerConnector("connector1", new MockConnector());
        return factory;
    }

    @Bean
    public ApiExecutionService apiExecutionService(ConnectorFactory connectorFactory) {
        return new ApiExecutionService(connectorFactory);
    }

    @Bean
    public ReconciliationService reconciliationService(EventRepository eventRepository, RetryScheduler retryScheduler,
                                                       IdempotencyService idempotencyService) {
        return new ReconciliationService(eventRepository, retryScheduler, idempotencyService);
    }

    @Bean
    public RetryScheduler retryScheduler() {
        return new RetrySchedulerImpl();
    }

    @Bean
    public PollingService pollingService(ConnectorFactory connectorFactory, RecordService recordService) {
        return new PollingService(connectorFactory, recordService);
    }

    @Bean
    public ResultProcessor resultProcessor(EventRepository eventRepository, RetryPolicy retryPolicy,
                                           RetryScheduler retryScheduler, ReconciliationService reconciliationService,
                                           AuditRepository auditRepository, IdempotencyService idempotencyService) {
        return new ResultProcessor(eventRepository, retryPolicy, retryScheduler, reconciliationService, auditRepository, idempotencyService);
    }

    @Bean
    public ConnectorWorker connectorWorker(IdempotencyService idempotencyService, VersionService versionService,
                                           SyncRateLimiter rateLimiter, ApiExecutionService apiExecutionService,
                                           ResultProcessor resultProcessor, RecordRepository recordRepository,
                                           EventRepository eventRepository, ConnectorConfigRepository configRepository,
                                           AuditRepository auditRepository,
                                           RetryScheduler retryScheduler, CircuitBreaker circuitBreaker,
                                           BackpressureService backpressureController) {
        ConnectorWorker worker = new ConnectorWorker(idempotencyService, versionService, rateLimiter, 
                                 apiExecutionService, resultProcessor, recordRepository,
                                 eventRepository, configRepository, auditRepository, circuitBreaker, backpressureController, retryScheduler);
        retryScheduler.setHandler(worker::process);
        return worker;
    }

    @Bean
    public RecordService recordService(RecordRepository recordRepository) {
        return new RecordService(recordRepository);
    }

    @Bean
    public EventCreator eventCreator() {
        return new EventCreator();
    }

    @Bean
    public EventDispatcher eventDispatcher(ConnectorWorker connectorWorker) {
        return new EventDispatcher(connectorWorker);
    }

    @Bean
    public SyncService syncService(RecordRepository recordRepository, EventRepository eventRepository,
                                   EventCreator eventCreator, EventDispatcher eventDispatcher,
                                   AuditRepository auditRepository) {
        return new SyncService(recordRepository, eventRepository, eventCreator, eventDispatcher, auditRepository);
    }
}