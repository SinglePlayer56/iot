package com.viktor.service.cache;

import com.datastax.oss.driver.shaded.guava.common.hash.BloomFilter;
import com.datastax.oss.driver.shaded.guava.common.hash.Funnels;
import com.viktor.avro.Device;
import com.viktor.entity.DevicePublication;
import com.viktor.repository.DevicePublicationRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicateCache {

    private final DevicePublicationRepository devicePublicationRepository;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final BloomFilter<String> filter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()),
            1_000_000,
            0.01
    );

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCacheFromDatabase() {
        devicePublicationRepository.findAll()
                .doOnNext(entity -> filter.put(buildKey(entity)))
                .doOnComplete(() -> log.info("All device IDs loaded into Bloom filter"))
                .blockLast();
    }

    @WithSpan("add-device-in-cache")
    public void add(Device device) {
        lock.writeLock().lock();
        try {
            log.info("Add device {} in cache", device);
            var key = buildKey(device);
            filter.put(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean contains(Device device) {
        lock.readLock().lock();
        try {
            var key = buildKey(device);
            return filter.mightContain(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    private String buildKey(Device device) {
        return device.getDeviceId() + "|" + device.getDeviceType() + "|" + device.getMeta();
    }

    private String buildKey(DevicePublication devicePublication) {
        return devicePublication.getDeviceId() + "|" + devicePublication.getDeviceType() + "|" + devicePublication.getMeta();
    }
}
