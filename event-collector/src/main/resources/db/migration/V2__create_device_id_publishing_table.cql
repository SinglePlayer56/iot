CREATE TABLE IF NOT EXISTS iot_events.published_device_ids
(
    device_id    text PRIMARY KEY,
    published_at timestamp
);