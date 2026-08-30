-- V9: Data platform - streaming ingest / analytics read-model surface.
-- A lightweight stand-in for Kafka consumption on the analytics read side:
-- domain events are POSTed here, queued as PENDING, drained deterministically
-- and marked PROCESSED by consumers. Keeps a replayable event log without a
-- broker dependency in dev.

CREATE TABLE ingest_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(60) NOT NULL,
    payload VARCHAR(2000),
    occurred_at TIMESTAMP NOT NULL,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_ingest_status ON ingest_events (processing_status);
CREATE INDEX idx_ingest_type ON ingest_events (event_type);