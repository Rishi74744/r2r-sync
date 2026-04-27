package com.r2r.sync.events;

import com.r2r.sync.dto.SyncEventDTO;

/**
 * EventDispatcher acts as the transport layer that hands off events to the processing worker.
 * in a real system, this might push events to a message queue (RabbitMQ/Kafka).
 */
public class EventDispatcher {
    private final ConnectorWorker connectorWorker;

    public EventDispatcher(ConnectorWorker connectorWorker) {
        this.connectorWorker = connectorWorker;
    }

    /**
     * Dispatches a synchronization event for processing.
     */
    public void dispatch(SyncEventDTO event) {
        connectorWorker.process(event);
    }
}
