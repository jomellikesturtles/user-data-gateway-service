package com.mdb.user_data_gateway_service.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserAccountCreatedProducer {
    private static final Logger log = LoggerFactory.getLogger(UserAccountCreatedProducer.class);
    private static final String TOPIC = "user.account.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserAccountCreatedProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(UserAccountCreatedEvent event) {
        log.info("Producing user.account.created event: {} : {}", event.accountId(), event.email());
        kafkaTemplate.send(TOPIC, event.userId().toString(), event);
    }
}
