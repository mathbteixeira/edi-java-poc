package com.mathbteixeira.edi_poc.controller;

import com.mathbteixeira.edi_poc.service.EdiPurchaseOrderParser;
import com.mathbteixeira.edi_poc.model.RetailOrderDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/edi")
public class EdiIngestionController {

    private static final Logger log = LoggerFactory.getLogger(EdiIngestionController.class);
    private static final String KAFKA_TOPIC = "retail.orders.validated";

    private final EdiPurchaseOrderParser ediParser;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EdiIngestionController(EdiPurchaseOrderParser ediParser, KafkaTemplate<String, Object> kafkaTemplate) {
        this.ediParser = ediParser;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping(value = "/parse/850", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ingestPurchaseOrder(@RequestBody String rawEdiPayload) {
        log.info("Received new raw EDI 850 payload. Length: {} characters", rawEdiPayload.length());

        try {
            RetailOrderDomain.PurchaseOrder cleanOrder = ediParser.parseX12Order(rawEdiPayload);
            log.info("Successfully parsed X12 PO Number: {} from Partner: {}", cleanOrder.poNumber(), cleanOrder.partnerId());

            kafkaTemplate.send(KAFKA_TOPIC, cleanOrder.poNumber(), cleanOrder);
            log.info("Published clean order event to Kafka topic: {}", KAFKA_TOPIC);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(cleanOrder);

        } catch (IllegalArgumentException e) {
            log.error("Validation failed for incoming EDI document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid EDI payload: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during EDI ingestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing the EDI document.");
        }
    }
}