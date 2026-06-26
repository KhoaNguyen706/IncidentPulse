-- Webhook ingestion: track which external monitor (and which of its alerts)
-- created an incident, so duplicate alerts don't open duplicate incidents.
ALTER TABLE incident ADD COLUMN source VARCHAR(255);
ALTER TABLE incident ADD COLUMN external_id VARCHAR(255);

-- Unique only when BOTH are present (Postgres treats NULLs as distinct, so
-- manually-created incidents with NULL source/external_id are unaffected).
CREATE UNIQUE INDEX uq_incident_source_external_id
    ON incident (source, external_id);
