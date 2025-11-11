package com.viktor.service;

import com.viktor.kafka.KafkaDeviceDltProducer;
import com.viktor.service.device.DeviceProcessor;
import com.viktor.service.device.DeviceService;
import com.viktor.utils.DeviceUtilData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class DeviceProcessorTest {

    @Mock
    DeviceService deviceService;

    @Mock
    KafkaDeviceDltProducer kafkaDeviceDltProducer;

    @InjectMocks
    DeviceProcessor deviceProcessor;

    @Test
    void givenDeviceSavedSuccessfully_whenProcessDevices_thenServiceCalledAndNoDlt() {
        var device = DeviceUtilData.getDevice1();

        BDDMockito.given(deviceService.saveDevice(device)).willReturn(Mono.empty());

        StepVerifier.create(deviceProcessor.processDevices(List.of(device)))
                .verifyComplete();

        BDDMockito.then(deviceService).should(times(1)).saveDevice(device);
        BDDMockito.then(kafkaDeviceDltProducer).shouldHaveNoInteractions();
    }

    @Test
    void givenDeviceFails_whenProcessDevices_thenMessageSentToDlt() {
        var device = DeviceUtilData.getDevice1();

        BDDMockito.given(deviceService.saveDevice(device))
                .willReturn(Mono.error(new RuntimeException("db error")));

        BDDMockito.given(kafkaDeviceDltProducer.publishToDlt(eq(device), any()))
                .willReturn(Mono.empty());

        StepVerifier.create(deviceProcessor.processDevices(java.util.List.of(device)))
                .verifyComplete();

        BDDMockito.then(deviceService).should(atLeastOnce()).saveDevice(device);

        BDDMockito.then(kafkaDeviceDltProducer).should(times(1))
                .publishToDlt(eq(device), any(Throwable.class));
    }
}
