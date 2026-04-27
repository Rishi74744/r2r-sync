# R2R-Sync: High Level Design (HLD)

This document outlines the architectural blueprint for the R2R-Sync system.

## 1. System Architecture

The system is designed as a modular, event-driven synchronization engine.

### Core Components

1.  **Ingestion Layer**: REST Controllers that receive sync triggers or inbound webhooks.
2.  **Service Layer**: Business logic for record management and sync orchestration.
3.  **Resilience Engine**: A suite of utilities including Circuit Breakers, Rate Limiters, and Backpressure Controllers.
4.  **Worker Layer**: Async processors that execute the synchronization logic.
5.  **Persistence Layer**: Mocked repositories for Records, Events, and Audits.

---

## 2. Event State Machine

### Overview

Each synchronization event follows a well-defined lifecycle to ensure:
- Correctness (no duplicate or stale execution)
- Resilience (retry and reconciliation)
- Observability (clear state transitions)

The system does not rely on strict ordering. Instead, it uses state-based validation (idempotency + versioning) to ensure correctness.

### Event States

- **PENDING**: Event is created and stored, awaiting processing.
- **IN_PROGRESS**: Event picked by worker, API execution is active.
- **SUCCESS**: Event successfully processed and confirmed.
- **RETRY**: Temporary failure occurred; event scheduled for backoff.
- **RECONCILE**: Retry threshold exceeded; automated reconciliation attempts active.
- **MANUAL_CHECK**: Reconciliation threshold exceeded; requires human intervention.
- **SKIPPED**: (Implicitly marked as SUCCESS) due to duplicate or stale version.

### State Transitions

| Current State | Condition | Next State | Description |
|--------------|----------|------------|-------------|
| PENDING | Picked by worker | IN_PROGRESS | Event enters execution pipeline after passing idempotency/version checks. |
| IN_PROGRESS | API success | SUCCESS | External system is confirmed consistent with internal state. |
| IN_PROGRESS | Retryable failure | RETRY | Temporary failure (timeout/5xx) triggers backoff schedule. |
| RETRY | Retry triggered | IN_PROGRESS | Event re-enters the execution phase. |
| RETRY | Retry exhausted | RECONCILE | Max retries reached; automated reconciliation begins. |
| RECONCILE | Reconcile threshold met | MANUAL_CHECK | Reconciliation failed multiple times; requires operator attention. |
| ANY | Duplicate / Stale | SUCCESS (Skip) | Event is marked as success to stop processing without calling external APIs. |

---

## 3. Resilience Strategy

1.  **Exponential Backoff**: Retries are scheduled with increasing delays and random jitter to prevent "thundering herd" problems.
2.  **Circuit Breaker**: Monitors failure rates per connector. If a threshold is met, it "trips" and fails fast to protect the system.
3.  **Backpressure**: Limits the number of concurrent synchronization tasks to prevent resource exhaustion.
4.  **Rate Limiting**: Ensures compliance with external API limits on a per-connector basis.

---

## 4. Data Flow

### Outbound Sync (Internal -> External)
1.  User/System triggers sync for a Record.
2.  `SyncService` creates a `SyncEvent` (PENDING).
3.  `EventDispatcher` hands event to `ConnectorWorker`.
4.  Worker performs pre-checks (Idempotency, Version, Rate Limit).
5.  Worker executes API call via `CircuitBreaker`.
6.  `ResultProcessor` updates state and audits.

### Inbound Webhook (External -> Internal)
1.  External system calls `/api/webhooks/inbound`.
2.  `WebhookController` updates the internal Record in `RecordRepository`.
3.  Versioning ensures that manual/external updates don't conflict with in-flight syncs.

---

## 5. Design Principles

- **Separation of Concerns**: Infrastructure (Spring Boot) is strictly separated from domain logic (Plain Java).
- **Extensibility**: New connectors can be added by implementing the `Connector` interface.
- **Testability**: Interfaces and mocking enable easy unit and integration testing.
- **Idempotency**: Every operation is designed to be safe to repeat.
--- 
## Technology Choices, Alternatives, and Tradeoffs

This section outlines the key technology decisions, alternatives considered, and why specific choices were made.

---

## 1. Messaging System

### Selected: Kafka (or Managed Kafka like MSK)

Used for:
- Event dispatching
- Connector queues
- Retry/reconciliation reprocessing

---

### Alternatives Considered

#### AWS SQS

**Why considered:**
- Fully managed
- Simple to use
- Built-in retry and DLQ

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| No partitioning model | Hard to scale parallel processing efficiently |
| Limited throughput per queue | Not suitable for 300M events/day |
| No ordering guarantees (except FIFO with limits) | Not reliable for event sequencing |
| No native replay capability | Hard to debug/reprocess events |
| FIFO queues have throughput limits | Bottleneck under high load |

---

#### RabbitMQ

**Why considered:**
- Mature queueing system
- Flexible routing

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Lower throughput vs Kafka | Not ideal for very high scale |
| Memory-heavy | Operational challenges |
| Not optimized for event streaming | Poor fit for event-driven systems |
| Harder horizontal scaling | Limits system growth |

---

### Final Decision: Kafka

