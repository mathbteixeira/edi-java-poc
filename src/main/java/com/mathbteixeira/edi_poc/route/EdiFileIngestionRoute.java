package com.mathbteixeira.edi_poc.route;

import com.mathbteixeira.edi_poc.EdiEventPublisher;
import com.mathbteixeira.edi_poc.service.EdiPurchaseOrderParser;
import com.mathbteixeira.edi_poc.model.RetailOrderDomain;
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
    public void configure() throws Exception {

        // 1. INGESTION: Watch the "edi-inbound" folder at the root of the project.
        // The '?move=.done' parameter tells Camel to automatically move the file
        // into a hidden '.done' folder after successful processing so it isn't read twice.
        from("file:edi-inbound?move=.done")
                .routeId("van-as2-file-ingestion-route")
                .log("Picked up new EDI file from Trading Partner: ${header.CamelFileName}")

                // 2. TRANSLATION: Read the file content as a String
                .convertBodyTo(String.class)

                // 3. PROCESSING: Pass the raw string to our custom parser
                .process(exchange -> {
                    String rawEdi = exchange.getIn().getBody(String.class);

                    // SANITIZATION: Strip out any invisible newlines or carriage returns
                    // that a text editor or trading partner's FTP client might have added.
                    String sanitizedEdi = rawEdi.replaceAll("[\\r\\n]+", "");

                    RetailOrderDomain.PurchaseOrder order = parser.parseX12Order(sanitizedEdi);

                    exchange.getIn().setBody(order);
                })
                .log("Successfully parsed X12 PO Number: ${body.poNumber}")

                // 4. PUBLISHING: Hand the clean object to our Kafka publisher service
                .process(exchange -> {
                    RetailOrderDomain.PurchaseOrder order = exchange.getIn().getBody(RetailOrderDomain.PurchaseOrder.class);
                    publisher.publish(order);
                })
                .log("File processing complete.");
    }
}