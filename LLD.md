# R2R-Sync: Detailed Technical Walkthrough

This document contains a deep dive into the code architecture, design decisions, and implementation details of the R2R-Sync system.

## Code Structure & Module Definitions

The project follows a **Multi-Module Maven** architecture to enforce a strict separation of concerns.

### 1. sync-base
- **Purpose**: Common dependencies and basic enums.
- **Contents**: EventStatus, Priority, SourceType, SyncException.

### 2. common-dto
- **Purpose**: Data Transfer Objects for the REST layer.
- **Contents**: Record, SyncEvent, ApiResponse, RecordDTO.

### 3. common-entity
- **Purpose**: Internal domain models used by the service and repository layers.
- **Contents**: RecordEntity, SyncEventEntity, AuditEntity, ConnectorConfigEntity.

### 4. common-db
- **Purpose**: Low-level database and cache abstraction.
- **Contents**: 
  - **DBConnection**: Generic interface for CRUD operations.
  - **CacheConnection**: Interface for distributed caching (e.g., Redis).
  - **InMemoryDBConnection**: Implementation that handles internal ID generation via reflection.

### 5. common-repository
- **Purpose**: Decoupled data storage layer.
- **Contents**: 
  - **Repositories**: RecordRepository, EventRepository, etc. (Single implementation per interface).
- **Implementation**: Repositories interact with a generic `DBConnection`. They no longer provide IDs during the `save` operation, allowing the DB layer to manage unique identifiers.

### 6. common-lib
- **Purpose**: Low-level infrastructure and resilience utilities.
- **Contents**: CircuitBreaker, BackpressureService, IdempotencyService, VersionService, Constants, JsonUtils.

### 7. sync-service
- **Purpose**: Business logic - services, connectors (mock), events (retry, worker, processor etc).
- **Contents**: 
  - ConnectorWorker: Orchestrates the sync lifecycle.
  - ResultProcessor: Handles outcomes and state transitions.
  - PollingService: Manages inbound pull-based synchronization.

### 8. sync-boot
- **Purpose**: Bootstrap module.
- **Contents**: AppConfig (manual wiring), REST Controllers, Swagger documentation.

---

## Repository Architecture & ID Generation

The system uses a tiered storage abstraction that prioritizes internal ID management:

1.  **DB-Managed IDs**: The `DBConnection.save()` method is responsible for generating and returning a unique identifier.
2.  **Reflection-Based Mapping**: The `InMemoryDBConnection` uses reflection to detect fields like `id`, `eventId`, or `recordId` on entities, ensuring IDs are populated correctly if they are missing.
3.  **Repository Hand-off**: Repositories call `save(data)` without an explicit ID, then update the entity with the returned identifier, ensuring a clean separation between business logic and storage details.

---

## Event State Machine

| Current State | Condition | Next State | Description |
|--------------|----------|------------|-------------|
| **PENDING** | Picked by worker | **IN_PROGRESS** | Event enters execution pipeline after passing idempotency/version checks. |
| **IN_PROGRESS** | API success | **SUCCESS** | External system is confirmed consistent with internal state. |
| **IN_PROGRESS** | Retryable failure | **RETRY** | Temporary failure triggers backoff schedule. |
| **RETRY** | Retry triggered | **IN_PROGRESS** | Event re-enters the execution phase. |
| **RETRY** | Retry exhausted | **RECONCILE** | Max retries reached; automated reconciliation begins. |
| **RECONCILE** | Threshold met | **MANUAL_CHECK** | Reconciliation failed multiple times; requires operator attention. |
| **ANY** | Duplicate / Stale | **SUCCESS (Skip)** | Event is marked as success to stop processing without calling external APIs. |

---

## API Reference & Documentation

### Interactive Swagger UI
- **URL**: http://localhost:9099/swagger-ui/index.html
