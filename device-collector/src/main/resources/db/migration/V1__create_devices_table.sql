CREATE TABLE devices
(
    device_id   text primary key,
    device_type text,
    created_at  bigint,
    meta        text
)