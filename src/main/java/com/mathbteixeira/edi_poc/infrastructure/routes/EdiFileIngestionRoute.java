package com.mathbteixeira.edi_poc.infrastructure.routes;

import com.mathbteixeira.edi_poc.application.EdiPurchaseOrderParser;
import com.mathbteixeira.edi_poc.domain.RetailOrderDomain;
import com.mathbteixeira.edi_poc.infrastructure.messaging.EdiEventPublisher;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class EdiFileIngestionRoute extends RouteBuilder {

    private final EdiPurchaseOrderParser parser;
    private final EdiEventPublisher publisher;

    public EdiFileIngestionRoute(EdiPurchaseOrderParser parser, EdiEventPublisher publisher) {
        this.parser = parser;
        this.publisher = publisher;
    }

    @Override
    public void configure() {

        from("file:edi-inbound?move=.done")
                .routeId("van-as2-file-ingestion-route")
                .log("Picked up new EDI file from Trading Partner: ${header.CamelFileName}")
                .convertBodyTo(String.class)
                .process(exchange -> {
                    String rawEdi = exchange.getIn().getBody(String.class);
                    String sanitizedEdi = rawEdi.replaceAll("[\\r\\n]+", "");
                    RetailOrderDomain.PurchaseOrder order = parser.parseX12Order(sanitizedEdi);
                    exchange.getIn().setBody(order);
                })
                .log("Successfully parsed X12 PO Number: ${body.poNumber}")
                .process(exchange -> {
                    RetailOrderDomain.PurchaseOrder order = exchange.getIn().getBody(RetailOrderDomain.PurchaseOrder.class);
                    publisher.publish(order);
                })
                .log("File processing complete.");
    }
}