package com.viktor.repository;

import com.viktor.entity.DeviceEventByDevice;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCollectorRepository extends CassandraRepository<DeviceEventByDevice, Void> {

}
