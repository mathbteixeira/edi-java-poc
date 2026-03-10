package com.mathbteixeira.edi_poc;

import com.mathbteixeira.edi_poc.model.RetailOrderDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EdiEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EdiEventPublisher.class);
    private static final String KAFKA_TOPIC = "retail.orders.validated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EdiEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(RetailOrderDomain.PurchaseOrder cleanOrder) {
        kafkaTemplate.send(KAFKA_TOPIC, cleanOrder.poNumber(), cleanOrder);
        log.info("Published clean order event to Kafka topic: {}", KAFKA_TOPIC);
    }
}