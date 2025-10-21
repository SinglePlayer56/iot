package com.viktor.service.event_collector;

import com.viktor.avro.DeviceEvent;
import com.viktor.entity.DeviceEventByDevice;
import com.viktor.repository.EventCollectorRepository;
import com.viktor.service.device_id_publication.DeviceIdPublicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCollectorServiceImpl implements EventCollectorService {

    private final EventCollectorRepository eventCollectorRepository;
    private final DeviceIdPublicationService deviceIdPublicationService;

    @Override
    @Transactional
    public void processEvent(DeviceEvent deviceEvent) {
        saveEvent(deviceEvent);
        deviceIdPublicationService.processDeviceId(deviceEvent.getDeviceId());
    }

    private void saveEvent(DeviceEvent deviceEvent) {
        DeviceEventByDevice event = new DeviceEventByDevice();
        event.setDeviceId(deviceEvent.getDeviceId());
        event.setEventId(deviceEvent.getEventId());
        event.setTimestamp(deviceEvent.getTimestamp());
        event.setType(deviceEvent.getType());
        event.setPayload(deviceEvent.getPayload());
        eventCollectorRepository.save(event);
    }
}
