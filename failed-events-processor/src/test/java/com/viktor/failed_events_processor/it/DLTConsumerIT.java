package com.viktor.failed_events_processor.it;

import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.service.FailedEventHandler;
import com.viktor.failed_events_processor.utils.DltErrorData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DLTConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, DltError> kafkaTemplate;

    @MockitoBean
    private FailedEventHandler failedEventHandler;

    @Test
    @DisplayName("Given DltError in DLT topic when consumed then handler called")
    void givenMessage_whenConsumed_thenHandlerCalled() {
        DltError error = DltErrorData.getDltError();
        given(failedEventHandler.processMessages(any()))
                .willReturn(Mono.empty());

        kafkaTemplate.send("dlt-topic", error);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(failedEventHandler, times(1))
                                .processMessages(argThat(list -> list.size() == 1))
                );
    }
}
