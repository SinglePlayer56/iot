package com.viktor.repository;

import com.viktor.entity.DeviceIdPublication;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceIdPublicationRepository extends ReactiveCassandraRepository<DeviceIdPublication, String> {
}