| Reason | Explanation |
|--------|------------|
| High throughput | Handles millions of events/sec |
| Partitioning | Enables parallelism |
| Replay capability | Useful for debugging and recovery |
| Durability | Strong guarantees |

---

## 2. Retry System

### Selected: Redis ZSET + DB Scheduler

---

### Alternatives Considered

#### Kafka Retry Topics

**Why considered:**
- Native to messaging system
- No extra infrastructure

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| No fine-grained delay control | Hard to implement precise retry timing |
| Topic explosion (multiple retry topics) | Operational complexity |
| Hard to manage exponential backoff | Less flexible |

---

#### SQS Delay Queues

**Why considered:**
- Built-in delay feature

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Limited delay flexibility | Not ideal for exponential backoff |
| No fine control over scheduling | Less control |
| Visibility timeout complexity | Harder to manage retries |

---

### Final Decision: Redis ZSET

| Reason | Explanation |
|--------|------------|
| Time-based scheduling | Precise retry timing |
| Fast access | Low latency |
| Flexible backoff | Easy to implement |

---

## 3. Execution Environment

### Selected: Kubernetes

---

### Alternatives Considered

#### AWS Lambda

**Why considered:**
- Serverless
- Auto-scaling

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Cold starts | Increased latency |
| Execution time limits | Not suitable for long workflows |
| Limited control over concurrency | Hard to manage backpressure |
| Cost at scale | Expensive for high throughput |
| Difficult connection reuse | Inefficient API calls |

---

#### ECS / Managed Containers

**Why considered:**
- Easier than Kubernetes

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Less flexibility than Kubernetes | Limited control |
| Vendor-specific | Lock-in risk |

---

### Final Decision: Kubernetes

| Reason | Explanation |
|--------|------------|
| Fine-grained control | Over threads, batching |
| Efficient resource usage | Better performance |
| Supports long-running workers | Ideal for sync pipelines |

---

## 4. Ingestion Mechanism

### Selected: Webhook + Polling Hybrid

---

### Alternatives Considered

#### Only Webhooks

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Missed events possible | Data inconsistency |
| Dependency on external reliability | Risky |

---

#### Only Polling

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| High latency | Not real-time |
| Inefficient | Unnecessary API calls |
| Rate limit pressure | More load |

---

### Final Decision: Hybrid

| Reason | Explanation |
|--------|------------|
| Webhook | Real-time updates |
| Polling | Reliability fallback |

---

## 5. Database Choice

### Selected: Relational DB (PostgreSQL/MySQL)

---

### Alternatives Considered

#### NoSQL (DynamoDB, MongoDB)

**Why considered:**
- Scalable
- Flexible schema

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Weak consistency (eventual) | Risky for state correctness |
| Complex querying | Hard for reconciliation |
| No strong transactions | Data integrity issues |

---

### Final Decision: Relational DB

| Reason | Explanation |
|--------|------------|
| Strong consistency | Critical for SSOT |
| ACID transactions | Reliable state updates |
| Easy querying | Useful for debugging/reconciliation |

---

## 6. Event Processing Model

### Selected: Event-driven (Kafka + Workers)

---

### Alternatives Considered

#### Synchronous API-based sync

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Tight coupling | Hard to scale |
| Blocking operations | Poor performance |
| No retry control | Failure handling weak |

---

### Final Decision: Event-driven

| Reason | Explanation |
|--------|------------|
| Decoupled system | Independent scaling |
| Async processing | Better throughput |
| Built-in retry flexibility | Resilience |

---

## 7. Rate Limiting

### Selected: Custom Rate Limiter + External API Constraints

---

### Alternatives Considered

#### Only External API Rate Limits

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Reactive handling | Too late |
| Causes retries | Increased load |

---

### Final Decision

- Early check (Connector Worker)
- Final check (API Worker)

---

## 8. Deployment Strategy

### Selected: Rolling Deployment + Canary

---

### Alternatives Considered

#### Blue-Green Deployment

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Resource heavy | Duplicate infra |
| Slower switchovers | Operational complexity |

---

### Final Decision

| Reason | Explanation |
|--------|------------|
| Rolling updates | Minimal disruption |
| Canary releases | Safer rollout |

---

## 9. Scaling Strategy

### Selected: Horizontal Scaling (Kafka + Workers)

---

### Alternatives Considered

#### Vertical Scaling

**Why not chosen:**

| Limitation | Impact |
|-----------|--------|
| Limited capacity | Cannot scale infinitely |
| Expensive | Cost inefficiency |

---

### Final Decision

| Reason | Explanation |
|--------|------------|
| Partition-based scaling | Efficient |
| Worker-based scaling | Flexible |

---

## 10. Summary of Decisions

| Component | Selected | Alternative | Reason |
|----------|----------|------------|--------|
| Messaging | Kafka | SQS | Throughput, partitioning |
| Retry | Redis ZSET | Kafka/SQS | Flexible scheduling |
| Execution | Kubernetes | Lambda | Control, performance |
| Ingestion | Webhook + Poll | Single method | Reliability |
| DB | Relational | NoSQL | Strong consistency |
| Processing | Event-driven | Sync APIs | Scalability |

---

## Final Takeaway

Each technology choice was made based on:
- scale requirements
- failure handling needs
- control over execution

Tradeoffs were accepted in favor of:
- reliability
- scalability
- operational correctness