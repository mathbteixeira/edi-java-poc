package com.mathbteixeira.edi_poc.controller;

import com.mathbteixeira.edi_poc.service.EdiPurchaseOrderParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EdiIngestionController.class)
@Import(EdiPurchaseOrderParser.class) // Tells Spring to use your REAL parser logic
class EdiIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // Mocks Kafka so the test doesn't require Docker to be running
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldSuccessfullyParseEdiAndPublishToKafka() throws Exception {
        // Arrange: The raw EDI string
        String rawEdi = "ISA*00* *00* *ZZ*RETAILER123    *ZZ*SUPPLIER999    *260309*1530*U*00401*000000001*0*P*>~" +
                "GS*PO*RETAILER123*SUPPLIER999*20260309*1530*1*X*004010~" +
                "ST*850*0001~" +
                "BEG*00*SA*PO-987654**20260309~" +
                "PO1*1*100*EA*12.50**UP*012345678905~" +
                "PO1*2*50*EA*8.75**UP*098765432109~" +
                "CTT*2~" +
                "SE*7*0001~" +
                "GE*1*1~" +
                "IEA*1*000000001~";

        // Act & Assert: Simulate an HTTP POST and verify the JSON output
        mockMvc.perform(post("/api/v1/edi/parse/850")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(rawEdi))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.poNumber").value("PO-987654"))
                .andExpect(jsonPath("$.partnerId").value("RETAILER123"))
                .andExpect(jsonPath("$.items[0].upcCode").value("012345678905"))
                .andExpect(jsonPath("$.items[0].quantity").value(100));

        // Assert: Verify that our Anti-Corruption Layer attempted to publish the event
        verify(kafkaTemplate).send(eq("retail.orders.validated"), eq("PO-987654"), any());
    }
}