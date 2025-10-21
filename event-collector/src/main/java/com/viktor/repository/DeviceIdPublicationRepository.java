package com.viktor.repository;

import com.viktor.entity.DeviceIdPublication;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceIdPublicationRepository extends CassandraRepository<DeviceIdPublication, String> {
}
