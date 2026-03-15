package com.mathbteixeira.edi_poc.infrastructure.messaging;

import com.mathbteixeira.edi_poc.domain.RetailOrderDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EdiEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EdiEventPublisher publisher;

    @Test
    void shouldPublishOrderToValidatedTopic() {
        // Arrange
        RetailOrderDomain.PurchaseOrder mockOrder = new RetailOrderDomain.PurchaseOrder(
                "PO-111222",
                "20260310",
                "TEST-PARTNER",
                java.util.List.of()
        );

        // Act
        publisher.publish(mockOrder);

        // Assert: Verify the template's send() method was called with Topic, Key, and Payload
        verify(kafkaTemplate).send(
                eq("retail.orders.validated"),
                eq("PO-111222"),               // The Kafka partition key!
                eq(mockOrder)
        );
    }
}
