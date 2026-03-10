package com.mathbteixeira.edi_poc;

import com.mathbteixeira.edi_poc.model.RetailOrderDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EdiOrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EdiOrderEventConsumer.class);

    @KafkaListener(topics = "retail.orders.validated", groupId = "fulfillment-service-group")
    public void consumeOrderEvent(RetailOrderDomain.PurchaseOrder orderEvent) {
        log.info("======================================================");
        log.info("DOWNSTREAM SERVICE RECEIVED NEW ORDER EVENT");
        log.info("======================================================");
        log.info("PO Number: {}", orderEvent.poNumber());
        log.info("Partner ID: {}", orderEvent.partnerId());
        log.info("Total Items: {}", orderEvent.items().size());

        orderEvent.items().forEach(item ->
                log.info(" -> Actioning item UPC: {} (Qty: {})", item.upcCode(), item.quantity())
        );
        log.info("======================================================");
    }
}