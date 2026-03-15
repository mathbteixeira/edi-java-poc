package com.mathbteixeira.edi_poc.infrastructure.routes;

import com.mathbteixeira.edi_poc.application.EdiPurchaseOrderParser;
import com.mathbteixeira.edi_poc.domain.RetailOrderDomain;
import com.mathbteixeira.edi_poc.infrastructure.messaging.EdiEventPublisher;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class EdiFileIngestionRouteTest extends CamelTestSupport {

    // Mock the Kafka publisher so we strictly test the Camel route in isolation
    private final EdiEventPublisher mockPublisher = Mockito.mock(EdiEventPublisher.class);

    // Use the real parser to test the actual data translation and sanitization
    private final EdiPurchaseOrderParser realParser = new EdiPurchaseOrderParser();

    private final Path inbox = Paths.get("edi-inbound");
    private final Path doneFolder = inbox.resolve(".done");
    private final Path testFile = inbox.resolve("test_order.txt");
    private final Path doneFile = doneFolder.resolve("test_order.txt");

    @Override
    protected RouteBuilder createRouteBuilder() {
        // Inject our dependencies into the route being tested
        return new EdiFileIngestionRoute(realParser, mockPublisher);
    }

    @BeforeEach
    void setUpFiles() throws Exception {
        // Ensure a clean slate before the test runs
        Files.createDirectories(inbox);
        Files.deleteIfExists(testFile);
        Files.deleteIfExists(doneFile);
    }

    @AfterEach
    void tearDownFiles() throws Exception {
        // Clean up after the test completes
        Files.deleteIfExists(testFile);
        Files.deleteIfExists(doneFile);
    }

    @Test
    void shouldIngestFileSanitizeParseAndMoveToDoneFolder() throws Exception {
        // Arrange: Build a tracker to notify us when exactly 1 message is completed
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1)
                .create();

        // A perfectly formatted, single-line EDI string
        String validEdi = "ISA*00* *00* *ZZ*RETAILER123    *ZZ*SUPPLIER999    *260309*1530*U*00401*000000001*0*P*>~" +
                "GS*PO*RETAILER123*SUPPLIER999*20260309*1530*1*X*004010~" +
                "ST*850*0001~" +
                "BEG*00*SA*PO-987654**20260309~" +
                "PO1*1*100*EA*12.50**UP*012345678905~" +
                "CTT*2~" +
                "SE*7*0001~" +
                "GE*1*1~" +
                "IEA*1*000000001~";

        // Act: Use Camel's ProducerTemplate to simulate dropping a file into the folder
        template.sendBodyAndHeader("file://edi-inbound", validEdi, org.apache.camel.Exchange.FILE_NAME, "test_order.txt");

        // Block the test thread for up to 5 seconds waiting for Camel to process the file
        boolean matches = notify.matches(5, TimeUnit.SECONDS);

        // Assert: Ensure Camel actually finished processing a file
        assertTrue(matches, "The Camel route should have completed processing 1 file");

        // Assert: Verify the file system state (it should be in the .done folder)
        assertTrue(Files.exists(doneFile), "The original file must be moved to the .done folder to prevent double-processing");

        // Assert: Verify our Kafka publisher was invoked with a fully parsed domain object
        Mockito.verify(mockPublisher, Mockito.times(1)).publish(any(RetailOrderDomain.PurchaseOrder.class));
    }
}