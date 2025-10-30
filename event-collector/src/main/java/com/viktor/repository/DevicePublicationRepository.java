package com.viktor.repository;

import com.viktor.entity.DevicePublication;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DevicePublicationRepository extends ReactiveCassandraRepository<DevicePublication, String> {
}
