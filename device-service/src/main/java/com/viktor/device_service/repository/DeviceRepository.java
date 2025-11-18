package com.viktor.device_service.repository;

import com.viktor.device_service.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {

    @Query("from Device d where d.deviceId = :deviceId")
    Optional<Device> findByDeviceId(@Param("deviceId") String deviceId);
}
