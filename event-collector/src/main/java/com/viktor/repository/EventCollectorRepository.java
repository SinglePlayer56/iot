package com.viktor.repository;

import com.viktor.entity.DeviceEventByDevice;
import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCollectorRepository extends ReactiveCassandraRepository<DeviceEventByDevice, Void> {

}
