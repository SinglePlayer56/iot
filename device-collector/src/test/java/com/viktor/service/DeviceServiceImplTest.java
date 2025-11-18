package com.viktor.service;

import com.viktor.avro.DeviceAvro;
import com.viktor.dao.DeviceDao;
import com.viktor.entity.DeviceEntity;
import com.viktor.mapper.device.DeviceMapper;
import com.viktor.service.device.DeviceServiceImpl;
import com.viktor.utils.DeviceUtilData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class DeviceServiceImplTest {

    @Mock
    DeviceDao deviceDao;

    @Mock
    DeviceMapper deviceMapper;

    @InjectMocks
    DeviceServiceImpl deviceService;

    @Test
    @DisplayName("Given valid device When saveDevice called Then DAO save is invoked")
    void givenValidDevice_whenSaveDeviceCalled_thenDaoSaveIsInvoked() {
        DeviceAvro device = DeviceUtilData.getDevice1();
        DeviceEntity entity = new DeviceEntity();

        given(deviceMapper.toDeviceEntity(device)).willReturn(entity);
        willDoNothing().given(deviceDao).save(entity);

        StepVerifier.create(deviceService.saveDevice(device))
                .verifyComplete();

        BDDMockito.then(deviceMapper).should(times(1)).toDeviceEntity(device);
        BDDMockito.then(deviceDao).should(times(1)).save(entity);
    }

    @Test
    @DisplayName("Given valid device ID When deleteDevice called Then DAO deleteById is invoked")
    void givenValidDeviceId_whenDeleteDeviceCalled_thenDaoDeleteIsInvoked() {
        DeviceAvro device = DeviceUtilData.getDevice1();

        BDDMockito.given(deviceDao.deleteById(device.getDeviceId()))
                .willReturn(1);

        StepVerifier.create(deviceService.deleteDevice(device.getDeviceId()))
                .verifyComplete();

        BDDMockito.then(deviceDao).should(times(1)).deleteById(device.getDeviceId());
    }
}
