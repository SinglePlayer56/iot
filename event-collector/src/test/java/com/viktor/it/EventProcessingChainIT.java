package com.viktor.it;

import com.viktor.avro.DeviceAvro;
import com.viktor.avro.DeviceEventAvro;
import com.viktor.repository.DevicePublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import com.viktor.util.DeviceDataUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EventProcessingChainIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private DevicePublicationRepository publicationRepository;

    @Autowired
    private DeduplicateCache deduplicateCache;

    @Test
    @DisplayName("Should process Kafka event, save it to DB and cache deviceId")
    void givenKafkaEvent_whenProcessed_thenSavedToDbAndCached() throws InterruptedException {
        DeviceAvro device = DeviceDataUtil.getDevice1();

        DeviceEventAvro event = new DeviceEventAvro("evt-chain-1", device, System.currentTimeMillis(), "ALERT", "{}");

        /* Cassandra может быть запущена, но ещё не готова принимать запросы */
        Thread.sleep(2000);
        kafkaTemplate.send("events", event);
        /* ждём завершения асинхронной обработки Kafka -> Consumer -> Cassandra -> Cache */
        Thread.sleep(2000);

        StepVerifier.create(publicationRepository.findById(device.getDeviceId()))
                .expectNextMatches(pub -> pub.getDeviceId().equals(device.getDeviceId()))
                .verifyComplete();

        assertThat(deduplicateCache.contains(device)).isTrue();
    }
}
