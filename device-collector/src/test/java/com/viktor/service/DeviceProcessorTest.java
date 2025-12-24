package com.viktor.service;

import com.viktor.kafka.KafkaDltProducer;
import com.viktor.service.device.DeviceProcessor;
import com.viktor.service.device.DeviceService;
import com.viktor.utils.DeviceUtilData;
import org.junit.jupiter.api.DisplayName;
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
    KafkaDltProducer kafkaDltProducer;

    @InjectMocks
    DeviceProcessor deviceProcessor;

    @Test
    @DisplayName("Given device is saved successfully When processDevices called Then service invoked and no DLT message sent")
    void givenDeviceSavedSuccessfully_whenProcessDevices_thenServiceCalledAndNoDlt() {
        var device = DeviceUtilData.getDevice1();

        BDDMockito.given(deviceService.saveDevice(device)).willReturn(Mono.empty());

        StepVerifier.create(deviceProcessor.processDevices(List.of(device)))
                .verifyComplete();

        BDDMockito.then(deviceService).should(times(1)).saveDevice(device);
        BDDMockito.then(kafkaDltProducer).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Given device save fails When processDevices called Then message sent to DLT")
    void givenDeviceFails_whenProcessDevices_thenMessageSentToDlt() {
        var device = DeviceUtilData.getDevice1();

        BDDMockito.given(deviceService.saveDevice(device))
                .willReturn(Mono.error(new RuntimeException("db error")));

        BDDMockito.given(kafkaDltProducer.publishToDlt(eq(device), any()))
                .willReturn(Mono.empty());

        StepVerifier.create(deviceProcessor.processDevices(java.util.List.of(device)))
                .verifyComplete();

        BDDMockito.then(deviceService).should(atLeastOnce()).saveDevice(device);

        BDDMockito.then(kafkaDltProducer).should(times(1))
                .publishToDlt(eq(device), any(Throwable.class));
    }

    @Test
    @DisplayName("Given device deleted successfully When deleteDevices called Then service invoked and no DLT message sent")
    void givenDeviceDeletedSuccessfully_whenDeleteDevicesCalled_thenServiceInvokedAndNoDlt() {
        var device = DeviceUtilData.getDevice1();

        BDDMockito.given(deviceService.deleteDevice(device.getDeviceId())).willReturn(Mono.empty());

        StepVerifier.create(deviceProcessor.deleteDevices(List.of(device)))
                .verifyComplete();

        BDDMockito.then(deviceService).should(times(1)).deleteDevice(device.getDeviceId());
        BDDMockito.then(kafkaDltProducer).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Given device delete fails When deleteDevices called Then message sent to DLT")
    void givenDeviceDeleteFails_whenDeleteDevicesCalled_thenMessageSentToDlt() {
        var device = DeviceUtilData.getDevice1();

        BDDMockito.given(deviceService.deleteDevice(device.getDeviceId()))
                .willReturn(Mono.error(new RuntimeException("db error")));

        BDDMockito.given(kafkaDltProducer.publishToDlt(eq(device), any()))
                .willReturn(Mono.empty());

        StepVerifier.create(deviceProcessor.deleteDevices(List.of(device)))
                .verifyComplete();

        BDDMockito.then(deviceService).should(atLeastOnce()).deleteDevice(device.getDeviceId());

        BDDMockito.then(kafkaDltProducer).should(times(1))
                .publishToDlt(eq(device), any(Throwable.class));
    }
}
