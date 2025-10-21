package com.viktor.service.event_collector;

import com.viktor.avro.DeviceEvent;

public interface EventCollectorService {

    void processEvent(DeviceEvent deviceEvent);
}
