package com.viktor.kafka;

import com.viktor.utils.DeviceUtilData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class KafkaDeviceDltProducerTest {

    @Mock
    KafkaTemplate<Object, Object> template;

    @InjectMocks
    KafkaDeviceDltProducer producer;

    @Test
    void givenDevice_whenPublishToDlt_thenKafkaTemplateSendCalled() {
        var device = DeviceUtilData.getDevice1();

        CompletableFuture<SendResult<Object, Object>> future =
                CompletableFuture.completedFuture(null);

        BDDMockito.willReturn(future)
                .given(template).send(eq("devices.DLT"), eq(device));

        ReflectionTestUtils.setField(producer, "topicName", "devices");

        StepVerifier.create(producer.publishToDlt(device, new RuntimeException("err")))
                .verifyComplete();

        BDDMockito.then(template).should(times(1)).send("devices.DLT", device);
    }
}
