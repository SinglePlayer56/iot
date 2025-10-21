package com.viktor.service.cache;

import com.datastax.oss.driver.shaded.guava.common.hash.BloomFilter;
import com.datastax.oss.driver.shaded.guava.common.hash.Funnels;
import com.viktor.entity.DeviceIdPublication;
import com.viktor.repository.DeviceIdPublicationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@RequiredArgsConstructor
public class DeduplicateCache {

    private final DeviceIdPublicationRepository deviceIdPublicationRepository;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private BloomFilter<String> filter;

    @PostConstruct
    private void init() {
        filter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                1_000_000,
                0.01
        );

        initPublishedDeviceId(filter);
    }

    private void initPublishedDeviceId(BloomFilter<String> filter) {
        Pageable pageable = CassandraPageRequest.first(10_000);
        Slice<DeviceIdPublication> slice;

        do {
            slice = deviceIdPublicationRepository.findAll(pageable);
            slice.forEach(entity -> filter.put(entity.getDeviceId()));
            if (slice.hasNext()) {
                pageable = slice.nextPageable();
            }
        } while (slice.hasNext());
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
