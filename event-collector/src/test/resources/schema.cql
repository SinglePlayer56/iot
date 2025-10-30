CREATE KEYSPACE IF NOT EXISTS iot_events
            WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS iot_events.device_events_by_device
(
    device_id text,
    event_id  text,
    timestamp bigint,
    type      text,
    payload   text,
    PRIMARY KEY ((device_id), timestamp, event_id)
) WITH CLUSTERING ORDER BY (timestamp DESC);

CREATE TABLE IF NOT EXISTS iot_events.device_id_publication
(
    device_id text PRIMARY KEY,
    timestamp bigint
);

CREATE TABLE IF NOT EXISTS iot_events.device_publication
(
    device_id   text PRIMARY KEY,
    device_type text,
    meta        text,
    timestamp   bigint
);

DROP TABLE IF EXISTS iot_events.device_id_publication;