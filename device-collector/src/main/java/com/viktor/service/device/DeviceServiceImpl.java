package com.viktor.service.device;

import com.viktor.avro.Device;
import com.viktor.dao.DeviceDao;
import com.viktor.mapper.device.DeviceMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceDao deviceDao;
    private final DeviceMapper deviceMapper;

    @Override
    @WithSpan("save-device-service")
    public Mono<Void> saveDevice(Device device) {
        log.info("Saving device {}", device.getDeviceId());
        return Mono.<Void>fromCallable(() -> {
                    var entity = deviceMapper.toDeviceEntity(device);
                    deviceDao.save(entity);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
