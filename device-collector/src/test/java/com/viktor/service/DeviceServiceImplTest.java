package com.viktor.service;

import com.viktor.avro.Device;
import com.viktor.dao.DeviceDao;
import com.viktor.entity.DeviceEntity;
import com.viktor.mapper.device.DeviceMapper;
import com.viktor.service.device.DeviceServiceImpl;
import com.viktor.utils.DeviceUtilData;
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
    void givenValidDevice_whenSaveDevice_thenDaoSaveIsCalled() {
        Device device = DeviceUtilData.getDevice1();
        DeviceEntity entity = new DeviceEntity();

        given(deviceMapper.toDeviceEntity(device)).willReturn(entity);
        willDoNothing().given(deviceDao).save(entity);

        StepVerifier.create(deviceService.saveDevice(device))
                .verifyComplete();

        BDDMockito.then(deviceMapper).should(times(1)).toDeviceEntity(device);
        BDDMockito.then(deviceDao).should(times(1)).save(entity);
    }
}
