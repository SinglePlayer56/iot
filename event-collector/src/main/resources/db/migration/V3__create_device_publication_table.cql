CREATE TABLE IF NOT EXISTS iot_events.device_publication
(
    device_id   text PRIMARY KEY,
    device_type text,
    meta        text,
    timestamp   bigint
);
