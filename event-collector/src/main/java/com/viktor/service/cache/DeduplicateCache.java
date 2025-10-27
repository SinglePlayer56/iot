package com.viktor.service.cache;

import com.datastax.oss.driver.shaded.guava.common.hash.BloomFilter;
import com.datastax.oss.driver.shaded.guava.common.hash.Funnels;
import com.viktor.repository.DeviceIdPublicationRepository;
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

    private final DeviceIdPublicationRepository deviceIdPublicationRepository;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final BloomFilter<String> filter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()),
            1_000_000,
            0.01
    );

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCacheFromDatabase() {
        deviceIdPublicationRepository.findAll()
                .doOnNext(entity -> filter.put(entity.getDeviceId()))
                .doOnComplete(() -> log.info("All device IDs loaded into Bloom filter"))
                .blockLast();
    }

    public void add(String deviceId) {
        lock.writeLock().lock();
        try {
            filter.put(deviceId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean contains(String deviceId) {
        lock.readLock().lock();
        try {
            return filter.mightContain(deviceId);
        } finally {
            lock.readLock().unlock();
        }
    }
}
