package com.mdb.user_data_gateway_service.producer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "user.account.created" })
class UserAccountCreatedProducerTest {

    @Autowired
    private UserAccountCreatedProducer producer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void testSendEvent() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UserAccountCreatedEvent event = new UserAccountCreatedEvent(
            userId,
            accountId,
            "testuser",
            "test@example.com",
            Instant.now()
        );

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(
            consumerProps,
            new org.apache.kafka.common.serialization.StringDeserializer(),
            new org.apache.kafka.common.serialization.StringDeserializer()
        );
        
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList("user.account.created"));

            producer.send(event);

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "user.account.created");
            assertThat(record.key()).isEqualTo(userId.toString());
            assertThat(record.value()).contains(userId.toString());
            assertThat(record.value()).contains("testuser");
            assertThat(record.value()).contains("test@example.com");
        }
    }
}
