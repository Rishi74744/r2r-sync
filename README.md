# R2R-Sync: Modular Java Sync System

A robust, modular Java-based synchronization system designed with a clean separation of concerns, high extensibility, and resilience at its core.

## Architecture Overview

The project is structured into several modules to ensure zero tight coupling between core logic and infrastructure:

- **sync-base**: Common dependencies and basic enums.
- **common-dto**: Data Transfer Objects for the REST layer.
- **common-entity**: Internal domain models.
- **common-db**: Database and cache abstraction.
- **common-repository**: Decoupled storage layer.
- **common-lib**: Shared infrastructure logic (Resilience, Utils).
- **sync-service**: Business logic and sync engine.
- **sync-boot**: Spring Boot entry point and REST controllers.

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Setup & Build
1. Clone the repository.
2. Build all modules:
```bash
mvn clean install -DskipTests
```

### Running the Application
Run the bootstrap module:
```bash
mvn spring-boot:run -pl sync-boot
```
The application will start on port 9099.

---

## Automated Scenario Verification
The system includes a suite of integration tests to automatically verify all synchronization scenarios and resilience features.

### How to Run Tests
Execute the following command from the root directory:
```bash
mvn test -pl sync-boot -Dtest=ScenarioVerificationTests
```

### Test Case Descriptions

| Test Case | Purpose | Verification Logic |
|-----------|---------|-------------------|
| **testSuccessScenario** | Verifies the Happy Path. | Ensures a record with `scenario: success` reaches the `SUCCESS` state in the event repository. |
| **testFailureToReconcileScenario** | Verifies Retries & Reconciliation. | Simulates a persistent failure and waits for the system to exhaust all retries (3 strikes) and move to `RECONCILE`. |
| **testTimeoutScenario** | Verifies Network Resilience. | Ensures that an external timeout triggers the `RETRY` state rather than failing permanently. |
| **testRateLimitScenario** | Verifies Throttling Resilience. | Confirms that a `429 Too Many Requests` response from a connector triggers a retry with backoff. |

### Interpreting Results
- **Pass**: All assertions met within the `Awaitility` window. This confirms that state transitions and timing (exponential backoff) are functioning correctly.
- **Fail/Timeout**: Indicates a regression in state management or that the backoff delays exceeded the test's maximum wait time (45s).
- **Logs**: The test output will print live audit transitions for each record (e.g., `PENDING:EVENT_CREATED, RETRY:SYNC_RETRY and so on`), allowing you to trace the lifecycle in real-time.

---

## Config-Driven Behavior (Runtime Tuning)

The system is entirely stateless and all resilience thresholds are fetched from the database for each execution. You can tune these parameters at runtime via the **Config Management** API.

### Available Configuration Parameters
- **`maxRetries`**: Number of retry attempts before moving an event to `RECONCILE`.
- **`maxReconcileAttempts`**: Number of reconciliation attempts before moving to `MANUAL_CHECK`.
- **`retryInitialBackoffMs`**: The starting delay for exponential backoff (e.g., 1000ms).
- **`rateLimitPerSecond`**: Maximum requests per second for a specific connector.
- **`maxConcurrency`**: Maximum simultaneous synchronization threads allowed for a connector.

### How to Update Config
1. Go to **Config Management** -> `POST /api/config`.
2. Use the following payload to update `connector1`:
   ```json
   {
     "connectorId": "connector1",
     "rateLimitPerSecond": 10,
     "maxConcurrency": 20,
     "maxRetries": 5,
     "retryInitialBackoffMs": 500
   }
   ```
3. The next event processed for this connector will immediately use the new values.

---

## Testing with Swagger UI
The system includes a fully interactive Swagger UI for manual testing and verification.

### Accessing Swagger
- **URL**: [http://localhost:9099/swagger-ui/index.html](http://localhost:9099/swagger-ui/index.html)

---

## Testing Synchronization Scenarios (Manual)

You can simulate different API behaviors by passing a `scenario` key in the record data payload.

### Step 1: Create a Test Record
1. Go to **Record Management** -> `POST /api/records`.
2. Click **Try it out**.
3. Use a payload with one of the following scenarios:

| Scenario | Payload Key | Expected Behavior |
|----------|-------------|-------------------|
| **Success** | `"scenario": "success"` | Event status: `SUCCESS`. |
| **Failure** | `"scenario": "failure"` | Event status: `RETRY` -> `SUCCESS` (or `RECONCILE` if retries exhausted). |
| **Timeout** | `"scenario": "timeout"` | Event status: `RETRY` (Simulates network timeout). |
| **Rate Limit** | `"scenario": "rate_limit"` | Event status: `RETRY` (Simulates external 429 response). |

Example Payload:
```json
{
  "type": "USER",
  "data": {
    "name": "Test User",
    "scenario": "failure" 
  }
}
```
4. Click **Execute** and copy the generated `id`.

### Step 2: Trigger Synchronization
1. Go to **Sync Management** -> `POST /api/sync/{recordId}`.
2. Enter the `recordId` from Step 1.
3. Set `connectorId` to `connector1`.
4. Click **Execute**.

### Step 3: Verify Results
1. Go to **Monitoring & Audits** -> `GET /api/monitoring/audits`.
2. Click **Execute** to see the state transitions (e.g., `PENDING -> IN_PROGRESS -> RETRY`).

---

## Testing Resilience Features

### 1. Exponential Backoff (Retries)
- **Test**: Use `"scenario": "failure"`.
- **Observation**: Check audit logs. You will see multiple `SYNC_RETRY` entries with increasing time gaps between them.

### 2. Circuit Breaker
- **Test**: Trigger the `failure` scenario multiple times (3+ consecutive times).
- **Observation**: The system will eventually stop calling the external API and fail fast, indicating the circuit is `OPEN`.

### 3. Rate Limiting (Throttling)
- **Test**: Quickly trigger multiple `POST /api/sync/` calls for the same connector.
- **Observation**: Some requests will be delayed or temporarily rejected to stay within the configured `rateLimitPerSecond` (default: 1 req/sec).

### 4. Backpressure (Concurrency)
- **Test**: Trigger many simultaneous sync requests.
- **Observation**: The system will limit the number of active threads processing events for that connector based on `maxConcurrency` (default: 5).

---

## Inbound Integration (External -> Internal)

### Webhook Model:
- Use **Inbound Integration** -> `POST /api/inbound/webhook`.
- This simulates an external system pushing data into our database.

### Polling Model:
- Use **Inbound Integration** -> `POST /api/inbound/poll/{connectorId}`.
- This simulates our system periodically "pulling" updates from an external source.


### Design:
Refer to
- For [HLD](./HLD.md) Design
- For [LLD](./LLD.md) Design