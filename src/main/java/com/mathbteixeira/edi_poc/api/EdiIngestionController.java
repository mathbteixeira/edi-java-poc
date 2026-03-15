package com.mathbteixeira.edi_poc.api;

import com.mathbteixeira.edi_poc.application.Edi997Generator;
import com.mathbteixeira.edi_poc.application.EdiPurchaseOrderParser;
import com.mathbteixeira.edi_poc.domain.RetailOrderDomain;
import com.mathbteixeira.edi_poc.infrastructure.messaging.EdiEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/edi")
public class EdiIngestionController {

    private static final Logger log = LoggerFactory.getLogger(EdiIngestionController.class);

    private final EdiPurchaseOrderParser ediParser;
    private final EdiEventPublisher ediPublisher;
    private final Edi997Generator ackGenerator;

    public EdiIngestionController(EdiPurchaseOrderParser ediParser,
                                  EdiEventPublisher ediPublisher,
                                  Edi997Generator ackGenerator) {
        this.ediParser = ediParser;
        this.ediPublisher = ediPublisher;
        this.ackGenerator = ackGenerator;
    }

    @PostMapping(value = "/parse/850", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ingestPurchaseOrder(@RequestBody String rawEdiPayload) {
        log.info("Received new raw EDI 850 payload via API.");

        try {
            // 1. Translation
            RetailOrderDomain.PurchaseOrder cleanOrder = ediParser.parseX12Order(rawEdiPayload);

            // 2. Publish to Kafka
            ediPublisher.publish(cleanOrder);

            // 3. Generate the 997 Functional Acknowledgment
            String functionalAck997 = ackGenerator.generateAcceptedAck(cleanOrder.partnerId());
            log.info("Generated 997 Functional Acknowledgment for partner: {}", cleanOrder.partnerId());

            // 4. Return the raw X12 string to the trading partner
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(functionalAck997);

        } catch (IllegalArgumentException e) {
            log.error("Validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid EDI payload: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("System error processing EDI.");
        }
    }
}