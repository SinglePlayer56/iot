CREATE TABLE IF NOT EXISTS iot_events.device_events_by_device
(
    device_id text,
    event_id  text,
    timestamp bigint,
    type      text,
    payload   text,
    PRIMARY KEY ((device_id), timestamp, event_id)
) WITH CLUSTERING ORDER BY (timestamp DESC);